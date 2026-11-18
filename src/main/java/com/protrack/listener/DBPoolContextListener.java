package com.protrack.listener;

import com.protrack.util.DBConnection;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/**
 * Servlet Context Listener responsible for managing the lifecycle of the
 * database connection pool (HikariCP). Ensures clean release of database connections
 * when the application is undeployed or Tomcat shuts down.
 */
public class DBPoolContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        sce.getServletContext().log("[DBPoolContextListener] ProTrack application context initialized.");
        initSchemaMigrations(sce);
    }

    private void initSchemaMigrations(ServletContextEvent sce) {
        try (java.sql.Connection conn = DBConnection.getConnection();
             java.sql.Statement st = conn.createStatement()) {

            // 1. Ensure contact_number column in users table
            try {
                st.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS contact_number VARCHAR(30) DEFAULT NULL");
            } catch (java.sql.SQLException ignored) {}

            // 2. Ensure password_reset_otps table
            st.execute("""
                CREATE TABLE IF NOT EXISTS password_reset_otps (
                    otp_id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    email VARCHAR(100) NOT NULL,
                    contact_number VARCHAR(30) DEFAULT NULL,
                    otp_code VARCHAR(10) NOT NULL,
                    channel VARCHAR(20) NOT NULL,
                    expires_at TIMESTAMP NOT NULL,
                    is_used BOOLEAN DEFAULT FALSE,
                    attempts INT DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                )
            """);

            // 3. Populate default contact numbers for demo seed users if unset
            st.executeUpdate("UPDATE users SET contact_number = '+1 555-014-9921' WHERE user_id = 1 AND (contact_number IS NULL OR contact_number = '')");
            st.executeUpdate("UPDATE users SET contact_number = '+1 555-019-2834' WHERE user_id = 2 AND (contact_number IS NULL OR contact_number = '')");
            st.executeUpdate("UPDATE users SET contact_number = '+1 555-016-3351' WHERE user_id = 3 AND (contact_number IS NULL OR contact_number = '')");
            st.executeUpdate("UPDATE users SET contact_number = '+1 555-018-7742' WHERE user_id = 4 AND (contact_number IS NULL OR contact_number = '')");
            st.executeUpdate("UPDATE users SET contact_number = '+1 555-012-6689' WHERE user_id = 5 AND (contact_number IS NULL OR contact_number = '')");
            st.executeUpdate("UPDATE users SET contact_number = '+1 555-015-8833' WHERE user_id = 6 AND (contact_number IS NULL OR contact_number = '')");
            st.executeUpdate("UPDATE users SET contact_number = '+1 555-017-4420' WHERE user_id = 7 AND (contact_number IS NULL OR contact_number = '')");

            sce.getServletContext().log("[DBPoolContextListener] Database schema migration verified (contact_number & password_reset_otps).");
        } catch (Exception e) {
            sce.getServletContext().log("[DBPoolContextListener] Notice: Schema migration check: " + e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        sce.getServletContext().log("[DBPoolContextListener] Closing HikariCP connection pool on application shutdown...");
        DBConnection.closePool();
        sce.getServletContext().log("[DBPoolContextListener] HikariCP connection pool closed.");
    }
}
