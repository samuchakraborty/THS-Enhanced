// ths/server/dao/PrescriptionDao.java
package ths.server.dao;

import ths.server.db.Database;
import ths.server.model.Prescription;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionDao {

    public long insert(Prescription p) throws SQLException {
        String sql = """
            INSERT INTO prescriptions
            (patient_id, specialist_id, drug_name, dosage, instructions, refills_total, refills_used, status)
            VALUES (?, ?, ?, ?, ?, ?, 0, 'ACTIVE')
        """;
        try (Connection c = Database.conn(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, p.patientId);
            ps.setLong(2, p.specialistId);
            ps.setString(3, p.drugName);
            ps.setString(4, p.dosage);
            if (p.instructions == null || p.instructions.isBlank()) {
                ps.setNull(5, Types.VARCHAR);
            } else {
                ps.setString(5, p.instructions);
            }
            ps.setInt(6, p.refillsTotal
            );
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public List<Prescription> listByPatient(long patientId) throws SQLException {
        String sql = "SELECT * FROM prescriptions WHERE patient_id=? ORDER BY created_at DESC";
        List<Prescription> out = new ArrayList<>();
        try (Connection c = Database.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Prescription p = new Prescription();
                    p.id = rs.getLong("id");
                    p.patientId = rs.getLong("patient_id");
                    p.specialistId = rs.getLong("specialist_id");
                    p.drugName = rs.getString("drug_name");
                    p.dosage = rs.getString("dosage");
                    p.instructions = rs.getString("instructions");
                    p.refillsTotal = rs.getInt("refills_total");
                    p.refillsUsed = rs.getInt("refills_used");
                    
                    p.status = rs.getString("status");
                    out.add(p);
                }
            }
        }
        return out;
    }
    // ths/server/dao/PrescriptionDao.java
public void setRefillsTotal(long id, int total) throws SQLException {
    String sql = "UPDATE prescriptions SET refills_total=? WHERE id=?";
    try (Connection c = Database.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setInt(1, total);
        ps.setLong(2, id);
        ps.executeUpdate();
    }
}

public void addRefills(long id, int delta) throws SQLException {
    String sql = "UPDATE prescriptions SET refills_total = refills_total + ? WHERE id=?";
    try (Connection c = Database.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setInt(1, delta);
        ps.setLong(2, id);
        ps.executeUpdate();
    }
}

}
