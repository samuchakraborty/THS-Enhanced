// ths.client.ui.BookingController.java
package ths.client.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ths.client.net.Api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import ths.client.App;

public class BookingController {

    // ---- UI fields ----
    @FXML
    private ComboBox<SpecOption> specialistBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private Spinner<Integer> hourSpinner, minuteSpinner;
    @FXML
    private TextField notes;

    @FXML
    private TableView<Row> table;
    @FXML
    private TableColumn<Row, Long> colId;
    @FXML
    private TableColumn<Row, String> colSpec, colStart, colStatus, colNotes;
    @FXML
    private Label msg;

    // ---- table row model ----
    public static class Row {

        public long id;
        public String specialist;
        public String startAt;
        public String status;
        public String notes;

        public Row(long id, String specialist, String startAt, String status, String notes) {
            this.id = id;
            this.specialist = specialist;
            this.startAt = startAt;
            this.status = status;
            this.notes = notes;
        }

        public long getId() {
            return id;
        }

        public String getSpecialist() {
            return specialist;
        }

        public String getStartAt() {
            return startAt;
        }

        public String getStatus() {
            return status;
        }

        public String getNotes() {
            return notes;
        }
    }

    // ---- specialist option model ----
    public static class SpecOption {

        public final long id;
        public final String name;
        public final String email;

        public SpecOption(long id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        @Override
        public String toString() {
            return name != null && !name.isBlank() ? name : email;
        }
    }

    private static final DateTimeFormatter ISO_MIN = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @FXML
    private void goDashboard() {
        ths.client.App.load("dashboard.fxml", "THS — Dashboard");
    }
     @FXML
    public void goVitals(){ App.load("vitals.fxml", "THS — Vitals"); }
     @FXML
    public void goRx(){ App.load("prescriptions.fxml", "THS — Prescriptions");
    }

    @FXML
    public void initialize() {
        // table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSpec.setCellValueFactory(new PropertyValueFactory<>("specialist"));
        colStart.setCellValueFactory(new PropertyValueFactory<>("startAt"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));

        // time spinners
        hourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9));
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 5));
        hourSpinner.setEditable(true);
        minuteSpinner.setEditable(true);

        datePicker.setValue(LocalDate.now());

        // load specialists then bookings
        loadSpecialists();
        refresh();
    }

    private void loadSpecialists() {
        try {
            JsonObject res = Api.call("specialist.list", null);
            if (res == null || !"ok".equalsIgnoreCase(res.get("status").getAsString())) {
                msg.setText(getMsg(res, "Failed to load specialists"));
                specialistBox.setItems(FXCollections.observableArrayList());
                return;
            }
            JsonArray items = res.getAsJsonObject("data").getAsJsonArray("items");
            var options = FXCollections.<SpecOption>observableArrayList();
            items.forEach(j -> {
                JsonObject o = j.getAsJsonObject();
                options.add(new SpecOption(
                        o.get("id").getAsLong(),
                        o.has("name") && !o.get("name").isJsonNull() ? o.get("name").getAsString() : null,
                        o.has("email") && !o.get("email").isJsonNull() ? o.get("email").getAsString() : null
                ));
            });
            specialistBox.setItems(options);
            if (!options.isEmpty()) {
                specialistBox.getSelectionModel().select(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            msg.setText("Failed to load specialists: " + e.getMessage());
        }
    }

    @FXML
    private void create() {
        try {
            msg.setText("");
            SpecOption sel = specialistBox.getSelectionModel().getSelectedItem();
            if (sel == null) {
                msg.setText("Select a specialist");
                return;
            }

            LocalDate d = datePicker.getValue();
            int h = hourSpinner.getValue() == null ? 0 : hourSpinner.getValue();
            int m = minuteSpinner.getValue() == null ? 0 : minuteSpinner.getValue();
            if (d == null) {
                msg.setText("Pick a date");
                return;
            }

            String startIso = LocalDateTime.of(d.getYear(), d.getMonthValue(), d.getDayOfMonth(), h, m).format(ISO_MIN);

            JsonObject body = new JsonObject();
            body.addProperty("specialistId", sel.id);
            body.addProperty("startAt", startIso);
            if (notes.getText() != null && !notes.getText().isBlank()) {
                body.addProperty("notes", notes.getText().trim());
            }

            JsonObject res = Api.call("booking.create", body);
            if (res == null || !"ok".equalsIgnoreCase(res.get("status").getAsString())) {
                msg.setText(getMsg(res, "Create failed"));
                return;
            }
            long id = res.getAsJsonObject("data").get("id").getAsLong();
            msg.setText("Created booking #" + id);
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
            msg.setText("Create error: " + e.getMessage());
        }
    }

    private void refresh() {
        try {
            msg.setText("");
            table.getItems().clear();

            JsonObject res = Api.call("booking.listMine", null);
            if (res == null || !"ok".equalsIgnoreCase(res.get("status").getAsString())) {
                msg.setText(getMsg(res, "Couldn’t load bookings"));
                return;
            }
            JsonArray items = res.getAsJsonObject("data").getAsJsonArray("items");
            items.forEach(j -> {
                JsonObject o = j.getAsJsonObject();
                table.getItems().add(new Row(
                        o.get("id").getAsLong(),
                        o.has("specialistName") && !o.get("specialistName").isJsonNull()
                        ? o.get("specialistName").getAsString()
                        : String.valueOf(o.get("specialistId").getAsLong()),
                        o.get("startAt").getAsString(),
                        o.get("status").getAsString(),
                        o.has("notes") && !o.get("notes").isJsonNull() ? o.get("notes").getAsString() : ""
                ));
            });
            if (table.getItems().isEmpty()) {
                msg.setText("No bookings yet");
            }
        } catch (Exception e) {
            e.printStackTrace();
            msg.setText("Load failed: " + e.getMessage());
        }
    }

    private static String getMsg(JsonObject res, String fallback) {
        if (res == null) {
            return fallback;
        }
        if (res.has("message") && !res.get("message").isJsonNull()) {
            return res.get("message").getAsString();
        }
        if (res.has("error") && !res.get("error").isJsonNull()) {
            return res.get("error").getAsString();
        }
        return fallback;
    }
}
