package ths.client.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ths.client.App;
import ths.client.net.Api;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

// ...inside PrescriptionsController
public class PrescriptionsController {

    @FXML
    private TextField patientIdField, drugField, qtyField, thresholdField, refillField, filterField;
    @FXML
    private TableView<Row> rxTable;
    @FXML
    private TableColumn<Row, String> patientCol, drugCol, qtyCol, thrCol;

    private final ObservableList<Row> rows = FXCollections.observableArrayList();

    // Keep DB id in the row so updates know what to modify
    public static class Row {

        private final long id;
        private final String patient;
        private final String drug;
        private final String qty;
        private final String thr;

        public Row(long id, String patient, String drug, String qty, String thr) {
            this.id = id;
            this.patient = patient;
            this.drug = drug;
            this.qty = qty;
            this.thr = thr;
        }

        public long getId() {
            return id;
        }

        public String getPatient() {
            return patient;
        }

        public String getDrug() {
            return drug;
        }

        public String getQty() {
            return qty;
        }

        public String getThr() {
            return thr;
        }
    }
// imports to add

    @FXML
    private ComboBox<PatientOption> patientCombo; // NEW: dropdown of patients

// Tiny holder for the combo box
    public static class PatientOption {

        final long id;
        final String name;

        PatientOption(long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @FXML
    public void initialize() {
        patientCol.setCellValueFactory(new PropertyValueFactory<>("patient"));
        drugCol.setCellValueFactory(new PropertyValueFactory<>("drug"));
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("qty"));
        thrCol.setCellValueFactory(new PropertyValueFactory<>("thr"));

        rxTable.setItems(rows);

        // Pretty labels in the combo
        patientCombo.setConverter(new StringConverter<PatientOption>() {
            @Override
            public String toString(PatientOption p) {
                return p == null ? "" : p.name;
            }

            @Override
            public PatientOption fromString(String s) {
                return null;
            } // not used
        });

        // Load patients first, then prescriptions for the first patient (if any)
        loadPatients();
    }

  
    @FXML
    private void load() {
        try {
            rows.clear();

            PatientOption sel = patientCombo.getValue();
            if (sel == null) {
                return; // nothing selected yet
            }
            JsonObject body = new JsonObject();
            body.addProperty("patientId", sel.id);

            JsonObject res = Api.call("rx.listMine", body);
            if (res == null || !"ok".equalsIgnoreCase(safeString(res, "status"))) {
                alert(msg(res, "Load failed"));
                return;
            }

            var data = res.getAsJsonObject("data");
            if (data == null || !data.has("items") || !data.get("items").isJsonArray()) {
                return;
            }

            var items = data.getAsJsonArray("items");
            for (var el : items) {
                if (!el.isJsonObject()) {
                    continue;
                }
                var o = el.getAsJsonObject();
                long id = safeLong(o, "id", -1L);
                // show patient name from the combo (nicer than numeric id)
                String patientName = sel.name;
                String drug = safeString(o, "drugName");
                String qty = safeString(o, "refillsTotal");
                   
                String thr = safeString(o, "refillsTotal");
//                String thr = o.has("refillThreshold") ? safeString(o, "refillThreshold")
//                        : (o.has("threshold") ? safeString(o, "threshold") : "");
                rows.add(new Row(id, patientName, drug, qty, thr));
            }
        } catch (Exception e) {
            alert("Load failed: " + e.getMessage());
        }
    }

    @FXML
    private void add() {
        try {
            PatientOption sel = patientCombo.getValue();
            if (sel == null) {
                throw new IllegalArgumentException("Select a patient first");
            }

            var body = new JsonObject();
            body.addProperty("patientId", sel.id);
            body.addProperty("drugName", required(drugField, "Drug"));

            // dosage: map from your Quantity field (or add a dedicated dosage field)
            String dosage = required(qtyField, "Quantity (used as Dosage)");
            body.addProperty("dosage", dosage);

            // optional totals/thresholds
            String thrText = text(thresholdField);
            if (!thrText.isBlank()) {
                Integer refillsTotal = tryParseInt(thrText);
                if (refillsTotal == null) {
                    throw new IllegalArgumentException("Low-Stock Threshold must be a number");
                }
                body.addProperty("refillsTotal", refillsTotal);
            }
            String instr = text(filterField);
            if (!instr.isBlank()) {
                body.addProperty("instructions", instr);
            }

            var res = Api.call("rx.create", body);
            if (res == null || !"ok".equalsIgnoreCase(safeString(res, "status"))) {
                alert(msg(res, "Create failed"));
                return;
            }
            alert("Prescription created: #" + safeLong(res.getAsJsonObject("data"), "id", -1L));
            load();
        } catch (IllegalArgumentException iae) {
            alert("Validation error: " + iae.getMessage());
        } catch (Exception e) {
            alert("Create error: " + e.getMessage());
        }
    }

    @FXML
    private void updateQty() {
        Row sel = rxTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            alert("Select a prescription in the table first.");
            return;
        }
        try {
            Integer total = tryParseInt(required(qtyField, "Quantity"));
            if (total == null) {
                throw new IllegalArgumentException("Quantity must be a number");
            }

            var body = new JsonObject();
            body.addProperty("id", sel.getId());
            body.addProperty("refillsTotal", total);

            var res = Api.call("rx.updateQty", body);
            if (res == null || !"ok".equalsIgnoreCase(safeString(res, "status"))) {
                alert(msg(res, "Update failed"));
                return;
            }
            load();
        } catch (IllegalArgumentException iae) {
            alert(iae.getMessage());
        } catch (Exception e) {
            alert("Update error: " + e.getMessage());
        }
    }

    @FXML
    private void refillAdd() {
        Row sel = rxTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            alert("Select a prescription in the table first.");
            return;
        }
        try {
            String deltaText = text(refillField);
            int delta = 1;
            if (!deltaText.isBlank()) {
                Integer v = tryParseInt(deltaText);
                if (v == null) {
                    throw new IllegalArgumentException("Refill amount must be a number.");
                }
                delta = v;
            }

            var body = new JsonObject();
            body.addProperty("id", sel.getId());
            body.addProperty("amount", delta);

            var res = Api.call("rx.refillAdd", body);
            if (res == null || !"ok".equalsIgnoreCase(safeString(res, "status"))) {
                alert(msg(res, "Refill failed"));
                return;
            }
            load();
        } catch (IllegalArgumentException iae) {
            alert(iae.getMessage());
        } catch (Exception e) {
            alert("Refill error: " + e.getMessage());
        }
    }

    // ===== Navigation =====
    @FXML
    private void goDashboard() {
        App.load("dashboard.fxml", "THS — Dashboard");
    }

    @FXML
    private void goVitals() {
        App.load("vitals.fxml", "THS — Vitals");
    }

    @FXML
    private void goRx() {
        App.load("prescriptions.fxml", "THS — Prescriptions");
    }

    @FXML
    private void logout() {
        var ask = new Alert(Alert.AlertType.CONFIRMATION, "Log out now?", ButtonType.OK, ButtonType.CANCEL);
        ask.setHeaderText(null);
        ask.showAndWait();
        if (ask.getResult() != ButtonType.OK) {
            return;
        }

        try {
            Api.setToken(null);
        } catch (Exception ignored) {
        }
        App.load("login.fxml", "THS — Login");
    }

    // ===== Helpers =====
    private static boolean has(TextField f) {
        return f.getText() != null && !f.getText().isBlank();
    }

    private static String text(TextField f) {
        return f == null || f.getText() == null ? "" : f.getText().trim();
    }

    private static String required(TextField f, String label) {
        String v = text(f);
        if (v.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return v;
    }

    private static String safeString(JsonObject o, String k) {
        return (o != null && o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsString() : "";
    }

    private static long safeLong(JsonObject o, String k, long def) {
        try {
            if (o != null && o.has(k) && !o.get(k).isJsonNull()) {
                return o.get(k).getAsLong();
            }
        } catch (Exception ignored) {
        }
        return def;
    }

    private static Integer tryParseInt(String s) {
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long tryParseLong(String s) {
        try {
            return Long.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String msg(JsonObject r, String fb) {
        return (r != null && r.has("message") && !r.get("message").isJsonNull())
                ? r.get("message").getAsString() : fb;
    }

    private static void alert(String m) {
        var a = new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
    private void loadPatients() {
    try {
        var body = new JsonObject();
        body.addProperty("mine", false); // set false to list ALL patients
        JsonObject res = Api.call("users.listPatients", body);

        if (res == null || !"ok".equalsIgnoreCase(safeString(res, "status"))) {
            alert(msg(res, "Could not load patients"));
            return;
        }

        var items = res.getAsJsonObject("data").getAsJsonArray("items");
        var options = FXCollections.<PatientOption>observableArrayList();
        items.forEach(e -> {
            var o = e.getAsJsonObject();
            long id = safeLong(o, "id", -1L);
            String name = safeString(o, "fullName");
            if (name.isBlank()) name = "Patient #" + id;
            if (id > 0) options.add(new PatientOption(id, name));
        });

        patientCombo.setItems(options);
        if (!options.isEmpty()) {
            patientCombo.getSelectionModel().selectFirst();
            load(); // show prescriptions for the first patient
        }

        patientCombo.getSelectionModel().selectedItemProperty().addListener((obs, a, b) -> load());

    } catch (Exception ex) {
        alert("Load patients failed: " + ex.getMessage());
    }
}

}
