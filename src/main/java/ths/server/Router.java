package ths.server;

import com.google.gson.JsonObject;
import ths.server.handler.*;   // AuthHandler, BookingHandler, VitalHandler, PrescriptionHandler, ...

public class Router {

    public static JsonObject route(String action, JsonObject req, Long userId) throws Exception {
        return switch (action) {

            // ── AUTH (no userId needed) ──────────────────────────────────────────────
            case "auth.register" ->
                new ths.server.handler.AuthHandler().register(req);
            case "auth.login" ->
                new ths.server.handler.AuthHandler().login(req);

            // ── YOUR EXISTING ROUTES (examples below) ───────────────────────────────
            case "booking.create" ->
                BookingHandler.create(req, userId);
            case "booking.listMine" ->
                BookingHandler.listMine(req, userId);
            case "specialist.list" ->
                SpecialistHandler.list(req, userId);

            case "vital.record" ->
                VitalsHandler.record(req, userId);
            case "vitals.record" ->
                VitalsHandler.record(req, userId);
            case "vitals.listMine" ->
                VitalsHandler.listMine(req, userId);
//            case "vital.importCsv" ->
//                VitalHandler(req, userId);
//            case "vital.exportCsv" ->
//                VitalHandler.exportCsv(req, userId);
            case "rx.listMine" ->
                PrescriptionHandler.listMine(req, userId);
            case "rx.create" ->
                PrescriptionHandler.create(req, userId);
            case "rx.updateQty" ->
                PrescriptionHandler.updateQty(req, userId);
            case "rx.refillAdd" ->
                PrescriptionHandler.refillAdd(req, userId);
                case "users.listPatients" ->
              UsersHandler.listPatients(req, userId);

                
            // … keep the rest of your existing cases …
            // ── unknown action ──────────────────────────────────────────────────────
            default -> {
                JsonObject err = new JsonObject();
                err.addProperty("status", "error");
                err.addProperty("message", "Unknown action: " + action);
                yield err;
            }
        };
    }
}
