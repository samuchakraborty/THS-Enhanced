package ths.client.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import ths.client.App;
import ths.client.net.Api;

public class VitalsController {

    // Toolbar navigation
    @FXML private void goDashboard()    { App.load("dashboard.fxml",     "THS — Dashboard"); }
    @FXML private void goVitals()       { App.load("vitals.fxml",        "THS — Vitals"); }
    @FXML private void goRx()           { App.load("prescriptions.fxml", "THS — Prescriptions"); }

    // Patient dropdown (replaces patientIdField)
    @FXML private ComboBox<PatientOption> patientCombo;

    // Top form fields
    @FXML private TextField pulseField, respField, tempField, systolicField, diastolicField;

    // Table
    @FXML private TableView<Row> vitalsTable;
    @FXML private TableColumn<Row,String> patientCol, timeCol, pulseCol, respCol, tempCol, bpCol;

    private final ObservableList<Row> rows = FXCollections.observableArrayList();

    // --- Models ---
    public static class PatientOption {
        final long id;
        final String name;
        PatientOption(long id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }

    // Table row model matching the column names
    public static class Row {
        private final String patient, time, pulse, resp, temp, bp;
        public Row(String patient, String time, String pulse, String resp, String temp, String bp) {
            this.patient = patient; this.time = time; this.pulse = pulse; this.resp = resp; this.temp = temp; this.bp = bp;
        }
        public String getPatient() { return patient; }
        public String getTime()    { return time; }
        public String getPulse()   { return pulse; }
        public String getResp()    { return resp; }
        public String getTemp()    { return temp; }
        public String getBp()      { return bp; }
    }

    @FXML
    public void initialize() {
        // Wire table columns to Row getters
        patientCol.setCellValueFactory(new PropertyValueFactory<>("patient"));
        timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        pulseCol.setCellValueFactory(new PropertyValueFactory<>("pulse"));
        respCol.setCellValueFactory(new PropertyValueFactory<>("resp"));
        tempCol.setCellValueFactory(new PropertyValueFactory<>("temp"));
        bpCol.setCellValueFactory(new PropertyValueFactory<>("bp"));
        vitalsTable.setItems(rows);

        // Combo rendering
        patientCombo.setConverter(new StringConverter<PatientOption>() {
            @Override public String toString(PatientOption p) { return p == null ? "" : p.name; }
            @Override public PatientOption fromString(String s) { return null; }
        });

        loadPatients(); // populate patients then vitals for the first one
    }

    // === Patients ===
    private void loadPatients() {
        try {
            // Ask server for ALL patients; works regardless of caller role
            var body = new JsonObject();
            body.addProperty("mine", false);

            JsonObject res = Api.call("users.listPatients", body);
            if (res == null || !"ok".equalsIgnoreCase(safeString(res, "status"))) {
                alert(msg(res, "Could not load patients"));
                return;
            }
            JsonArray items = res.getAsJsonObject("data").getAsJsonArray("items");
            var options = FXCollections.<PatientOption>observableArrayList();
            for (JsonElement el : items) {
                var o = el.getAsJsonObject();
                long id = safeLong(o, "id", -1L);
                String name = safeString(o, "fullName");
                if (name.isBlank()) name = "Patient #" + id;
                if (id > 0) options.add(new PatientOption(id, name));
            }
            patientCombo.setItems(options);
            if (!options.isEmpty()) {
                patientCombo.getSelectionModel().selectFirst();
                loadVitals();
            }
            patientCombo.getSelectionModel().selectedItemProperty().addListener((obs, a, b) -> loadVitals());
        } catch (Exception e) {
            alert("Load patients failed: " + e.getMessage());
        }
    }

    // === Vitals list ===
    @FXML
    private void loadVitals() {
        try {
            rows.clear();
            PatientOption sel = patientCombo.getValue();
            if (sel == null) return;

            var body = new JsonObject();
            body.addProperty("patientId", sel.id);

            // If your route name differs, adjust here
            JsonObject res = Api.call("vitals.listMine", body);
            if (res == null || !"ok".equalsIgnoreCase(safeString(res, "status"))) {
                alert(msg(res, "Failed to load vitals"));
                return;
            }

            JsonArray items = res.getAsJsonObject("data").getAsJsonArray("items");
            for (JsonElement e : items) {
                var o = e.getAsJsonObject();
                String patient = sel.name;
                // Try a couple common timestamp keys
                String when  = safeString(o, "measuredAt");
                if (when.isBlank()) when = safeString(o, "createdAt");
                // Map common vital keys; change names if your backend differs
                String pulse = safeString(o, "pulse");
                String resp  = safeString(o, "respiration");
                String temp  = safeString(o, "temperature");
                String sys   = safeString(o, "systolic");
                String dia   = safeString(o, "diastolic");
                String bp    = (!sys.isBlank() && !dia.isBlank()) ? (sys + "/" + dia) : "";
                rows.add(new Row(patient, when, pulse, resp, temp, bp));
            }
        } catch (Exception e) {
            alert("Load error: " + e.getMessage());
        }
    }

    // === Record new vital ===
    @FXML
    private void record() {
        try {
            PatientOption sel = patientCombo.getValue();
            if (sel == null) { alert("Select a patient first."); return; }

            var body = new JsonObject();
            body.addProperty("patientId", sel.id);
            if (has(pulseField))    body.addProperty("pulse",       Integer.parseInt(pulseField.getText().trim()));
            if (has(respField))     body.addProperty("respiration", Integer.parseInt(respField.getText().trim()));
            if (has(tempField))     body.addProperty("temperature", Double.parseDouble(tempField.getText().trim()));
            if (has(systolicField)) body.addProperty("systolic",    Integer.parseInt(systolicField.getText().trim()));
            if (has(diastolicField))body.addProperty("diastolic",   Integer.parseInt(diastolicField.getText().trim()));

            JsonObject res = Api.call("vitals.record", body); // adjust route name if needed
            if (res == null || !"ok".equalsIgnoreCase(safeString(res, "status"))) {
                alert(msg(res, "Record failed")); return;
            }
            loadVitals(); // refresh table
        } catch (NumberFormatException nfe) {
            alert("Please enter valid numbers for pulse/resp/temp/BP.");
        } catch (Exception e) {
            alert("Record failed: " + e.getMessage());
        }
    }

    @FXML
    private void exportCsv() {
        try {
            StringBuilder sb = new StringBuilder("Patient,Time,Pulse,Resp,Temp,BP\n");
            vitalsTable.getItems().forEach(r ->
                sb.append(csv(r.getPatient())).append(',')
                  .append(csv(r.getTime())).append(',')
                  .append(csv(r.getPulse())).append(',')
                  .append(csv(r.getResp())).append(',')
                  .append(csv(r.getTemp())).append(',')
                  .append(csv(r.getBp())).append('\n')
            );
            alert("CSV prepared in memory (hook up a FileChooser to save).");
        } catch (Exception e) {
            alert("Export failed: " + e.getMessage());
        }
    }

    // --- helpers ---
    private static boolean has(TextField f){ return f != null && f.getText() != null && !f.getText().trim().isBlank(); }
    private static String safeString(JsonObject o, String k) {
        return (o != null && o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsString() : "";
    }
    private static long safeLong(JsonObject o, String k, long def) {
        try { if (o != null && o.has(k) && !o.get(k).isJsonNull()) return o.get(k).getAsLong(); }
        catch (Exception ignored) {}
        return def;
    }
    private static String msg(JsonObject r, String fb) {
        return (r != null && r.has("message") && !r.get("message").isJsonNull())
                ? r.get("message").getAsString() : fb;
    }
    private static String csv(String s){ return s == null ? "" : s.replace("\"","\"\""); }
    private static void alert(String msg){
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }
}
