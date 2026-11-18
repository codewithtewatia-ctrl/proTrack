package com.protrack.servlet;

import com.protrack.exception.AppException;
import com.protrack.model.Role;
import com.protrack.model.User;
import com.protrack.model.UserFactory;
import com.protrack.service.OtpService;
import com.protrack.service.UserService;
import com.protrack.util.CSRFTokenUtil;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ForgotPasswordServletTest {

    private ForgotPasswordServlet servlet;
    private OtpService mockOtpService;
    private UserService mockUserService;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private RequestDispatcher dispatcher;
    private StringWriter responseWriter;

    @BeforeEach
    public void setup() throws IOException {
        mockOtpService = mock(OtpService.class);
        mockUserService = mock(UserService.class);
        servlet = new ForgotPasswordServlet(mockOtpService, mockUserService);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);

        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        when(request.getContextPath()).thenReturn("/protrack");
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
    }

    @Test
    @DisplayName("Verify doGet forwards to forgot-password.jsp")
    public void testDoGetForwardsToView() throws ServletException, IOException {
        servlet.doGet(request, response);

        verify(request).getRequestDispatcher("/WEB-INF/views/auth/forgot-password.jsp");
        verify(dispatcher).forward(request, response);
    }

    @Test
    @DisplayName("Verify POST action=sendOtp via AJAX returns JSON payload with OTP code")
    public void testPostSendOtpAjax() throws ServletException, IOException, AppException {
        when(request.getParameter("ajax")).thenReturn("true");
        when(request.getParameter("action")).thenReturn("sendOtp");
        when(request.getParameter("identifier")).thenReturn("sarah.pm@protrack.com");
        when(request.getParameter("channel")).thenReturn("MAIL");

        OtpService.SendOtpResponse mockResp = new OtpService.SendOtpResponse(
                true, "OTP sent successfully.", "MAIL", "s***h@protrack.com", "482915", 10, "sarah.pm@protrack.com"
        );
        when(mockOtpService.sendOtp("sarah.pm@protrack.com", "MAIL")).thenReturn(mockResp);

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json; charset=UTF-8");
        String output = responseWriter.toString();
        assertTrue(output.contains("\"success\":true"));
        assertTrue(output.contains("\"maskedTarget\":\"s***h@protrack.com\""));
        assertTrue(output.contains("\"channel\":\"MAIL\""));
        // Ensure OTP code is NOT leaked to browser JSON response
        assertFalse(output.contains("\"otpCode\""));
    }

    @Test
    @DisplayName("Verify POST action=verifyAndReset with valid CSRF succeeds via AJAX")
    public void testPostVerifyAndResetAjax() throws ServletException, IOException, AppException {
        when(request.getParameter("ajax")).thenReturn("true");
        when(request.getParameter("action")).thenReturn("verifyAndReset");
        when(request.getParameter("identifier")).thenReturn("sarah.pm@protrack.com");
        when(request.getParameter("otpCode")).thenReturn("482915");
        when(request.getParameter("newPassword")).thenReturn("NewSecurePass@2026");
        when(request.getParameter("confirmPassword")).thenReturn("NewSecurePass@2026");

        // CSRF Token setup
        String validCsrf = "csrf-token-abc-123";
        when(session.getAttribute("csrfToken")).thenReturn(validCsrf);
        when(request.getParameter("csrfToken")).thenReturn(validCsrf);

        OtpService.ResetPasswordResponse mockResp = new OtpService.ResetPasswordResponse(
                true, "Password has been successfully updated!", "sarah.pm@protrack.com"
        );
        when(mockOtpService.verifyAndResetPassword("sarah.pm@protrack.com", "482915", "NewSecurePass@2026", "NewSecurePass@2026"))
                .thenReturn(mockResp);

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String output = responseWriter.toString();
        assertTrue(output.contains("\"success\":true"));
        assertTrue(output.contains("successfully updated"));
    }

    @Test
    @DisplayName("Verify POST action=verifyAndReset without CSRF token returns 400 Bad Request JSON")
    public void testPostVerifyAndResetMissingCsrfRejected() throws ServletException, IOException {
        when(request.getParameter("ajax")).thenReturn("true");
        when(request.getParameter("action")).thenReturn("verifyAndReset");
        when(request.getParameter("identifier")).thenReturn("sarah.pm@protrack.com");
        when(session.getAttribute("csrfToken")).thenReturn("expected-token");
        when(request.getParameter("csrfToken")).thenReturn(null); // Missing CSRF

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String output = responseWriter.toString();
        assertTrue(output.contains("\"success\":false"));
        assertTrue(output.contains("Security token expired or invalid"));
    }
}
