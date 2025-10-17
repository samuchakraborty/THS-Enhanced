// src/main/java/ths/server/handler/UsersHandler.java
package ths.server.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import ths.server.Protocol;
import ths.server.dao.UserDao;
import ths.server.model.User;

import java.util.List;
import ths.server.dao.VitalDao;

public class UsersHandler {

    /**
     * Action: users.listPatients
     * Body (optional): { "mine": true }  -> only patients related to this specialist
     * Response: { status:"ok", data:{ items:[{id, fullName}] } }
     */
    public static JsonObject listPatients(JsonObject req, Long userId) {
        if (userId == null) return Protocol.error("Unauthorized");

        JsonObject body = (req != null && req.has("body") && req.get("body").isJsonObject())
                ? req.getAsJsonObject("body") : req;

        boolean mine = false;
        if (body != null && body.has("mine") && !body.get("mine").isJsonNull()) {
            try { mine = body.get("mine").getAsBoolean(); } catch (Exception ignored) {}
        }

        try {
            var dao = new UserDao();
            List<User> list = mine ? dao.listMyPatients(userId) : dao.listAllPatients();

            JsonArray items = new JsonArray();
            for (User u : list) {
                JsonObject o = new JsonObject();
                o.addProperty("id", u.getId());
                // client expects "fullName" or similar; provide it from `name`
                o.addProperty("fullName", u.getName() != null && !u.getName().isBlank()
                        ? u.getName() : ("Patient #" + u.getId()));
                items.add(o);
            }

            JsonObject data = new JsonObject();
            data.add("items", items);
            return Protocol.ok("ok", data);
        } catch (SQLException e) {
            e.printStackTrace();
            return Protocol.error("Failed to load patients");
        }
    }
   
    public static JsonObject listMine(JsonObject req, Long userId) {
        if (userId == null) return Protocol.error("Unauthorized");

        // unwrap {"body":{...}} or accept raw
        JsonObject body = (req != null && req.has("body") && req.get("body").isJsonObject())
                ? req.getAsJsonObject("body") : req;

        long patientId = userId; // default to caller
        if (body != null && body.has("patientId") && !body.get("patientId").isJsonNull()) {
            try {
                patientId = body.get("patientId").getAsLong();
            } catch (Exception ex) {
                patientId = Long.parseLong(body.get("patientId").getAsString().trim());
            }
        }

        try {
            var list = new VitalDao().listByPatient(patientId);

            var items = new JsonArray();
            var fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            list.forEach(v -> {
                var o = new JsonObject();
                o.addProperty("id", v.id);
                o.addProperty("patientId", v.patientId);
                if (v.pulse != null)       o.addProperty("pulse", v.pulse);
                if (v.respiration != null) o.addProperty("respiration", v.respiration);
                if (v.temperature != null) o.addProperty("temperature", v.temperature);
                if (v.systolic != null)    o.addProperty("systolic", v.systolic);
                if (v.diastolic != null)   o.addProperty("diastolic", v.diastolic);
//                if (v.createdAt != null) {
//                    var iso = v.createdAt.toInstant().atOffset(ZoneOffset.UTC).format(fmt);
//                    o.addProperty("createdAt", iso); // your UI looks for createdAt or measuredAt
//                }
                items.add(o);
            });

            var ok = Protocol.ok();
            ok.getAsJsonObject("data").add("items", items);
            return ok;
        } catch (Exception e) {
            e.printStackTrace();
            return Protocol.error("Failed to load vitals");
        }
    }
}
