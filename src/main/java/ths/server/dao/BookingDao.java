package ths.server.dao;

import ths.server.db.Database;
import ths.server.model.Booking;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookingDao {

    // BookingDao.java
    public long insert(long patientId, long specialistId,
            java.time.LocalDateTime startAt, String notes) throws SQLException {
        Booking b = new Booking();
        b.patientId = patientId;
        b.specialistId = specialistId;
        b.startAt = startAt;
        b.status = "REQUESTED"; // default matches ENUM
        b.notes = notes;
        return insert(b);
    }

    private static String normalizeStatus(String s) {
        if (s == null || s.isBlank()) {
            return "REQUESTED";
        }
        return switch (s.toUpperCase()) {
            case "REQUESTED", "PENDING", "P" ->
                "REQUESTED";
            case "CONFIRMED", "C" ->
                "CONFIRMED";
            case "CANCELLED", "X" ->
                "CANCELLED";
            case "DONE", "COMPLETED", "D" ->
                "DONE";
            default ->
                "REQUESTED";
        };
    }

    public long insert(Booking b) throws SQLException {
        String sql = "INSERT INTO bookings(patient_id,specialist_id,start_at,status,notes) VALUES(?,?,?,?,?)";
        try (Connection c = Database.conn(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            String status = normalizeStatus(b.status);
            ps.setLong(1, b.patientId);
            ps.setLong(2, b.specialistId);
            ps.setTimestamp(3, Timestamp.valueOf(b.startAt));
            ps.setString(4, status);
            if (b.notes == null || b.notes.isBlank()) {
                ps.setNull(5, java.sql.Types.VARCHAR);
            } else {
                ps.setString(5, b.notes);
            }

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public List<Booking> listByPatient(long patientId) throws SQLException {
        String sql = "SELECT * FROM bookings WHERE patient_id=? ORDER BY start_at DESC";
        List<Booking> out = new ArrayList<>();
        try (Connection c = Database.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        }
        return out;
    }

    private Booking map(ResultSet r) throws SQLException {
        Booking b = new Booking();
        b.id = r.getLong("id");
        b.patientId = r.getLong("patient_id");
        b.specialistId = r.getLong("specialist_id");
        b.startAt = r.getTimestamp("start_at").toLocalDateTime();
        b.status = r.getString("status");
        b.notes = r.getString("notes");
        return b;
    }

}
