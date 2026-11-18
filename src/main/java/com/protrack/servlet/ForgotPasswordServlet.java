package com.protrack.servlet;

import com.protrack.exception.AppException;
import com.protrack.model.User;
import com.protrack.service.OtpService;
import com.protrack.service.UserService;
import com.protrack.util.CSRFTokenUtil;
import com.protrack.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Controller handling self-service password recovery via Mail OTP and Contact OTP.
 * Supports in-modal AJAX flows (from Profile and Login) and standalone page flows.
 */
@WebServlet(name = "ForgotPasswordServlet", urlPatterns = {"/forgot-password", "/api/forgot-password"})
public class ForgotPasswordServlet extends HttpServlet {

    private final OtpService otpService;
    private final UserService userService;

    public ForgotPasswordServlet() {
        this.otpService = new OtpService();
        this.userService = new UserService();
    }

    public ForgotPasswordServlet(OtpService otpService, UserService userService) {
        this.otpService = otpService;
        this.userService = userService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        CSRFTokenUtil.getToken(session);
        request.getRequestDispatcher("/WEB-INF/views/auth/forgot-password.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        boolean isAjax = "true".equalsIgnoreCase(request.getParameter("ajax"))
                || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "sendOtp";
        }

        try {
            if ("sendOtp".equalsIgnoreCase(action)) {
                String identifier = request.getParameter("identifier");
                if (!ValidationUtil.isNotEmpty(identifier) && currentUser != null) {
                    identifier = currentUser.getEmail();
                }

                String channel = request.getParameter("channel"); // "MAIL" or "CONTACT"
                OtpService.SendOtpResponse otpResp = otpService.sendOtp(identifier, channel);

                if (isAjax) {
                    // Do not leak the OTP code to client JSON response (must be delivered via email/contact only)
                    sendJsonResponse(response, HttpServletResponse.SC_OK, true,
                            otpResp.message(), otpResp.channel(), otpResp.maskedTarget(),
                            null, otpResp.expiryMinutes(), otpResp.email());
                } else {
                    if (session != null) session.setAttribute("flash_success", otpResp.message());
                    request.setAttribute("step", "verify");
                    request.setAttribute("identifier", otpResp.email());
                    request.setAttribute("maskedTarget", otpResp.maskedTarget());
                    request.setAttribute("channel", otpResp.channel());
                    request.getRequestDispatcher("/WEB-INF/views/auth/forgot-password.jsp").forward(request, response);
                }

            } else if ("verifyAndReset".equalsIgnoreCase(action)) {
                // Validate CSRF token for password change
                if (!CSRFTokenUtil.isValid(request)) {
                    throw new AppException("Security token expired or invalid. Please refresh the page and try again.");
                }

                String identifier = request.getParameter("identifier");
                if (!ValidationUtil.isNotEmpty(identifier) && currentUser != null) {
                    identifier = currentUser.getEmail();
                }

                String otpCode = request.getParameter("otpCode");
                String newPassword = request.getParameter("newPassword");
                String confirmPassword = request.getParameter("confirmPassword");

                OtpService.ResetPasswordResponse resetResp = otpService.verifyAndResetPassword(
                        identifier, otpCode, newPassword, confirmPassword);

                // If user is currently logged in, refresh session data
                if (currentUser != null && currentUser.getEmail().equalsIgnoreCase(resetResp.email())) {
                    User freshUser = userService.getUserById(currentUser.getId());
                    session.setAttribute("currentUser", freshUser);
                }

                if (isAjax) {
                    sendJsonResponse(response, HttpServletResponse.SC_OK, true, resetResp.message(), null, null, null, 0, resetResp.email());
                } else {
                    if (session != null) session.setAttribute("flash_success", resetResp.message());
                    if (currentUser != null) {
                        response.sendRedirect(request.getContextPath() + "/profile");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/login");
                    }
                }
            } else {
                throw new AppException("Unsupported forgot-password action: " + action);
            }

        } catch (AppException e) {
            if (isAjax) {
                sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } else {
                if (session != null) session.setAttribute("flash_error", e.getMessage());
                request.setAttribute("errorMessage", e.getMessage());
                request.getRequestDispatcher("/WEB-INF/views/auth/forgot-password.jsp").forward(request, response);
            }
        } catch (Exception e) {
            getServletContext().log("Unexpected error in ForgotPasswordServlet", e);
            if (isAjax) {
                sendJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "A system error occurred. Please try again.");
            } else {
                if (session != null) session.setAttribute("flash_error", "A system error occurred. Please try again.");
                request.getRequestDispatcher("/WEB-INF/views/auth/forgot-password.jsp").forward(request, response);
            }
        }
    }

    private void sendJsonResponse(HttpServletResponse response, int status, boolean success,
                                  String message, String channel, String maskedTarget,
                                  String otpCode, int expiryMinutes, String email) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            StringBuilder json = new StringBuilder("{");
            json.append("\"success\":").append(success);
            json.append(",\"message\":").append(quote(message));
            if (channel != null) json.append(",\"channel\":").append(quote(channel));
            if (maskedTarget != null) json.append(",\"maskedTarget\":").append(quote(maskedTarget));
            if (otpCode != null) json.append(",\"otpCode\":").append(quote(otpCode));
            if (expiryMinutes > 0) json.append(",\"expiryMinutes\":").append(expiryMinutes);
            if (email != null) json.append(",\"email\":").append(quote(email));
            json.append("}");
            out.print(json.toString());
            out.flush();
        }
    }

    private void sendJsonError(HttpServletResponse response, int status, String errorMessage) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.print("{\"success\":false,\"error\":" + quote(errorMessage) + "}");
            out.flush();
        }
    }

    private String quote(String str) {
        if (str == null) return "null";
        return "\"" + str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }
}
