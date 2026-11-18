package com.protrack.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Service managing outbound email delivery via SMTP (RFC 5321/3207)
 * using standard Java networking with TLS/SSL encryption and authentication.
 * Sends HTML formatted verification OTP codes to user email addresses.
 */
public class EmailService {

    private String host = "smtp.gmail.com";
    private int port = 587;
    private boolean auth = true;
    private boolean starttls = true;
    private boolean ssl = false;
    private String username = "";
    private String password = "";
    private String fromEmail = "";
    private String fromName = "ProTrack Enterprise Security";
    private boolean enabled = true;

    public EmailService() {
        loadConfig();
    }

    public EmailService(String host, int port, String username, String password, String fromEmail) {
        this.host = host;
        this.port = port;
        this.username = username != null ? username.trim() : "";
        this.password = password != null ? password.trim() : "";
        this.fromEmail = fromEmail != null ? fromEmail.trim() : "";
        this.auth = !this.username.isBlank();
    }

    private void loadConfig() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("mail.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                this.enabled = Boolean.parseBoolean(props.getProperty("mail.smtp.enabled", "true"));
                this.host = props.getProperty("mail.smtp.host", "smtp.gmail.com").trim();
                this.port = Integer.parseInt(props.getProperty("mail.smtp.port", "587").trim());
                this.auth = Boolean.parseBoolean(props.getProperty("mail.smtp.auth", "true"));
                this.starttls = Boolean.parseBoolean(props.getProperty("mail.smtp.starttls.enable", "true"));
                this.ssl = Boolean.parseBoolean(props.getProperty("mail.smtp.ssl.enable", "false"));
                this.username = props.getProperty("mail.smtp.username", "").trim();
                this.password = props.getProperty("mail.smtp.password", "").trim();
                this.fromEmail = props.getProperty("mail.from.email", "").trim();
                this.fromName = props.getProperty("mail.from.name", "ProTrack Enterprise Security").trim();
            }
        } catch (Exception e) {
            System.err.println("[EmailService] Note: mail.properties not loaded: " + e.getMessage());
        }

        // Environment variables take precedence if present
        String envHost = System.getenv("MAIL_SMTP_HOST");
        if (envHost != null && !envHost.isBlank()) this.host = envHost.trim();

        String envPort = System.getenv("MAIL_SMTP_PORT");
        if (envPort != null && !envPort.isBlank()) {
            try { this.port = Integer.parseInt(envPort.trim()); } catch (NumberFormatException ignored) {}
        }

        String envUser = System.getenv("MAIL_SMTP_USERNAME");
        if (envUser != null && !envUser.isBlank()) this.username = envUser.trim();

        String envPass = System.getenv("MAIL_SMTP_PASSWORD");
        if (envPass != null && !envPass.isBlank()) this.password = envPass.trim();

        String envFrom = System.getenv("MAIL_FROM_EMAIL");
        if (envFrom != null && !envFrom.isBlank()) this.fromEmail = envFrom.trim();

        if (this.fromEmail.isBlank() && !this.username.isBlank()) {
            this.fromEmail = this.username;
        }
    }

    public boolean isConfigured() {
        return enabled && !username.isBlank() && !password.isBlank();
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getUsername() { return username; }
    public String getFromEmail() { return fromEmail; }

    /**
     * Sends password reset OTP email to recipient.
     * Returns true if sent successfully via SMTP, false if fallback/simulated.
     */
    public boolean sendOtpEmail(String recipientEmail, String recipientName, String otpCode, int expiryMinutes) {
        if (!isConfigured()) {
            System.out.printf("[EmailService] SMTP credentials not set in mail.properties (mail.smtp.username / password). "
                    + "OTP for %s logged on server: %s%n", recipientEmail, otpCode);
            return false;
        }

        String subject = "ProTrack Password Reset Code: " + otpCode;
        String htmlBody = buildOtpHtmlBody(recipientName, otpCode, expiryMinutes);

        try {
            sendSmtp(recipientEmail, subject, htmlBody);
            System.out.printf("[EmailService] SUCCESS: Outbound OTP email sent to %s via %s:%d%n",
                    recipientEmail, host, port);
            return true;
        } catch (Exception e) {
            System.err.printf("[EmailService] SMTP dispatch to %s failed: %s%n", recipientEmail, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void sendSmtp(String to, String subject, String bodyHtml) throws Exception {
        Socket socket = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;

        try {
            if (ssl || port == 465) {
                SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
                socket = ssf.createSocket(host, port);
            } else {
                socket = new Socket(host, port);
            }
            socket.setSoTimeout(15000); // 15 seconds timeout

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            readResponse(reader, 220); // 220 Service Ready

            sendCommand(writer, "EHLO localhost");
            readResponse(reader, 250);

            if (starttls && !ssl && port != 465) {
                sendCommand(writer, "STARTTLS");
                readResponse(reader, 220);

                SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket sslSocket = (SSLSocket) ssf.createSocket(socket, host, port, true);
                sslSocket.setUseClientMode(true);
                sslSocket.startHandshake();

                socket = sslSocket;
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

                sendCommand(writer, "EHLO localhost");
                readResponse(reader, 250);
            }

            if (auth && !username.isBlank()) {
                sendCommand(writer, "AUTH LOGIN");
                readResponse(reader, 334);

                String b64User = Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8));
                sendCommand(writer, b64User);
                readResponse(reader, 334);

                String cleanPass = password.replace(" ", ""); // Strip spaces if user copied Google App Password with spaces
                String b64Pass = Base64.getEncoder().encodeToString(cleanPass.getBytes(StandardCharsets.UTF_8));
                sendCommand(writer, b64Pass);
                readResponse(reader, 235);
            }

            String fromAddr = fromEmail.isBlank() ? username : fromEmail;
            sendCommand(writer, "MAIL FROM:<" + fromAddr + ">");
            readResponse(reader, 250);

            sendCommand(writer, "RCPT TO:<" + to + ">");
            readResponse(reader, 250);

            sendCommand(writer, "DATA");
            readResponse(reader, 354);

            writer.write("From: " + fromName + " <" + fromAddr + ">\r\n");
            writer.write("To: <" + to + ">\r\n");
            writer.write("Subject: " + subject + "\r\n");
            writer.write("MIME-Version: 1.0\r\n");
            writer.write("Content-Type: text/html; charset=UTF-8\r\n");
            writer.write("Content-Transfer-Encoding: 8bit\r\n");
            writer.write("\r\n");
            writer.write(bodyHtml);
            writer.write("\r\n.\r\n");
            writer.flush();
            readResponse(reader, 250);

            sendCommand(writer, "QUIT");
            try { readResponse(reader, 221); } catch (Exception ignored) {}

        } finally {
            if (writer != null) try { writer.close(); } catch (Exception ignored) {}
            if (reader != null) try { reader.close(); } catch (Exception ignored) {}
            if (socket != null && !socket.isClosed()) try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private void sendCommand(BufferedWriter writer, String cmd) throws Exception {
        writer.write(cmd + "\r\n");
        writer.flush();
    }

    private String readResponse(BufferedReader reader, int expectedCode) throws Exception {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
            if (line.length() >= 4 && line.charAt(3) == ' ') {
                break;
            }
        }
        String full = sb.toString().trim();
        if (!full.startsWith(String.valueOf(expectedCode))) {
            throw new RuntimeException("SMTP error (expected " + expectedCode + "): " + full);
        }
        return full;
    }

    private String buildOtpHtmlBody(String name, String otpCode, int expiryMinutes) {
        String displayName = (name != null && !name.isBlank()) ? name : "User";
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; background-color: #f1f5f9; margin: 0; padding: 32px 16px;'>"
                + "  <div style='max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 12px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06);'>"
                + "    <div style='background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%); padding: 24px 20px; text-align: center; color: #ffffff;'>"
                + "      <h1 style='margin: 0; font-size: 22px; font-weight: 700; letter-spacing: -0.5px;'>ProTrack Enterprise</h1>"
                + "      <p style='margin: 4px 0 0 0; font-size: 13px; opacity: 0.9;'>Self-Service Password Recovery</p>"
                + "    </div>"
                + "    <div style='padding: 28px 24px;'>"
                + "      <p style='margin: 0 0 16px 0; font-size: 15px; color: #334155;'>Hello <strong>" + displayName + "</strong>,</p>"
                + "      <p style='margin: 0 0 20px 0; font-size: 14px; color: #475569; line-height: 1.5;'>You requested a password reset for your ProTrack account. Use the 6-digit verification code below to reset your password:</p>"
                + "      <div style='background: #f8fafc; border: 2px dashed #cbd5e1; border-radius: 8px; padding: 20px 16px; text-align: center; margin-bottom: 24px;'>"
                + "        <div style='font-size: 12px; text-transform: uppercase; letter-spacing: 1.5px; color: #64748b; margin-bottom: 6px; font-weight: 600;'>Your One-Time Password</div>"
                + "        <div style='font-family: monospace; font-size: 34px; font-weight: 700; letter-spacing: 8px; color: #4f46e5;'>" + otpCode + "</div>"
                + "      </div>"
                + "      <p style='margin: 0 0 12px 0; font-size: 13px; color: #64748b; line-height: 1.4;'>This OTP code is valid for <strong style='color: #0f172a;'>" + expiryMinutes + " minutes</strong> and can only be used once.</p>"
                + "      <p style='margin: 0; font-size: 12px; color: #94a3b8; line-height: 1.4;'>If you did not request this password reset, please ignore this email or contact your administrator immediately.</p>"
                + "    </div>"
                + "    <div style='background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 16px 24px; text-align: center; font-size: 12px; color: #94a3b8;'>"
                + "      &copy; 2026 ProTrack Enterprise Project Management &bull; Automated Security Notification"
                + "    </div>"
                + "  </div>"
                + "</body>"
                + "</html>";
    }
}
