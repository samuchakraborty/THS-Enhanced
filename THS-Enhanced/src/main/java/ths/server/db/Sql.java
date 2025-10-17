package ths.server.db;

import java.sql.Connection;
import java.sql.DriverManager;

public final class Sql {
    public static final String CREATE_USERS = """
      CREATE TABLE IF NOT EXISTS users(
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        name VARCHAR(120) NOT NULL,
        email VARCHAR(160) NOT NULL UNIQUE,
        role ENUM('PATIENT','SPECIALIST','ADMIN') NOT NULL DEFAULT 'PATIENT',
        password_hash VARCHAR(200) NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
      )""";

    public static final String CREATE_AUDIT = """
      CREATE TABLE IF NOT EXISTS audit_logs(
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        user_id BIGINT NULL,
        action VARCHAR(80) NOT NULL,
        target VARCHAR(80) NULL,
        meta JSON NULL,
        ts TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id)
      )""";

    public static final String CREATE_BOOKINGS = """
      CREATE TABLE IF NOT EXISTS bookings(
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        patient_id BIGINT NOT NULL,
        specialist_id BIGINT NOT NULL,
        start_at DATETIME NOT NULL,
        status ENUM('REQUESTED','CONFIRMED','DONE','CANCELLED') NOT NULL DEFAULT 'REQUESTED',
        notes TEXT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (patient_id) REFERENCES users(id),
        FOREIGN KEY (specialist_id) REFERENCES users(id)
      )""";

    public static final String CREATE_VITALS = """
      CREATE TABLE IF NOT EXISTS vitals(
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        patient_id BIGINT NOT NULL,
        measured_at DATETIME NOT NULL,
        pulse INT NULL,
        respiration INT NULL,
        systolic INT NULL,
        diastolic INT NULL,
        spo2 INT NULL,
        temperature DECIMAL(4,1) NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (patient_id) REFERENCES users(id),
        INDEX idx_patient_time (patient_id, measured_at)
      )""";

    public static final String CREATE_RX = """
      CREATE TABLE IF NOT EXISTS prescriptions(
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        patient_id BIGINT NOT NULL,
        specialist_id BIGINT NOT NULL,
        drug_name VARCHAR(160) NOT NULL,
        dosage VARCHAR(80) NOT NULL,
        instructions VARCHAR(255) NULL,
        refills_total INT NOT NULL DEFAULT 0,
        refills_used INT NOT NULL DEFAULT 0,
        status ENUM('ACTIVE','EXPIRED','REVOKED') NOT NULL DEFAULT 'ACTIVE',
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (patient_id) REFERENCES users(id),
        FOREIGN KEY (specialist_id) REFERENCES users(id)
      )""";

    public static final String CREATE_REFILLS = """
      CREATE TABLE IF NOT EXISTS refill_requests(
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        prescription_id BIGINT NOT NULL,
        patient_id BIGINT NOT NULL,
        requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
        decision_by BIGINT NULL,
        decision_at TIMESTAMP NULL,
        notes VARCHAR(255) NULL,
        FOREIGN KEY (prescription_id) REFERENCES prescriptions(id),
        FOREIGN KEY (patient_id) REFERENCES users(id),
        FOREIGN KEY (decision_by) REFERENCES users(id)
      )""";

    private static final String URL  = System.getProperty("B_URL",  "jdbc:mysql://localhost:3306/ths?zeroDateTimeBehavior=CONVERT_TO_NULL&serverTimezone=Australia/Sydney");
    private static final String USER = System.getProperty("B_USER", "root");
    private static final String PASS = System.getProperty("B_PASS", "password");

    public static Connection get() {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (Exception e) {
            throw new RuntimeException("DB connect failed: " + e.getMessage(), e);
        }
    }

    private Sql() {}
}
