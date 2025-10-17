package ths.server.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    public static HikariDataSource ds;

    public static void init() {
        if (ds != null) {
            return;
        }

        String url = System.getProperty("DB_URL", "jdbc:mysql://localhost:3306/ths?zeroDateTimeBehavior=CONVERT_TO_NULL&serverTimezone=Australia/Sydney");
//   "jdbc:mysql://localhost:3306/ths?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
//jdbc:mysql://localhost:3306/ths?zeroDateTimeBehavior=CONVERT_TO_NULL&serverTimezone=Australia/Sydney [root on Default schema]
        String user = System.getProperty("DB_USER", "root");
        String pass = System.getProperty("DB_PASS", "password");

        try {
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(url);
            cfg.setUsername(user);
            cfg.setPassword(pass);
            cfg.setMaximumPoolSize(8);
            cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");

            ds = new HikariDataSource(cfg);

            // Run idempotent migrations (creates tables if missing)
            Migrations.apply();

            System.out.println("[DB] Connected and migrations complete.");
        } catch (Exception e) {
            System.err.println("[DB] FAILED to initialize: " + e.getMessage());
            e.printStackTrace();
            ds = null;
            throw new RuntimeException("DB init failed", e);
        }
    }

    /**
     * Convenience method to get a connection from the pool.
     */
    public static Connection conn() throws SQLException {
        if (ds == null) {
            throw new IllegalStateException("Database not initialized. Call Database.init() first.");
        }
        return ds.getConnection();
    }

    public static void exec(String sql) throws Exception {
        if (ds == null) {
            throw new IllegalStateException("Database not initialized. Call Database.init() first.");
        }
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate(sql);
        }
    }
}
