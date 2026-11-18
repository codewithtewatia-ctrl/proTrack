package com.protrack.dao.impl;

import com.protrack.dao.OtpDAO;
import com.protrack.exception.DatabaseOperationException;
import com.protrack.model.PasswordResetOtp;
import com.protrack.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * JDBC implementation of OtpDAO.
 */
public class OtpDAOImpl implements OtpDAO {

    private PasswordResetOtp mapRowToOtp(ResultSet rs) throws SQLException {
        PasswordResetOtp otp = new PasswordResetOtp(
                rs.getInt("otp_id"),
                rs.getInt("user_id"),
                rs.getString("email"),
                rs.getString("contact_number"),
                rs.getString("otp_code"),
                rs.getString("channel"),
                rs.getTimestamp("expires_at"),
                rs.getBoolean("is_used"),
                rs.getInt("attempts")
        );
        otp.setCreatedAt(rs.getTimestamp("created_at"));
        return otp;
    }

    @Override
    public PasswordResetOtp save(PasswordResetOtp otp) {
        String sql = "INSERT INTO password_reset_otps (user_id, email, contact_number, otp_code, channel, expires_at, is_used, attempts) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, otp.getUserId());
            ps.setString(2, otp.getEmail().trim().toLowerCase());
            ps.setString(3, otp.getContactNumber());
            ps.setString(4, otp.getOtpCode());
            ps.setString(5, otp.getChannel());
            ps.setTimestamp(6, otp.getExpiresAt());
            ps.setBoolean(7, otp.isUsed());
            ps.setInt(8, otp.getAttempts());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        otp.setId(rs.getInt(1));
                    }
                }
            }
            return otp;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to save password reset OTP for: " + otp.getEmail(), e);
        }
    }

    @Override
    public Optional<PasswordResetOtp> findLatestValidOtp(String email, String otpCode) {
        String sql = "SELECT * FROM password_reset_otps WHERE LOWER(email) = ? AND otp_code = ? AND is_used = FALSE AND expires_at > CURRENT_TIMESTAMP ORDER BY otp_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email != null ? email.trim().toLowerCase() : "");
            ps.setString(2, otpCode != null ? otpCode.trim() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToOtp(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to query valid OTP for: " + email, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<PasswordResetOtp> findLatestOtpByEmail(String email) {
        String sql = "SELECT * FROM password_reset_otps WHERE LOWER(email) = ? AND is_used = FALSE AND expires_at > CURRENT_TIMESTAMP ORDER BY otp_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email != null ? email.trim().toLowerCase() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToOtp(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to query latest OTP for: " + email, e);
        }
        return Optional.empty();
    }

    @Override
    public void incrementAttempts(int otpId) {
        String sql = "UPDATE password_reset_otps SET attempts = attempts + 1 WHERE otp_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, otpId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to increment attempts for OTP ID: " + otpId, e);
        }
    }

    @Override
    public void markUsed(int otpId) {
        String sql = "UPDATE password_reset_otps SET is_used = TRUE WHERE otp_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, otpId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to mark OTP ID as used: " + otpId, e);
        }
    }

    @Override
    public void invalidatePreviousOtps(int userId) {
        String sql = "UPDATE password_reset_otps SET is_used = TRUE WHERE user_id = ? AND is_used = FALSE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to invalidate previous OTPs for user: " + userId, e);
        }
    }
}
