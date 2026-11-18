<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.protrack.model.User" %>
<%@ page import="com.protrack.util.ValidationUtil" %>
<%
    User modalUser = (User) session.getAttribute("currentUser");
    String modalCp = request.getContextPath();
%>
<!-- Self-Service Password Reset Modal (Mail OTP & Contact OTP) -->
<div class="modal fade" id="forgotPasswordModal" tabindex="-1" aria-labelledby="forgotPasswordModalLabel" aria-hidden="true" data-bs-backdrop="static">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow-lg">
            <div class="modal-header bg-light border-bottom py-3">
                <div class="d-flex align-items-center gap-2">
                    <div class="rounded-circle bg-primary-subtle text-primary p-2 d-flex align-items-center justify-content-center" style="width: 36px; height: 36px;">
                        <i class="bi bi-shield-lock-fill"></i>
                    </div>
                    <div>
                        <h5 class="modal-title fw-bold text-dark fs-6 mb-0" id="forgotPasswordModalLabel">Self-Service Password Reset</h5>
                        <small class="text-muted" style="font-size: 0.78rem;">Dual-Channel Identity Verification via OTP</small>
                    </div>
                </div>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close" id="btnCloseForgotModal"></button>
            </div>

            <div class="modal-body p-4">
                <!-- Live Feedback Alerts -->
                <div id="forgotAlertContainer"></div>

                <!-- STEP 1: Select Channel & Request OTP -->
                <div id="forgotStep1">
                    <p class="text-secondary small mb-3">
                        Choose your verification method to receive a secure 6-digit One-Time Password (OTP) to reset your account password.
                    </p>

                    <!-- Account Identifier Input (if logged out) or pre-filled badge (if logged in) -->
                    <% if (modalUser != null) { %>
                        <div class="bg-light p-3 rounded-3 mb-3 border">
                            <div class="d-flex align-items-center justify-content-between mb-1">
                                <span class="small fw-semibold text-dark"><i class="bi bi-person-circle me-1 text-primary"></i> <%= ValidationUtil.escapeXml(modalUser.getFullName()) %></span>
                                <span class="badge bg-primary-subtle text-primary"><%= modalUser.getRole().getDisplayName() %></span>
                            </div>
                            <div class="small text-muted"><%= ValidationUtil.escapeXml(modalUser.getEmail()) %></div>
                            <input type="hidden" id="forgotIdentifier" value="<%= ValidationUtil.escapeXml(modalUser.getEmail()) %>">
                        </div>
                    <% } else { %>
                        <div class="mb-3">
                            <label for="forgotIdentifier" class="form-label small fw-semibold text-dark">Account Email or Contact Number</label>
                            <div class="input-group">
                                <span class="input-group-text bg-light text-muted"><i class="bi bi-person-badge"></i></span>
                                <input type="text" class="form-control" id="forgotIdentifier" placeholder="name@protrack.com or +1 555-..." required>
                            </div>
                        </div>
                    <% } %>

                    <!-- Channel Selection -->
                    <label class="form-label small fw-semibold text-dark mb-2">Select OTP Delivery Channel:</label>
                    <div class="row g-2 mb-4">
                        <div class="col-6">
                            <label class="channel-card p-3 border rounded-3 text-center w-100 h-100 d-block cursor-pointer position-relative selected" id="cardChannelMail">
                                <input type="radio" name="otpChannel" value="MAIL" class="form-check-input position-absolute top-0 end-0 m-2" checked>
                                <div class="fs-4 text-primary mb-1"><i class="bi bi-envelope-at-fill"></i></div>
                                <div class="fw-bold text-dark small">Mail OTP</div>
                                <div class="text-muted" style="font-size: 0.75rem;">Verified Email Inbox</div>
                            </label>
                        </div>
                        <div class="col-6">
                            <label class="channel-card p-3 border rounded-3 text-center w-100 h-100 d-block cursor-pointer position-relative" id="cardChannelContact">
                                <input type="radio" name="otpChannel" value="CONTACT" class="form-check-input position-absolute top-0 end-0 m-2">
                                <div class="fs-4 text-success mb-1"><i class="bi bi-phone-fill"></i></div>
                                <div class="fw-bold text-dark small">Contact OTP</div>
                                <div class="text-muted" style="font-size: 0.75rem;">SMS / Mobile Number</div>
                            </label>
                        </div>
                    </div>

                    <div class="d-grid">
                        <button type="button" class="btn btn-primary py-2 fw-semibold" id="btnSendOtp" onclick="handleSendOtp()">
                            <span class="spinner-border spinner-border-sm me-1 d-none" id="sendOtpSpinner"></span>
                            <i class="bi bi-send-fill me-1" id="sendOtpIcon"></i> Request Verification OTP
                        </button>
                    </div>
                </div>

                <!-- STEP 2: Enter OTP & Set New Password -->
                <div id="forgotStep2" class="d-none">
                    <!-- Dispatch Banner -->
                    <div class="alert alert-info py-2 px-3 small d-flex align-items-center justify-content-between mb-3" id="otpDispatchBanner">
                        <div>
                            <i class="bi bi-check2-circle me-1"></i> OTP sent to <strong id="otpMaskedTarget">...</strong>
                        </div>
                        <span class="badge bg-primary" id="otpTimerBadge">10:00</span>
                    </div>

                    <!-- Simulated Viva/Demo Gateway Banner -->
                    <div class="alert alert-warning py-2 px-3 small border mb-3" id="demoOtpBox" style="background-color: #fffbeb;">
                        <div class="d-flex align-items-center justify-content-between">
                            <div>
                                <i class="bi bi-lightning-charge-fill text-warning me-1"></i>
                                <strong>Simulated Gateway Dispatch:</strong>
                                <span class="text-secondary ms-1">Code: </span>
                                <span class="badge bg-dark fs-6 font-monospace" id="demoOtpCodeText">------</span>
                            </div>
                            <button type="button" class="btn btn-sm btn-link text-decoration-none p-0 text-primary small fw-semibold" onclick="autoFillDemoOtp()">Auto-Fill</button>
                        </div>
                    </div>

                    <!-- 6-digit OTP code input -->
                    <div class="mb-3">
                        <label for="inputOtpCode" class="form-label small fw-semibold text-dark">Enter 6-Digit OTP Code</label>
                        <input type="text" class="form-control form-control-lg text-center font-monospace fw-bold fs-4"
                               id="inputOtpCode" maxlength="6" placeholder="000000" autocomplete="one-time-code" style="letter-spacing: 6px;">
                    </div>

                    <!-- New Password Input -->
                    <div class="mb-3">
                        <label for="inputNewPass" class="form-label small fw-semibold text-dark">New Password</label>
                        <div class="input-group">
                            <span class="input-group-text bg-light text-muted"><i class="bi bi-key-fill"></i></span>
                            <input type="password" class="form-control" id="inputNewPass" minlength="8" placeholder="Min 8 characters" required>
                            <button class="btn btn-outline-secondary" type="button" onclick="togglePassVisibility('inputNewPass', this)"><i class="bi bi-eye"></i></button>
                        </div>
                    </div>

                    <!-- Confirm New Password Input -->
                    <div class="mb-4">
                        <label for="inputConfirmPass" class="form-label small fw-semibold text-dark">Confirm New Password</label>
                        <div class="input-group">
                            <span class="input-group-text bg-light text-muted"><i class="bi bi-shield-check"></i></span>
                            <input type="password" class="form-control" id="inputConfirmPass" minlength="8" placeholder="Re-enter new password" required>
                            <button class="btn btn-outline-secondary" type="button" onclick="togglePassVisibility('inputConfirmPass', this)"><i class="bi bi-eye"></i></button>
                        </div>
                    </div>

                    <div class="d-grid gap-2">
                        <button type="button" class="btn btn-primary py-2 fw-semibold" id="btnVerifyAndReset" onclick="handleVerifyAndReset()">
                            <span class="spinner-border spinner-border-sm me-1 d-none" id="verifySpinner"></span>
                            <i class="bi bi-check-circle-fill me-1" id="verifyIcon"></i> Verify OTP & Reset Password
                        </button>
                        <div class="d-flex justify-content-between align-items-center mt-2">
                            <button type="button" class="btn btn-sm btn-link text-decoration-none text-secondary p-0" onclick="backToStep1()">
                                <i class="bi bi-arrow-left me-1"></i> Change Method / Target
                            </button>
                            <button type="button" class="btn btn-sm btn-link text-decoration-none text-primary p-0" id="btnResendOtp" onclick="handleSendOtp()">
                                Resend OTP
                            </button>
                        </div>
                    </div>
                </div>

                <!-- STEP 3: Success Confirmation -->
                <div id="forgotStep3" class="d-none text-center py-3">
                    <div class="rounded-circle bg-success-subtle text-success d-inline-flex align-items-center justify-content-center mb-3" style="width: 64px; height: 64px;">
                        <i class="bi bi-shield-fill-check fs-1"></i>
                    </div>
                    <h5 class="fw-bold text-dark mb-2">Password Successfully Reset!</h5>
                    <p class="text-secondary small mb-4" id="successDesc">
                        Your account security credentials have been updated with your new password.
                    </p>
                    <div class="d-grid">
                        <% if (modalUser != null) { %>
                            <button type="button" class="btn btn-primary py-2 fw-semibold" onclick="window.location.reload()">
                                <i class="bi bi-check2 me-1"></i> Continue to Profile
                            </button>
                        <% } else { %>
                            <button type="button" class="btn btn-primary py-2 fw-semibold" onclick="window.location.href='<%= modalCp %>/login'">
                                <i class="bi bi-box-arrow-in-right me-1"></i> Proceed to Sign In
                            </button>
                        <% } %>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<style>
.channel-card {
    transition: all 0.2s ease-in-out;
    border: 2px solid #e2e8f0 !important;
}
.channel-card:hover {
    border-color: #6366f1 !important;
    background-color: #f8fafc;
}
.channel-card.selected {
    border-color: #6366f1 !important;
    background-color: #f5f3ff;
}
</style>
