<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.protrack.model.User" %>
<%@ page import="com.protrack.util.ValidationUtil" %>
<%
    String step = (String) request.getAttribute("step");
    if (step == null) step = "request";
    String identifier = (String) request.getAttribute("identifier");
    String maskedTarget = (String) request.getAttribute("maskedTarget");
    String channel = (String) request.getAttribute("channel");
    String error = (String) request.getAttribute("errorMessage");
    String success = (String) session.getAttribute("flash_success");
    if (success != null) session.removeAttribute("flash_success");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reset Password | ProTrack Enterprise</title>
    
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
    <!-- Google Fonts: Inter -->
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <!-- ProTrack Styles -->
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
    <meta name="csrf-token" content="${sessionScope.csrfToken}">
</head>
<body class="bg-light">

<div class="container min-vh-100 d-flex align-items-center justify-content-center py-5">
    <div class="card border-0 shadow-sm rounded-4" style="max-width: 480px; width: 100%;">
        <div class="card-body p-4 p-md-5">
            <!-- Brand -->
            <div class="text-center mb-4">
                <a href="<%= request.getContextPath() %>/" class="d-inline-flex align-items-center gap-2 text-decoration-none mb-3">
                    <div class="brand-icon rounded-3 bg-primary text-white d-flex align-items-center justify-content-center" style="width: 40px; height: 40px;">
                        <i class="bi bi-kanban-fill fs-5"></i>
                    </div>
                    <span class="fs-4 fw-bold text-dark">ProTrack</span>
                </a>
                <h4 class="fw-bold text-dark mb-1">Forgot Password</h4>
                <p class="text-secondary small mb-0">Reset account password via Mail OTP or Contact OTP</p>
            </div>

            <!-- Alerts -->
            <% if (error != null) { %>
            <div class="alert alert-danger py-2 px-3 small d-flex align-items-center mb-3">
                <i class="bi bi-exclamation-triangle-fill me-2 fs-5"></i>
                <div><%= ValidationUtil.escapeXml(error) %></div>
            </div>
            <% } %>
            <% if (success != null) { %>
            <div class="alert alert-success py-2 px-3 small d-flex align-items-center mb-3">
                <i class="bi bi-check-circle-fill me-2 fs-5"></i>
                <div><%= ValidationUtil.escapeXml(success) %></div>
            </div>
            <% } %>

            <!-- In-page Form Trigger / Modal -->
            <jsp:include page="../common/forgot-password-modal.jsp" />

            <div class="text-center py-4">
                <button type="button" class="btn btn-primary px-4 py-2 fw-semibold" data-bs-toggle="modal" data-bs-target="#forgotPasswordModal">
                    <i class="bi bi-shield-lock me-1"></i> Open Password Reset Wizard
                </button>
            </div>

            <div class="text-center pt-3 border-top mt-3">
                <a href="<%= request.getContextPath() %>/login" class="text-decoration-none small text-secondary">
                    <i class="bi bi-arrow-left me-1"></i> Back to Login
                </a>
            </div>
        </div>
    </div>
</div>

<!-- Bootstrap 5 Bundle JS -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<!-- ProTrack Main Script -->
<script src="<%= request.getContextPath() %>/assets/js/main.js"></script>
<script>
document.addEventListener('DOMContentLoaded', () => {
    // Automatically launch modal on page load
    const modalEl = document.getElementById('forgotPasswordModal');
    if (modalEl) {
        const modal = new bootstrap.Modal(modalEl);
        modal.show();
    }
});
</script>
</body>
</html>
