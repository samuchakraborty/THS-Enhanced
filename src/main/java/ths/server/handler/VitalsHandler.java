// ths/server/handler/VitalsHandler.java
package ths.server.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ths.server.Protocol;
import ths.server.dao.VitalDao;
import ths.server.model.VitalSign;

import java.time.LocalDateTime;

public class VitalsHandler {

    private static String needStr(JsonObject o, String k) {
        if (o == null || !o.has(k) || o.get(k).isJsonNull()) throw new IllegalArgumentException("Missing field: " + k);
        String v = o.get(k).getAsString().trim();
        if (v.isEmpty()) throw new IllegalArgumentException("Missing field: " + k);
        return v;
    }
    private static Integer optInt(JsonObject o, String k){
        return (o != null && o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsInt() : null;
    }
    private static Double optDbl(JsonObject o, String k){
        return (o != null && o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsDouble() : null;
    }

    /** POST: body { patientId? , pulse?, respiration?, temperature?, systolic?, diastolic? } */
    public static JsonObject record(JsonObject req, Long userId) {
        if (userId == null) return Protocol.error("Unauthorized");

        // unwrap {"body":{...}} or accept raw
        JsonObject body = (req != null && req.has("body") && req.get("body").isJsonObject())
                ? req.getAsJsonObject("body") : req;

        try {
            long patientId;
            if (body != null && body.has("patientId") && !body.get("patientId").isJsonNull()) {
                // allow string or number
                try { patientId = body.get("patientId").getAsLong(); }
                catch (Exception ignore) { patientId = Long.parseLong(needStr(body, "patientId")); }
            } else {
                patientId = userId; // default to current user
            }

            VitalSign v = new VitalSign();
            v.patientId  = patientId;
            v.measuredAt = LocalDateTime.now();
            v.pulse      = optInt(body, "pulse");
            v.respiration= optInt(body, "respiration");
            v.temperature= optDbl(body, "temperature");
            v.systolic   = optInt(body, "systolic");
            v.diastolic  = optInt(body, "diastolic");

            long id = new VitalDao().insert(v);

            JsonObject data = new JsonObject();
            data.addProperty("id", id);
            return Protocol.ok("ok", data);

        } catch (IllegalArgumentException e) {
            return Protocol.error("Validation error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Protocol.error("Record failed");
        }
    }

    /** GET: returns current user’s vitals (patient) */
    public static JsonObject listMine(JsonObject req, long userId) {
        try {
            var list = new VitalDao().listByPatient(userId);
            JsonArray items = new JsonArray();
            for (VitalSign v : list) {
                JsonObject o = new JsonObject();
                o.addProperty("patientId", v.patientId);
                o.addProperty("measuredAt", v.measuredAt.toString());
                if (v.pulse != null)       o.addProperty("pulse", v.pulse);
                if (v.respiration != null) o.addProperty("respiration", v.respiration);
                if (v.temperature != null) o.addProperty("temperature", v.temperature);
                if (v.systolic != null)    o.addProperty("systolic", v.systolic);
                if (v.diastolic != null)   o.addProperty("diastolic", v.diastolic);
                items.add(o);
            }
            JsonObject ok = Protocol.ok();
            ok.getAsJsonObject("data").add("items", items);
            return ok;
        } catch (Exception e) {
            e.printStackTrace();
            return Protocol.error("Failed to load vitals");
        }
    }
}
