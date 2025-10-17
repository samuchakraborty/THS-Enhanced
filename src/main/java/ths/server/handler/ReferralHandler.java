// src/main/java/ths/server/handler/ReferralHandler.java
package ths.server.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ths.server.Protocol;
import ths.server.dao.ReferralDao;

public class ReferralHandler {

    private static JsonObject bodyOf(JsonObject req) {
        return (req != null && req.has("body") && req.get("body").isJsonObject())
                ? req.getAsJsonObject("body") : req;
    }

    // GET: referral.listMine  { patientId? }  (defaults to caller id)
    public static JsonObject listMine(JsonObject req, Long userId) {
        if (userId == null) return Protocol.error("Unauthorized");
        JsonObject body = bodyOf(req);
        long patientId = userId;
        if (body != null && body.has("patientId") && !body.get("patientId").isJsonNull()) {
            try { patientId = body.get("patientId").getAsLong(); }
            catch (Exception ex) { patientId = Long.parseLong(body.get("patientId").getAsString().trim()); }
        }
        try {
            var list = new ReferralDao().listByPatient(patientId);
            var items = new JsonArray();
            for (ReferralDao.Referral r : list) {
                var o = new JsonObject();
                o.addProperty("id", r.id);
                o.addProperty("patientId", r.patientId);
                o.addProperty("facility", r.facility);
                o.addProperty("date", r.date);
                o.addProperty("time", r.time);
                if (r.notes != null) o.addProperty("notes", r.notes);
                items.add(o);
            }
            var ok = Protocol.ok();
            ok.getAsJsonObject("data").add("items", items);
            return ok;
        } catch (Exception e) {
            e.printStackTrace();
            return Protocol.error("Failed to load referrals");
        }
    }

    // POST: referral.book { patientId, facility, date, time, notes? }
    public static JsonObject book(JsonObject req, Long userId) {
        if (userId == null) return Protocol.error("Unauthorized");
        JsonObject b = bodyOf(req);
        try {
            long patientId = b.get("patientId").getAsLong();
            String facility = b.get("facility").getAsString();
            String date = b.get("date").getAsString();
            String time = b.get("time").getAsString();
            String notes = (b.has("notes") && !b.get("notes").isJsonNull()) ? b.get("notes").getAsString() : null;

            long id = new ReferralDao().insert(patientId, facility, date, time, notes);
            var data = new JsonObject(); data.addProperty("id", id);
            return Protocol.ok("ok", data);
        } catch (Exception e) {
            e.printStackTrace();
            return Protocol.error("Booking failed");
        }
    }

    // POST: referral.delete { id }
    public static JsonObject delete(JsonObject req, Long userId) {
        if (userId == null) return Protocol.error("Unauthorized");
        JsonObject b = bodyOf(req);
        try {
            long id = b.get("id").getAsLong();
            new ReferralDao().delete(id);
            return Protocol.ok();
        } catch (Exception e) {
            e.printStackTrace();
            return Protocol.error("Delete failed");
        }
    }
}
