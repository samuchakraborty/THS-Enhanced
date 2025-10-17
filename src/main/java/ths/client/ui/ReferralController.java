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
import ths.client.net.Api;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import ths.client.App;

public class ReferralController {

    // --- Top bar ---
    @FXML private ComboBox<PatientOption> patientCombo;
    @FXML private Button bookReferralBtn, deleteReferralBtn;

    // --- Form fields ---
    @FXML private TextField referralFacilityField, referralTimeField, referralNotesField;
    @FXML private DatePicker referralDatePicker;

    // --- Table ---
    @FXML private TableView<Row> referralTable;
    @FXML private TableColumn<Row, String> refFacilityCol, refDateCol, refTimeCol, refNotesCol;

    private final ObservableList<Row> rows = FXCollections.observableArrayList();

    // ===== Models =====
    public static class PatientOption {
        final long id; final String name;
        PatientOption(long id, String name) { this.id = id; this.name = name; }
    }

    public static class Row {
        private final long id;             // referral id (for delete)
        private final String facility, date, time, notes;
        public Row(long id, String facility, String date, String time, String notes) {
            this.id = id; this.facility = facility; this.date = date; this.time = time; this.notes = notes;
        }
        public long getId() { return id; }
        public String getFacility() { return facility; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getNotes() { return notes; }
    }

    @FXML
    public void initialize() {
        refFacilityCol.setCellValueFactory(new PropertyValueFactory<>("facility"));
        refDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        refTimeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        refNotesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));
        referralTable.setItems(rows);

        patientCombo.setConverter(new StringConverter<PatientOption>() {
            @Override public String toString(PatientOption p) { return p == null ? "" : p.name; }
            @Override public PatientOption fromString(String s) { return null; }
        });

        loadPatients();
    }

    // ========= Patients =========
    private void loadPatients() {
        try {
            var body = new JsonObject();
            body.addProperty("mine", false); // list ALL patients so it works for any role
            JsonObject res = Api.call("users.listPatients", body);
            if (!ok(res)) { alert(msg(res,"Could not load patients")); return; }
            var items = res.getAsJsonObject("data").getAsJsonArray("items");

            var opts = FXCollections.<PatientOption>observableArrayList();
            for (JsonElement e : items) {
                var o = e.getAsJsonObject();
                long id = getLong(o, "id", -1);
                String name = getStr(o, "fullName", "Patient #"+id);
                if (id > 0) opts.add(new PatientOption(id, name));
            }
            patientCombo.setItems(opts);
            if (!opts.isEmpty()) {
                patientCombo.getSelectionModel().selectFirst();
                refresh();
            }
            patientCombo.getSelectionModel().selectedItemProperty().addListener((obs,a,b) -> refresh());
        } catch (Exception ex) {
            alert("Load patients failed: " + ex.getMessage());
        }
    }

    // ========= Actions =========
    @FXML
    private void refresh() {
        try {
            rows.clear();
            var sel = patientCombo.getValue();
            if (sel == null) return;

            var body = new JsonObject();
            body.addProperty("patientId", sel.id);

            // Adjust to your backend route if different
            JsonObject res = Api.call("referral.listMine", body);
            if (!ok(res)) { alert(msg(res,"Load referrals failed")); return; }

            JsonArray arr = res.getAsJsonObject("data").getAsJsonArray("items");
            for (JsonElement e : arr) {
                var o = e.getAsJsonObject();
                long id = getLong(o, "id", -1);
                String facility = getStr(o, "facility", "");
                String date = getStr(o, "date", "");   // e.g., "2025-10-18"
                String time = getStr(o, "time", "");   // e.g., "11:30"
                String notes = getStr(o, "notes", "");
                rows.add(new Row(id, facility, date, time, notes));
            }
        } catch (Exception e) {
            alert("Refresh error: " + e.getMessage());
        }
    }

    @FXML
    private void bookReferral() {
        try {
            var sel = patientCombo.getValue();
            if (sel == null) { alert("Select a patient first."); return; }

            String facility = required(referralFacilityField, "Facility");
            LocalDate date = referralDatePicker.getValue();
            if (date == null) throw new IllegalArgumentException("Date is required");
            String time = required(referralTimeField, "Time (HH:MM)");
            String notes = getText(referralNotesField);

            var body = new JsonObject();
            body.addProperty("patientId", sel.id);
            body.addProperty("facility", facility);
            body.addProperty("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            body.addProperty("time", time);
            if (!notes.isBlank()) body.addProperty("notes", notes);

            // Adjust to your backend route if different
            JsonObject res = Api.call("referral.book", body);
            if (!ok(res)) { alert(msg(res, "Booking failed")); return; }

//            clearForm();
            refresh();
        } catch (IllegalArgumentException iae) {
            alert("Validation: " + iae.getMessage());
        } catch (Exception e) {
            alert("Booking error: " + e.getMessage());
        }
    }

    @FXML
    private void deleteSelected() {
        var sel = referralTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Select a referral row first."); return; }
        try {
            var body = new JsonObject();
            body.addProperty("id", sel.getId());
            // Adjust to your backend route if different
            JsonObject res = Api.call("referral.delete", body);
            if (!ok(res)) { alert(msg(res, "Delete failed")); return; }
            refresh();
        } catch (Exception e) {
            alert("Delete error: " + e.getMessage());
        }
    }

    // ========= Helpers =========
    private static boolean ok(JsonObject r) {
        return r != null && r.has("status") && "ok".equalsIgnoreCase(r.get("status").getAsString());
    }
    private static String msg(JsonObject r, String fb) {
        return (r != null && r.has("message") && !r.get("message").isJsonNull())
                ? r.get("message").getAsString() : fb;
    }
    private static String getText(TextField f) { return f!=null && f.getText()!=null ? f.getText().trim() : ""; }
    private static String required(TextField f, String label) {
        String v = getText(f);
        if (v.isBlank()) throw new IllegalArgumentException(label + " is required");
        return v;
    }
    private static String getStr(JsonObject o, String k, String d) {
        return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsString() : d;
    }
    private static long getLong(JsonObject o, String k, long d) {
        try { return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsLong() : d; }
        catch (Exception e) { return d; }
    }
    private static void alert(String m) {
        var a = new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
      @FXML private void goDashboard()    { App.load("dashboard.fxml",     "THS — Dashboard"); }
    @FXML private void goVitals()       { App.load("vitals.fxml",        "THS — Vitals"); }
    @FXML private void goRx()           { App.load("prescriptions.fxml", "THS — Prescriptions"); }


}
