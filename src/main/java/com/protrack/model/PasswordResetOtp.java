package com.protrack.model;

import java.sql.Timestamp;

/**
 * Model representing a one-time password (OTP) record for self-service password recovery.
 * Supports dual-channel verification: MAIL (Email OTP) and CONTACT (SMS/Phone OTP).
 */
public class PasswordResetOtp extends BaseEntity {
    private static final long serialVersionUID = 1L;

    public static final String CHANNEL_MAIL = "MAIL";
    public static final String CHANNEL_CONTACT = "CONTACT";

    private int userId;
    private String email;
    private String contactNumber;
    private String otpCode;
    private String channel; // MAIL or CONTACT
    private Timestamp expiresAt;
    private boolean isUsed;
    private int attempts;

    public PasswordResetOtp() {
        super();
        this.channel = CHANNEL_MAIL;
        this.isUsed = false;
        this.attempts = 0;
    }

    public PasswordResetOtp(int id, int userId, String email, String contactNumber, String otpCode,
                            String channel, Timestamp expiresAt, boolean isUsed, int attempts) {
        super(id);
        this.userId = userId;
        this.email = email;
        this.contactNumber = contactNumber;
        this.otpCode = otpCode;
        this.channel = channel != null ? channel.toUpperCase() : CHANNEL_MAIL;
        this.expiresAt = expiresAt;
        this.isUsed = isUsed;
        this.attempts = attempts;
    }

    public boolean isExpired() {
        return expiresAt != null && System.currentTimeMillis() > expiresAt.getTime();
    }

    public boolean isValid() {
        return !isUsed && !isExpired() && attempts < 5;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    @Override
    public String toString() {
        return "PasswordResetOtp{" +
                "id=" + id +
                ", userId=" + userId +
                ", email='" + email + '\'' +
                ", channel='" + channel + '\'' +
                ", isUsed=" + isUsed +
                ", attempts=" + attempts +
                '}';
    }
}
