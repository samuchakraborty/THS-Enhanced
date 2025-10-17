package ths.client.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ChoiceBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import ths.client.App;
import ths.client.net.Api;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.scene.control.cell.TextFieldTableCell;

public class AdminDashboardController {

    // nav
    @FXML private void goDashboard(){ App.load("dashboard.fxml","THS — Dashboard"); }
    @FXML private void goVitals(){ App.load("vitals.fxml","THS — Vitals"); }
    @FXML private void goRx(){ App.load("prescriptions.fxml","THS — Prescriptions"); }
    @FXML private void reloadAll(){ loadRoleFilter(); loadUsers(); loadSpecialists(); loadBookings(); }

    // users table
    @FXML private TableView<UserRow> usersTable;
    @FXML private TableColumn<UserRow, String> uIdCol, uEmailCol, uRoleCol, uNameCol;
    @FXML private ComboBox<String> roleFilter;

    // bookings table
    @FXML private TableView<BookingRow> bookingsTable;
    @FXML private TableColumn<BookingRow, String> bIdCol, bPatientCol, bSpecCol, bDateCol, bStatusCol, bNotesCol;

    // cache of specialists for editor
    private ObservableList<SpecialistOption> specialistOptions = FXCollections.observableArrayList();

    // models
    public static class UserRow {
        private final long id;
        private final String email;
        private final String role;
        private String name;
        public UserRow(long id, String email, String role, String name) {
            this.id=id; this.email=email; this.role=role; this.name=name;
        }
        public String getId(){ return String.valueOf(id); }
        public long id(){ return id; }
        public String getEmail(){ return email; }
        public String getRole(){ return role; }
        public String getName(){ return name; }
        public void setName(String n){ this.name=n; }
    }

    public static class BookingRow {
        private final long id, patientId;
        private long specialistId;
        private String patient, specialist, status, notes;
        private LocalDateTime startAt;
        public BookingRow(long id, long patientId, long specialistId, String patient, String specialist,
                          LocalDateTime startAt, String status, String notes){
            this.id=id; this.patientId=patientId; this.specialistId=specialistId;
            this.patient=patient; this.specialist=specialist; this.startAt=startAt; this.status=status; this.notes=notes;
        }
        public String getId(){ return String.valueOf(id); }
        public long id(){ return id; }
        public String getPatient(){ return patient; }
        public String getSpecialist(){ return specialist; }
        public long specialistId(){ return specialistId; }
        public void setSpecialist(String s){ this.specialist=s; }
        public void setSpecialistId(long id){ this.specialistId=id; }
        public String getDate(){ return startAt == null ? "" : startAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")); }
        public LocalDateTime startAt(){ return startAt; }
        public void setStartAt(LocalDateTime dt){ this.startAt = dt; }
        public String getStatus(){ return status; }
        public void setStatus(String s){ this.status=s; }
        public String getNotes(){ return notes; }
        public void setNotes(String n){ this.notes=n; }
    }

    public static class SpecialistOption {
        final long id; final String name;
        public SpecialistOption(long id, String name){ this.id=id; this.name=name; }
        @Override public String toString(){ return name; }
    }

    @FXML
    public void initialize(){
        // users table
        uIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        uEmailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        uRoleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        uNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        usersTable.setEditable(true);
        // editable name: on commit call server
        uNameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        uNameCol.setOnEditCommit(evt -> {
            UserRow row = evt.getRowValue();
            String newName = evt.getNewValue() == null ? "" : evt.getNewValue().trim();
            if (newName.isBlank()) {
                usersTable.refresh();
                return;
            }
            try {
                JsonObject body = new JsonObject();
                body.addProperty("id", row.id());
                body.addProperty("name", newName);
                JsonObject res = Api.call("admin.user.updateName", body);
                if (ok(res)) {
                    row.setName(newName);
                } else {
                    alert(msg(res,"Update failed"));
                }
                usersTable.refresh();
            } catch (Exception e){ alert("Update name error: "+e.getMessage()); }
        });

        // role filter choices
        roleFilter.setItems(FXCollections.observableArrayList("", "PATIENT", "SPECIALIST", "ADMIN"));
        roleFilter.setConverter(new StringConverter<>() {
            @Override public String toString(String r){ return (r==null || r.isBlank()) ? "All roles" : r; }
            @Override public String fromString(String s){ return s; }
        });
        roleFilter.getSelectionModel().select(0);

        // bookings table
        bIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        bPatientCol.setCellValueFactory(new PropertyValueFactory<>("patient"));
        bSpecCol.setCellValueFactory(new PropertyValueFactory<>("specialist"));
        bDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        bStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        bNotesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));

        bookingsTable.setEditable(true);

        // Editable Status (enum)
        var statuses = FXCollections.observableArrayList("REQUESTED","CONFIRMED","DONE","CANCELLED");
        bStatusCol.setCellFactory(ChoiceBoxTableCell.forTableColumn(statuses));
        bStatusCol.setOnEditCommit(evt -> {
            BookingRow r = evt.getRowValue();
            String newStatus = evt.getNewValue();
            if (newStatus == null || newStatus.isBlank() || newStatus.equals(r.getStatus())) {
                bookingsTable.refresh(); return;
            }
            updateBooking(r.id(), r.specialistId(), r.startAt(), newStatus, r.getNotes(), () -> {
                r.setStatus(newStatus); bookingsTable.refresh();
            });
        });

        // Editable Specialist (as free text that validates to existing specialist option)
        bSpecCol.setCellFactory(TextFieldTableCell.forTableColumn());
        bSpecCol.setOnEditCommit(evt -> {
            BookingRow r = evt.getRowValue();
            String name = evt.getNewValue() == null ? "" : evt.getNewValue().trim();
            SpecialistOption match = specialistOptions.stream()
                    .filter(s -> s.name.equalsIgnoreCase(name)).findFirst().orElse(null);
            if (match == null) { alert("Unknown specialist: "+name); bookingsTable.refresh(); return; }
            updateBooking(r.id(), match.id, r.startAt(), r.getStatus(), r.getNotes(), () -> {
                r.setSpecialist(match.name); r.setSpecialistId(match.id); bookingsTable.refresh();
            });
        });

        // Editable Date/Time (yyyy-MM-dd HH:mm)
        bDateCol.setCellFactory(TextFieldTableCell.forTableColumn());
        bDateCol.setOnEditCommit(evt -> {
            BookingRow r = evt.getRowValue();
            String text = evt.getNewValue() == null ? "" : evt.getNewValue().trim();
            try {
                LocalDateTime dt = LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                updateBooking(r.id(), r.specialistId(), dt, r.getStatus(), r.getNotes(), () -> {
                    r.setStartAt(dt); bookingsTable.refresh();
                });
            } catch (Exception ex) {
                alert("Invalid date/time format. Use yyyy-MM-dd HH:mm");
                bookingsTable.refresh();
            }
        });

        // Editable Notes
        bNotesCol.setCellFactory(TextFieldTableCell.forTableColumn());
        bNotesCol.setOnEditCommit(evt -> {
            BookingRow r = evt.getRowValue();
            String newNotes = evt.getNewValue()==null ? "" : evt.getNewValue().trim();
            updateBooking(r.id(), r.specialistId(), r.startAt(), r.getStatus(), newNotes, () -> {
                r.setNotes(newNotes); bookingsTable.refresh();
            });
        });

        // initial data
        loadRoleFilter();
        loadSpecialists();
        loadUsers();
        loadBookings();
    }

    private void updateBooking(long id, long specialistId, LocalDateTime startAt,
                               String status, String notes, Runnable onOk){
        try {
            JsonObject body = new JsonObject();
            body.addProperty("id", id);
            body.addProperty("specialistId", specialistId);
            if (startAt != null)
                body.addProperty("startAt", startAt.toString()); // ISO-LOCAL-DATE-TIME
            if (status != null)
                body.addProperty("status", status);
            if (notes != null)
                body.addProperty("notes", notes);

            JsonObject res = Api.call("admin.booking.update", body);
            if (ok(res)) onOk.run();
            else alert(msg(res,"Update failed"));
        } catch (Exception e){ alert("Update booking error: "+e.getMessage()); }
    }

    @FXML
    private void loadUsers(){
        try {
            JsonObject body = new JsonObject();
            String role = roleFilter.getValue();
            if (role != null && !role.isBlank()) body.addProperty("role", role);
            JsonObject res = Api.call("admin.users.list", body);
            if (!ok(res)) { alert(msg(res,"Load users failed")); return; }

            ObservableList<UserRow> rows = FXCollections.observableArrayList();
            JsonArray items = res.getAsJsonObject("data").getAsJsonArray("items");
            for (JsonElement el : items){
                JsonObject o = el.getAsJsonObject();
                long id = getLong(o,"id",0);
                rows.add(new UserRow(
                        id,
                        getStr(o,"email",""),
                        getStr(o,"role",""),
                        getStr(o,"name","")
                ));
            }
            usersTable.setItems(rows);
        } catch (Exception e){ alert("Load users error: "+e.getMessage()); }
    }

    private void loadRoleFilter(){ /* already filled in initialize() */ }

    private void loadSpecialists(){
        try {
            JsonObject res = Api.call("admin.users.list", json("role","SPECIALIST"));
            if (!ok(res)) { alert(msg(res,"Load specialists failed")); return; }
            specialistOptions.clear();
            JsonArray items = res.getAsJsonObject("data").getAsJsonArray("items");
            for (JsonElement el : items){
                JsonObject o = el.getAsJsonObject();
                specialistOptions.add(new SpecialistOption(
                        getLong(o,"id",0),
                        getStr(o,"name", getStr(o,"email","(no name)"))
                ));
            }
        } catch (Exception e){ alert("Load specialists error: "+e.getMessage()); }
    }

    @FXML
    private void loadBookings(){
        try {
            JsonObject res = Api.call("admin.bookings.list", null);
            if (!ok(res)) { alert(msg(res,"Load bookings failed")); return; }

            ObservableList<BookingRow> rows = FXCollections.observableArrayList();
            JsonArray items = res.getAsJsonObject("data").getAsJsonArray("items");
            for (JsonElement el : items){
                JsonObject o = el.getAsJsonObject();
                long id = getLong(o,"id",0);
                long pid = getLong(o,"patientId",0);
                long sid = getLong(o,"specialistId",0);
                LocalDateTime startAt = null;
                String start = getStr(o,"startAt","");
                if (!start.isBlank()){
                    try { startAt = LocalDateTime.parse(start); } catch (Exception ignored) {}
                }
                rows.add(new BookingRow(
                        id, pid, sid,
                        getStr(o,"patientName","Patient #"+pid),
                        getStr(o,"specialistName","Spec #"+sid),
                        startAt,
                        getStr(o,"status",""),
                        getStr(o,"notes","")
                ));
            }
            bookingsTable.setItems(rows);
        } catch (Exception e){ alert("Load bookings error: "+e.getMessage()); }
    }

    // helpers
    private static JsonObject json(String k, String v){ JsonObject o=new JsonObject(); o.addProperty(k,v); return o; }
    private static boolean ok(JsonObject r){ return r!=null && r.has("status") && "ok".equalsIgnoreCase(r.get("status").getAsString()); }
    private static String msg(JsonObject r,String fb){ return (r!=null && r.has("message") && !r.get("message").isJsonNull()) ? r.get("message").getAsString() : fb; }
    private static String getStr(JsonObject o,String k,String d){ return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsString() : d; }
    private static long getLong(JsonObject o,String k,long d){ try { return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsLong() : d; } catch(Exception e){ return d; } }
    private static void alert(String m){ var a=new Alert(Alert.AlertType.INFORMATION,m,ButtonType.OK); a.setHeaderText(null); a.showAndWait(); }
}
