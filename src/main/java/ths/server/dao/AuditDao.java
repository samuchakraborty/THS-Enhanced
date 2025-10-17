package ths.server.dao;

import ths.server.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class AuditDao {
    public void log(Long userId, String action, String target, String metaJson) throws Exception {
        String sql = "INSERT INTO audit_logs(user_id,action,target,meta) VALUES (?,?,?,CAST(? AS JSON))";
        try (Connection c = Database.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (userId == null) ps.setObject(1, null); else ps.setLong(1, userId);
            ps.setString(2, action);
            ps.setString(3, target);
            ps.setString(4, metaJson);
            ps.executeUpdate();
        }
    }
}
 