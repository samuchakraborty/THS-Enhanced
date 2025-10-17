// ths/server/handler/PrescriptionHandler.java
package ths.server.handler;

import com.google.gson.JsonObject;
import ths.server.Protocol;
import ths.server.dao.PrescriptionDao;
import ths.server.model.Prescription;

public class PrescriptionHandler {

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

    private static Integer optInt(JsonObject o, String k) {
        return (o != null && o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsInt() : null;
    }

    private static String optStr(JsonObject o, String k) {
        return (o != null && o.has(k) && !o.get(k).isJsonNull())
                ? o.get(k).getAsString().trim()
                : null;
    }

    /**
     * POST body: { patientId?, drugName, dosage, instructions?, refillsTotal? }
     */
    public static JsonObject create(JsonObject req, Long userId) {
        if (userId == null) {
            return Protocol.error("Unauthorized");
        }

        // unwrap {"body":{...}} or accept raw
        JsonObject body = (req != null && req.has("body") && req.get("body").isJsonObject())
                ? req.getAsJsonObject("body") : req;

        try {
            // If caller didn't pass a patientId, assume it's the current user
            long patientId;
            if (body != null && body.has("patientId") && !body.get("patientId").isJsonNull()) {
                try {
                    patientId = body.get("patientId").getAsLong();
                } catch (Exception ex) {
                    patientId = Long.parseLong(needStr(body, "patientId"));
                }
            } else {
                patientId = userId;
            }

            String drugName = needStr(body, "drugName");
            String dosage = needStr(body, "dosage");
            String instr = optStr(body, "instructions");
            Integer refTot = optInt(body, "refillsTotal"); // nullable -> DAO uses 0 if null

            Prescription p = new Prescription();
            p.patientId = patientId;
            p.specialistId = userId;         // the logged-in specialist creating it
            p.drugName = drugName;
            p.dosage = dosage;
            p.instructions = instr;
            p.refillsTotal = (refTot == null ? 0 : refTot);
            p.refillsUsed = 0;              // starts at 0
            p.status = "ACTIVE";

            long id = new PrescriptionDao().insert(p);

            JsonObject data = new JsonObject();
            data.addProperty("id", id);
            return Protocol.ok("ok", data);

        } catch (IllegalArgumentException iae) {
            return Protocol.error("Validation error: " + iae.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Protocol.error("Create failed");
        }

    }

    /**
     * GET action: rx.listMine — patient’s prescriptions
     */
// ths/server/handler/PrescriptionHandler.java
// ths/server/handler/PrescriptionHandler.java
 public static JsonObject listMine(JsonObject req, long userId) {
    try {
        // unwrap {"body":{...}} or accept raw
        JsonObject body = (req != null && req.has("body") && req.get("body").isJsonObject())
                ? req.getAsJsonObject("body") : req;

        long patientId = userId; // default: current user
        if (body != null && body.has("patientId") && !body.get("patientId").isJsonNull()) {
            try {
                patientId = body.get("patientId").getAsLong();
            } catch (Exception e) {
                patientId = Long.parseLong(body.get("patientId").getAsString().trim());
            }
        }

        var list = new PrescriptionDao().listByPatient(patientId);
        var items = new com.google.gson.JsonArray();
        list.forEach(p -> {
            var o = new com.google.gson.JsonObject();
            o.addProperty("id", p.id);
            o.addProperty("patientId", p.patientId);
            o.addProperty("specialistId", p.specialistId);
            o.addProperty("drugName", p.drugName);
            o.addProperty("dosage", p.dosage);
            if (p.instructions != null) o.addProperty("instructions", p.instructions);
            o.addProperty("refillsTotal", p.refillsTotal);
            o.addProperty("refillsUsed", p.refillsUsed);
            o.addProperty("status", p.status);
            items.add(o);
        });

        var ok = Protocol.ok();
        ok.getAsJsonObject("data").add("items", items);
        return ok;
    } catch (Exception e) {
        e.printStackTrace();
        return Protocol.error("Failed to load prescriptions");
    }
}

    // ths/server/handler/PrescriptionHandler.java
public static JsonObject updateQty(JsonObject req, Long userId) {
    if (userId == null) return Protocol.error("Unauthorized");
    JsonObject body = req.has("body") && req.get("body").isJsonObject() ? req.getAsJsonObject("body") : req;
    try {
        long id = body.get("id").getAsLong();
        int total = body.get("refillsTotal").getAsInt();
        new PrescriptionDao().setRefillsTotal(id, total);
        return Protocol.ok();
    } catch (Exception e) {
        e.printStackTrace();
        return Protocol.error("Update failed");
    }
}

public static JsonObject refillAdd(JsonObject req, Long userId) {
    if (userId == null) return Protocol.error("Unauthorized");
    JsonObject body = req.has("body") && req.get("body").isJsonObject() ? req.getAsJsonObject("body") : req;
    try {
        long id = body.get("id").getAsLong();
        int amount = body.get("amount").getAsInt();
        new PrescriptionDao().addRefills(id, amount);
        return Protocol.ok();
    } catch (Exception e) {
        e.printStackTrace();
        return Protocol.error("Refill failed");
    }
}

}
