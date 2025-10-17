package ths.server.dao;

import ths.server.db.Database;
import ths.server.model.RefillRequest;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RefillDao {
    public long insert(long rxId, long patientId) throws SQLException {
        String sql = "INSERT INTO refill_requests(prescription_id,patient_id) VALUES(?,?)";
        try (Connection c = Database.conn(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, rxId); ps.setLong(2, patientId);
            ps.executeUpdate(); try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getLong(1); }
        }
    }

    public void decide(long id, String status, long specialistId, String notes) throws SQLException {
        String sql = "UPDATE refill_requests SET status=?, decision_by=?, decision_at=NOW(), notes=? WHERE id=?";
        try (Connection c = Database.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status); ps.setLong(2, specialistId); ps.setString(3, notes); ps.setLong(4, id);
            ps.executeUpdate();
        }
    }

    public List<RefillRequest> listByPatient(long patientId) throws SQLException {
        String sql = "SELECT * FROM refill_requests WHERE patient_id=? ORDER BY requested_at DESC";
        List<RefillRequest> out = new ArrayList<>();
        try (Connection c = Database.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    private RefillRequest map(ResultSet r) throws SQLException {
        RefillRequest rr = new RefillRequest();
        rr.id = r.getLong("id");
        rr.prescriptionId = r.getLong("prescription_id");
        rr.patientId = r.getLong("patient_id");
        rr.status = r.getString("status");
        rr.decisionBy = (Long) r.getObject("decision_by");
        rr.decisionAt = r.getTimestamp("decision_at")==null?null:r.getTimestamp("decision_at").toInstant();
        rr.notes = r.getString("notes");
        return rr;
    }
}
