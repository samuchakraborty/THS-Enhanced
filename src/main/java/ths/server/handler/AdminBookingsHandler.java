package ths.server.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ths.server.Protocol;
import ths.server.db.Database;

import java.sql.*;
import java.time.LocalDateTime;

public class AdminBookingsHandler {

    private static JsonObject body(JsonObject req){
        return (req!=null && req.has("body") && req.get("body").isJsonObject()) ? req.getAsJsonObject("body") : req;
    }

    // GET: admin.bookings.list
    public static JsonObject list(JsonObject req, Long userId){
        try (Connection c = Database.conn();
             PreparedStatement ps = c.prepareStatement("""
                SELECT b.id, b.patient_id, b.specialist_id, b.start_at, b.status, b.notes,
                       p.name AS patient_name, s.name AS specialist_name
                  FROM bookings b
                  JOIN users p ON p.id = b.patient_id
                  JOIN users s ON s.id = b.specialist_id
                 ORDER BY b.start_at DESC, b.id DESC
             """)) {
            JsonArray items = new JsonArray();
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    JsonObject o = new JsonObject();
                    o.addProperty("id", rs.getLong("id"));
                    o.addProperty("patientId", rs.getLong("patient_id"));
                    o.addProperty("specialistId", rs.getLong("specialist_id"));
                    Timestamp ts = rs.getTimestamp("start_at");
                    if (ts != null) o.addProperty("startAt", ts.toLocalDateTime().toString());
                    o.addProperty("status", rs.getString("status"));
                    String notes = rs.getString("notes");
                    if (notes != null) o.addProperty("notes", notes);
                    o.addProperty("patientName", rs.getString("patient_name"));
                    o.addProperty("specialistName", rs.getString("specialist_name"));
                    items.add(o);
                }
            }
            JsonObject ok = Protocol.ok();
            ok.getAsJsonObject("data").add("items", items);
            return ok;
        } catch (Exception e){
            e.printStackTrace();
            return Protocol.error("Failed to load bookings");
        }
    }

    // POST: admin.booking.update { id, specialistId?, startAt?, status?, notes? }
    public static JsonObject update(JsonObject req, Long userId){
        try (Connection c = Database.conn()) {
            JsonObject b = body(req);
            long id = b.get("id").getAsLong();

            // Build dynamic update
            StringBuilder sql = new StringBuilder("UPDATE bookings SET ");
            java.util.List<Object> params = new java.util.ArrayList<>();
            if (b.has("specialistId") && !b.get("specialistId").isJsonNull()){
                sql.append("specialist_id=?,");
                params.add(b.get("specialistId").getAsLong());
            }
            if (b.has("startAt") && !b.get("startAt").isJsonNull()){
                sql.append("start_at=?,");
                params.add(Timestamp.valueOf(LocalDateTime.parse(b.get("startAt").getAsString())));
            }
            if (b.has("status") && !b.get("status").isJsonNull()){
                sql.append("status=?,");
                params.add(b.get("status").getAsString());
            }
            if (b.has("notes") && !b.get("notes").isJsonNull()){
                sql.append("notes=?,");
                params.add(b.get("notes").getAsString());
            }
            if (params.isEmpty()) return Protocol.ok(); // nothing to do

            sql.setLength(sql.length()-1); // remove trailing comma
            sql.append(" WHERE id=?");
            params.add(id);

            try (PreparedStatement ps = c.prepareStatement(sql.toString())){
                int idx=1;
                for (Object p : params){
                    if (p instanceof Long l) ps.setLong(idx++, l);
                    else if (p instanceof String s) ps.setString(idx++, s);
                    else if (p instanceof Timestamp t) ps.setTimestamp(idx++, t);
                }
                ps.executeUpdate();
            }
            return Protocol.ok();
        } catch (Exception e){
            e.printStackTrace();
            return Protocol.error("Update failed");
        }
    }
}
