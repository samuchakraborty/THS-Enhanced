package ths.server.db;

import java.sql.Connection;
import java.sql.Statement;

public class Migrations {

    /** Idempotent schema creation. Called once from Database.init() */
    public static void apply() throws Exception {
        // if your Sql constants already create these tables, keep them:
        Database.exec(Sql.CREATE_USERS);     // remove if you don't have this constant
        Database.exec(Sql.CREATE_AUDIT);
        Database.exec(Sql.CREATE_BOOKINGS);
        Database.exec(Sql.CREATE_VITALS);
        Database.exec(Sql.CREATE_RX);
        Database.exec(Sql.CREATE_REFILLS);
       Database.exec(Sql.CREATE_REFERRALS);


        // Ensure users table exists (safe to run even if it exists)
        try (Connection c = Database.ds.getConnection();
             Statement  s = c.createStatement()) {

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users(
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  email VARCHAR(190) NOT NULL UNIQUE,
                  full_name VARCHAR(120) NOT NULL,
                  role ENUM('PATIENT','SPECIALIST','ADMIN') NOT NULL DEFAULT 'PATIENT',
                  password_hash VARCHAR(100) NOT NULL,
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);
        }
    }
}
