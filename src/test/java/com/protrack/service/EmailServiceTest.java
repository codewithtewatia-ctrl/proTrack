package com.protrack.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EmailServiceTest {

    @Test
    @DisplayName("Verify unconfigured EmailService does not attempt network connect and returns false")
    public void testUnconfiguredEmailServiceFallback() {
        EmailService service = new EmailService("smtp.gmail.com", 587, "", "", "");
        assertFalse(service.isConfigured());

        boolean result = service.sendOtpEmail("test@example.com", "Test User", "123456", 10);
        assertFalse(result, "Unconfigured EmailService should safely return false without throwing");
    }

    @Test
    @DisplayName("Verify configured EmailService recognizes valid credentials")
    public void testConfiguredEmailService() {
        EmailService service = new EmailService("smtp.gmail.com", 587, "protrack@gmail.com", "app_pass_1234", "protrack@gmail.com");
        assertTrue(service.isConfigured());
        assertEquals("smtp.gmail.com", service.getHost());
        assertEquals(587, service.getPort());
        assertEquals("protrack@gmail.com", service.getUsername());
        assertEquals("protrack@gmail.com", service.getFromEmail());
    }

    @Test
    @DisplayName("Verify default constructor loads mail properties")
    public void testDefaultConstructor() {
        EmailService service = new EmailService();
        assertNotNull(service.getHost());
        assertEquals(587, service.getPort());
    }
}
