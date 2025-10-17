// ths/server/dao/VitalDao.java
package ths.server.dao;

import ths.server.db.Database;
import ths.server.model.VitalSign;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VitalDao {

    public long insert(VitalSign v) throws SQLException {
        String sql = """
            INSERT INTO vitals(patient_id, measured_at, pulse, respiration, temperature, systolic, diastolic)
            VALUES(?,?,?,?,?,?,?)
        """;
        try (Connection c = Database.conn();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, v.patientId);
            ps.setTimestamp(2, Timestamp.valueOf(v.measuredAt));
            if (v.pulse == null)        ps.setNull(3, Types.INTEGER); else ps.setInt(3, v.pulse);
            if (v.respiration == null)  ps.setNull(4, Types.INTEGER); else ps.setInt(4, v.respiration);
            if (v.temperature == null)  ps.setNull(5, Types.DECIMAL); else ps.setDouble(5, v.temperature);
            if (v.systolic == null)     ps.setNull(6, Types.INTEGER); else ps.setInt(6, v.systolic);
            if (v.diastolic == null)    ps.setNull(7, Types.INTEGER); else ps.setInt(7, v.diastolic);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getLong(1); }
        }
    }

    public List<VitalSign> listByPatient(long patientId) throws SQLException {
        String sql = "SELECT * FROM vitals WHERE patient_id=? ORDER BY measured_at DESC";
        List<VitalSign> out = new ArrayList<>();
        try (Connection c = Database.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    VitalSign v = new VitalSign();
                    v.id = rs.getLong("id");
                    v.patientId = rs.getLong("patient_id");
                    v.measuredAt = rs.getTimestamp("measured_at").toLocalDateTime();
                    v.pulse = (Integer) rs.getObject("pulse");
                    v.respiration = (Integer) rs.getObject("respiration");
                    v.temperature = rs.getObject("temperature") == null ? null : rs.getDouble("temperature");
                    v.systolic = (Integer) rs.getObject("systolic");
                    v.diastolic = (Integer) rs.getObject("diastolic");
                    out.add(v);
                }
            }
        }
        return out;
    }
}
