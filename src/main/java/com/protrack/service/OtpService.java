package com.protrack.service;

import com.protrack.dao.ActivityDAO;
import com.protrack.dao.OtpDAO;
import com.protrack.dao.UserDAO;
import com.protrack.dao.impl.ActivityDAOImpl;
import com.protrack.dao.impl.OtpDAOImpl;
import com.protrack.dao.impl.UserDAOImpl;
import com.protrack.exception.AppException;
import com.protrack.exception.UserNotFoundException;
import com.protrack.model.PasswordResetOtp;
import com.protrack.model.User;
import com.protrack.util.BCrypt;
import com.protrack.util.ValidationUtil;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Optional;

/**
 * Service managing dual-channel One-Time Password (OTP) verification
 * for self-service password recovery (Mail OTP & Contact OTP).
 */
public class OtpService {

    private final UserDAO userDAO;
    private final OtpDAO otpDAO;
    private final ActivityDAO activityDAO;
    private final SecureRandom secureRandom = new SecureRandom();

    public static final int OTP_EXPIRY_MINUTES = 10;
    public static final int MAX_ATTEMPTS = 5;

    public OtpService() {
        this.userDAO = new UserDAOImpl();
        this.otpDAO = new OtpDAOImpl();
        this.activityDAO = new ActivityDAOImpl();
    }

    public OtpService(UserDAO userDAO, OtpDAO otpDAO, ActivityDAO activityDAO) {
        this.userDAO = userDAO;
        this.otpDAO = otpDAO;
        this.activityDAO = activityDAO;
    }

    /**
     * DTO representing result of OTP dispatch.
     */
    public record SendOtpResponse(
            boolean success,
            String message,
            String channel,
            String maskedTarget,
            String otpCode,
            int expiryMinutes,
            String email
    ) {}

    /**
     * DTO representing result of password reset.
     */
    public record ResetPasswordResponse(
            boolean success,
            String message,
            String email
    ) {}

    /**
     * Sends an OTP code to user via selected channel (MAIL or CONTACT).
     */
    public SendOtpResponse sendOtp(String identifier, String channel) throws AppException {
        if (!ValidationUtil.isNotEmpty(identifier)) {
            throw new AppException("Please provide an email address or registered contact number.");
        }

        String ch = (channel != null && PasswordResetOtp.CHANNEL_CONTACT.equalsIgnoreCase(channel.trim()))
                ? PasswordResetOtp.CHANNEL_CONTACT
                : PasswordResetOtp.CHANNEL_MAIL;

        String idClean = identifier.trim();
        Optional<User> userOpt = userDAO.findByEmail(idClean.toLowerCase());
        if (userOpt.isEmpty()) {
            userOpt = userDAO.findByContactNumber(idClean);
        }

        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("No active account found for: " + identifier);
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            throw new AppException("This account is currently deactivated. Please contact your system administrator.");
        }

        String target;
        String maskedTarget;
        if (PasswordResetOtp.CHANNEL_CONTACT.equals(ch)) {
            if (!user.hasContactNumber()) {
                throw new AppException("No contact / mobile number registered for " + user.getEmail() + ". Please select Mail OTP or update your contact in your profile.");
            }
            target = user.getContactNumber();
            maskedTarget = maskContactNumber(target);
        } else {
            target = user.getEmail();
            maskedTarget = maskEmail(target);
        }

        // Invalidate any active previous OTPs for this user
        otpDAO.invalidatePreviousOtps(user.getId());

        // Generate cryptographically random 6-digit numeric OTP
        int codeInt = 100000 + secureRandom.nextInt(900000);
        String otpCode = String.valueOf(codeInt);

        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + (OTP_EXPIRY_MINUTES * 60 * 1000L));

        PasswordResetOtp otp = new PasswordResetOtp(
                0,
                user.getId(),
                user.getEmail(),
                user.getContactNumber(),
                otpCode,
                ch,
                expiresAt,
                false,
                0
        );

        otpDAO.save(otp);

        // Simulated Dispatch Logging for demonstration & testing
        System.out.printf("[OtpService] DISPATCH %s OTP -> %s (%s) [Code: %s, Expires in: %dm]%n",
                ch, target, maskedTarget, otpCode, OTP_EXPIRY_MINUTES);

        activityDAO.log(user.getId(), "OTP_REQUEST",
                "Requested " + ch + " OTP for password reset.", "AUTH", user.getId());

        String message = String.format("A 6-digit verification code has been dispatched via %s to %s.",
                PasswordResetOtp.CHANNEL_CONTACT.equals(ch) ? "SMS / Contact" : "Email", maskedTarget);

        return new SendOtpResponse(true, message, ch, maskedTarget, otpCode, OTP_EXPIRY_MINUTES, user.getEmail());
    }

    /**
     * Verifies provided OTP and updates the user's password with BCrypt hash.
     */
    public ResetPasswordResponse verifyAndResetPassword(String identifier, String otpCode,
                                                         String newPassword, String confirmPassword) throws AppException {
        if (!ValidationUtil.isNotEmpty(identifier)) {
            throw new AppException("Account identifier (email or contact) is required.");
        }
        if (!ValidationUtil.isNotEmpty(otpCode)) {
            throw new AppException("Please enter the 6-digit verification OTP.");
        }
        if (!ValidationUtil.isNotEmpty(newPassword)) {
            throw new AppException("New password cannot be empty.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new AppException("New passwords do not match.");
        }
        if (!ValidationUtil.isStrongPassword(newPassword)) {
            throw new AppException("New password must be at least 8 characters long.");
        }

        String idClean = identifier.trim();
        Optional<User> userOpt = userDAO.findByEmail(idClean.toLowerCase());
        if (userOpt.isEmpty()) {
            userOpt = userDAO.findByContactNumber(idClean);
        }

        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("No account found matching: " + identifier);
        }

        User user = userOpt.get();
        String codeClean = otpCode.trim();

        Optional<PasswordResetOtp> otpOpt = otpDAO.findLatestOtpByEmail(user.getEmail());
        if (otpOpt.isEmpty() || otpOpt.get().isExpired()) {
            throw new AppException("The verification code has expired or is invalid. Please request a new OTP.");
        }

        PasswordResetOtp otp = otpOpt.get();
        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            throw new AppException("Maximum verification attempts exceeded for this code. Please request a new OTP.");
        }

        if (!otp.getOtpCode().equals(codeClean)) {
            otpDAO.incrementAttempts(otp.getId());
            int remaining = MAX_ATTEMPTS - (otp.getAttempts() + 1);
            throw new AppException("Incorrect verification code. " + (remaining > 0 ? remaining + " attempt(s) remaining." : "Please request a new OTP."));
        }

        // OTP is verified! Atomically update user's password with BCrypt
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(10));
        boolean updated = userDAO.updatePassword(user.getId(), hashedPassword);
        if (!updated) {
            throw new AppException("Failed to update password in database. Please try again.");
        }

        // Mark OTP single-use consumption
        otpDAO.markUsed(otp.getId());

        // Audit log
        activityDAO.log(user.getId(), "PASSWORD_RESET_OTP",
                user.getFullName() + " successfully reset account password using " + otp.getChannel() + " OTP.",
                "AUTH", user.getId());

        return new ResetPasswordResponse(true, "Password has been successfully updated! You can now access your account with your new password.", user.getEmail());
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email != null ? email : "";
        int atIndex = email.indexOf('@');
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }

    public static String maskContactNumber(String contact) {
        if (contact == null || contact.trim().isEmpty()) return "";
        String trimmed = contact.trim();
        if (trimmed.length() <= 4) return "•••-" + trimmed;
        String last4 = trimmed.substring(trimmed.length() - 4);
        String prefix = trimmed.substring(0, Math.min(3, trimmed.length() - 4));
        return prefix + " •••-•••-" + last4;
    }
}
