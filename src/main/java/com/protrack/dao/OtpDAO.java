package com.protrack.dao;

import com.protrack.model.PasswordResetOtp;

import java.util.Optional;

/**
 * Data Access Object interface for One-Time Password (OTP) records.
 */
public interface OtpDAO {

    PasswordResetOtp save(PasswordResetOtp otp);

    Optional<PasswordResetOtp> findLatestValidOtp(String email, String otpCode);

    Optional<PasswordResetOtp> findLatestOtpByEmail(String email);

    void incrementAttempts(int otpId);

    void markUsed(int otpId);

    void invalidatePreviousOtps(int userId);
}
