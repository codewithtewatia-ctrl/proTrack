package com.protrack.service;

import com.protrack.dao.ActivityDAO;
import com.protrack.dao.OtpDAO;
import com.protrack.dao.UserDAO;
import com.protrack.exception.AppException;
import com.protrack.exception.UserNotFoundException;
import com.protrack.model.PasswordResetOtp;
import com.protrack.model.Role;
import com.protrack.model.User;
import com.protrack.model.UserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class OtpServiceTest {

    private UserDAO mockUserDAO;
    private OtpDAO mockOtpDAO;
    private ActivityDAO mockActivityDAO;
    private OtpService otpService;

    private User testUser;

    @BeforeEach
    public void setup() {
        mockUserDAO = mock(UserDAO.class);
        mockOtpDAO = mock(OtpDAO.class);
        mockActivityDAO = mock(ActivityDAO.class);
        otpService = new OtpService(mockUserDAO, mockOtpDAO, mockActivityDAO);

        testUser = UserFactory.createUser(2, "Sarah Jenkins", "sarah.pm@protrack.com", "hashed_pwd", Role.PROJECT_MANAGER, "ACTIVE");
        testUser.setContactNumber("+1 555-019-2834");
    }

    @Test
    @DisplayName("Verify Mail OTP generation succeeds with 6-digit code and masked email")
    public void testSendOtpViaMailChannel() throws AppException {
        when(mockUserDAO.findByEmail("sarah.pm@protrack.com")).thenReturn(Optional.of(testUser));

        OtpService.SendOtpResponse response = otpService.sendOtp("sarah.pm@protrack.com", "MAIL");

        assertTrue(response.success());
        assertEquals("MAIL", response.channel());
        assertEquals("sarah.pm@protrack.com", response.email());
        assertEquals(6, response.otpCode().length());
        assertTrue(response.otpCode().matches("\\d{6}"));
        assertTrue(response.maskedTarget().contains("***"));
        assertEquals(10, response.expiryMinutes());

        verify(mockOtpDAO).invalidatePreviousOtps(2);
        verify(mockOtpDAO).save(any(PasswordResetOtp.class));
    }

    @Test
    @DisplayName("Verify Contact OTP generation succeeds with masked contact number")
    public void testSendOtpViaContactChannel() throws AppException {
        when(mockUserDAO.findByEmail("sarah.pm@protrack.com")).thenReturn(Optional.of(testUser));

        OtpService.SendOtpResponse response = otpService.sendOtp("sarah.pm@protrack.com", "CONTACT");

        assertTrue(response.success());
        assertEquals("CONTACT", response.channel());
        assertTrue(response.maskedTarget().contains("•••"));
        assertEquals(6, response.otpCode().length());

        verify(mockOtpDAO).save(any(PasswordResetOtp.class));
    }

    @Test
    @DisplayName("Verify Contact OTP fails if user has no registered contact number")
    public void testSendContactOtpWithoutPhoneNumberThrowsException() {
        testUser.setContactNumber(null);
        when(mockUserDAO.findByEmail("sarah.pm@protrack.com")).thenReturn(Optional.of(testUser));

        AppException ex = assertThrows(AppException.class, () -> {
            otpService.sendOtp("sarah.pm@protrack.com", "CONTACT");
        });

        assertTrue(ex.getMessage().contains("No contact / mobile number registered"));
        verify(mockOtpDAO, never()).save(any());
    }

    @Test
    @DisplayName("Verify send OTP throws UserNotFoundException for unknown identifier")
    public void testSendOtpNonExistentUser() {
        when(mockUserDAO.findByEmail("unknown@protrack.com")).thenReturn(Optional.empty());
        when(mockUserDAO.findByContactNumber("unknown@protrack.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            otpService.sendOtp("unknown@protrack.com", "MAIL");
        });
    }

    @Test
    @DisplayName("Verify deactivated user cannot request OTP")
    public void testSendOtpDeactivatedUserThrowsException() {
        testUser.setStatus("INACTIVE");
        when(mockUserDAO.findByEmail("sarah.pm@protrack.com")).thenReturn(Optional.of(testUser));

        AppException ex = assertThrows(AppException.class, () -> {
            otpService.sendOtp("sarah.pm@protrack.com", "MAIL");
        });

        assertTrue(ex.getMessage().contains("currently deactivated"));
    }

    @Test
    @DisplayName("Verify valid OTP reset updates password with BCrypt hash and marks OTP as used")
    public void testVerifyAndResetPasswordSuccess() throws AppException {
        when(mockUserDAO.findByEmail("sarah.pm@protrack.com")).thenReturn(Optional.of(testUser));

        Timestamp futureExpiry = new Timestamp(System.currentTimeMillis() + 600_000L);
        PasswordResetOtp validOtp = new PasswordResetOtp(
                101, 2, "sarah.pm@protrack.com", "+1 555-019-2834", "482915",
                "MAIL", futureExpiry, false, 0
        );

        when(mockOtpDAO.findLatestOtpByEmail("sarah.pm@protrack.com")).thenReturn(Optional.of(validOtp));
        when(mockUserDAO.updatePassword(eq(2), anyString())).thenReturn(true);

        OtpService.ResetPasswordResponse response = otpService.verifyAndResetPassword(
                "sarah.pm@protrack.com", "482915", "NewSecurePass@2026", "NewSecurePass@2026"
        );

        assertTrue(response.success());
        verify(mockUserDAO).updatePassword(eq(2), anyString());
        verify(mockOtpDAO).markUsed(101);
        verify(mockActivityDAO).log(eq(2), eq("PASSWORD_RESET_OTP"), anyString(), eq("AUTH"), eq(2));
    }

    @Test
    @DisplayName("Verify incorrect OTP code increments attempt counter and throws exception")
    public void testVerifyIncorrectOtpIncrementsAttempts() {
        when(mockUserDAO.findByEmail("sarah.pm@protrack.com")).thenReturn(Optional.of(testUser));

        Timestamp futureExpiry = new Timestamp(System.currentTimeMillis() + 600_000L);
        PasswordResetOtp validOtp = new PasswordResetOtp(
                101, 2, "sarah.pm@protrack.com", "+1 555-019-2834", "482915",
                "MAIL", futureExpiry, false, 1
        );

        when(mockOtpDAO.findLatestOtpByEmail("sarah.pm@protrack.com")).thenReturn(Optional.of(validOtp));

        AppException ex = assertThrows(AppException.class, () -> {
            otpService.verifyAndResetPassword("sarah.pm@protrack.com", "999999", "NewPass@123", "NewPass@123");
        });

        assertTrue(ex.getMessage().contains("Incorrect verification code"));
        verify(mockOtpDAO).incrementAttempts(101);
        verify(mockUserDAO, never()).updatePassword(anyInt(), anyString());
    }

    @Test
    @DisplayName("Verify expired OTP throws exception and rejects password reset")
    public void testVerifyExpiredOtpThrowsException() {
        when(mockUserDAO.findByEmail("sarah.pm@protrack.com")).thenReturn(Optional.of(testUser));

        Timestamp pastExpiry = new Timestamp(System.currentTimeMillis() - 60_000L); // 1 minute in the past
        PasswordResetOtp expiredOtp = new PasswordResetOtp(
                101, 2, "sarah.pm@protrack.com", "+1 555-019-2834", "482915",
                "MAIL", pastExpiry, false, 0
        );

        when(mockOtpDAO.findLatestOtpByEmail("sarah.pm@protrack.com")).thenReturn(Optional.of(expiredOtp));

        AppException ex = assertThrows(AppException.class, () -> {
            otpService.verifyAndResetPassword("sarah.pm@protrack.com", "482915", "NewPass@123", "NewPass@123");
        });

        assertTrue(ex.getMessage().contains("expired or is invalid"));
        verify(mockUserDAO, never()).updatePassword(anyInt(), anyString());
    }

    @Test
    @DisplayName("Verify password mismatch throws AppException")
    public void testVerifyPasswordMismatchThrowsException() {
        AppException ex = assertThrows(AppException.class, () -> {
            otpService.verifyAndResetPassword("sarah.pm@protrack.com", "482915", "PasswordA1", "PasswordB2");
        });

        assertTrue(ex.getMessage().contains("do not match"));
    }

    @Test
    @DisplayName("Verify short password (<8 characters) throws AppException")
    public void testVerifyShortPasswordThrowsException() {
        AppException ex = assertThrows(AppException.class, () -> {
            otpService.verifyAndResetPassword("sarah.pm@protrack.com", "482915", "short", "short");
        });

        assertTrue(ex.getMessage().contains("at least 8 characters"));
    }
}
