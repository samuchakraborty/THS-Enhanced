package ths.server.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ths.server.Protocol;
import ths.server.service.BookingService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import ths.server.dao.BookingDao;

public class BookingHandler {

    private static final BookingService svc = new BookingService();
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ---- helpers ----
    private static JsonObject needObj(JsonObject o, String k) {
        if (o == null || !o.has(k) || o.get(k).isJsonNull() || !o.get(k).isJsonObject()) {
            throw new IllegalArgumentException("Missing object: " + k);
        }
        return o.getAsJsonObject(k);
    }

    private static String needStr(JsonObject o, String k) {
        if (o == null || !o.has(k) || o.get(k).isJsonNull()) {
            throw new IllegalArgumentException("Missing field: " + k);
        }
        String v = o.get(k).getAsString().trim();
        if (v.isEmpty()) {
            throw new IllegalArgumentException("Missing field: " + k);
        }
        return v;
    }

    private static String optStr(JsonObject o, String k) {
        if (o == null || !o.has(k) || o.get(k).isJsonNull()) {
            return null;
        }
        String v = o.get(k).getAsString().trim();
        return v.isEmpty() ? null : v;
    }

    private static long needLong(JsonObject o, String k) {
        if (o == null || !o.has(k) || o.get(k).isJsonNull()) {
            throw new IllegalArgumentException("Missing field: " + k);
        }
        return o.get(k).getAsLong();
    }
// ths/server/handler/BookingHandler.java

    public static JsonObject create(JsonObject req, Long userId) {
        if (userId == null) {
            return Protocol.error("Unauthorized");
        }

        // 🔎 Debug to verify shape coming in
        System.out.println("[booking.create] raw req=" + req);

        // ✅ Accept either "body" (new client) or "data" (legacy)
        final JsonObject payload
                = (req != null && req.has("body") && req.get("body").isJsonObject()) ? req.getAsJsonObject("body")
                : (req != null && req.has("data") && req.get("data").isJsonObject()) ? req.getAsJsonObject("data")
                : null;

        if (payload == null) {
            return Protocol.error("Missing object: body"); // ← make the error message reflect reality
        }
        System.out.println("[booking.create] payload keys=" + payload.keySet());

        try {
            long specialistId = payload.get("specialistId").getAsLong();
            String startAtStr = payload.get("startAt").getAsString();
            String notes = (payload.has("notes") && !payload.get("notes").isJsonNull())
                    ? payload.get("notes").getAsString() : null;

            // expects ISO-8601 like 2025-10-17T09:25
            java.time.LocalDateTime startAt = java.time.LocalDateTime.parse(startAtStr);

            BookingDao dao = new BookingDao();
            long id = dao.insert(userId, specialistId, startAt, notes);
//            long id = (ths.server.dao.BookingDao.insert(userId, specialistId, startAt, notes));

            // ✅ ALWAYS include a data object on success
            com.google.gson.JsonObject data = new com.google.gson.JsonObject();
            data.addProperty("id", id);
            return Protocol.ok("ok", data);

        } catch (java.time.format.DateTimeParseException dt) {
            return Protocol.error("Invalid date-time. Use YYYY-MM-DDTHH:mm");
        } catch (Exception e) {
            e.printStackTrace();
            return Protocol.error("Create failed");
        }
    }

    public static JsonObject listMine(JsonObject req, long userId) throws Exception {
        var list = svc.listMine(userId);
        JsonArray arr = new JsonArray();
        list.forEach(b -> {
            JsonObject o = new JsonObject();
            o.addProperty("id", b.id);
            o.addProperty("patientId", b.patientId);
            o.addProperty("specialistId", b.specialistId);
            o.addProperty("startAt", b.startAt.toString());
            o.addProperty("status", b.status);
            o.addProperty("notes", b.notes);
            arr.add(o);
        });
        var ok = Protocol.ok();
        ok.getAsJsonObject("data").add("items", arr);
        return ok;
    }
}
