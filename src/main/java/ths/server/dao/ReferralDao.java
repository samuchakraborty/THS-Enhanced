// src/main/java/ths/server/dao/ReferralDao.java
package ths.server.dao;

import ths.server.db.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReferralDao {

    public static class Referral {
        public long id, patientId;
        public String facility, notes, date, time; // date = "YYYY-MM-DD", time = "HH:MM"
    }

    public long insert(long patientId, String facility, String date, String time, String notes) throws SQLException {
        final String sql = """
            INSERT INTO referrals (patient_id, facility, date, time, notes, created_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;
        try (Connection c = Database.conn();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, patientId);
            ps.setString(2, facility);
            ps.setString(3, date);
            ps.setString(4, time);
            if (notes == null || notes.isBlank()) ps.setNull(5, Types.VARCHAR); else ps.setString(5, notes);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getLong(1); }
        }
    }

    public void delete(long id) throws SQLException {
        try (Connection c = Database.conn();
             PreparedStatement ps = c.prepareStatement("DELETE FROM referrals WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public List<Referral> listByPatient(long patientId) throws SQLException {
        final String sql = """
            SELECT id, patient_id, facility, date, time, notes
            FROM referrals
            WHERE patient_id = ?
            ORDER BY date DESC, time DESC, id DESC
        """;
        List<Referral> out = new ArrayList<>();
        try (Connection c = Database.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Referral r = new Referral();
                    r.id = rs.getLong("id");
                    r.patientId = rs.getLong("patient_id");
                    r.facility = rs.getString("facility");
                    r.date = rs.getString("date");
                    r.time = rs.getString("time");
                    r.notes = rs.getString("notes");
                    out.add(r);
                }
            }
        }
        return out;
    }
}
