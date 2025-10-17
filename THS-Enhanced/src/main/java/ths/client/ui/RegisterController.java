package ths.client.ui;

import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ths.client.App;
import ths.client.net.Api;

public class RegisterController {

    @FXML
    private TextField fullName, email;
    @FXML
    private PasswordField password;
    @FXML
    private ChoiceBox<String> role;
    @FXML
    private Label error;
    @FXML
    private Button createBtn;

    @FXML
    public void initialize() {
        role.setItems(FXCollections.observableArrayList("PATIENT", "SPECIALIST"));
        role.getSelectionModel().select("PATIENT");
        error.setText("");
    }

    @FXML
    void onRegister() {
        error.setText("");

        String name = trim(fullName.getText());
        String mail = trim(email.getText());
        String pass = password.getText();
        String r = role.getValue();

        if (isBlank(name) || isBlank(mail) || isBlank(pass) || isBlank(r)) {
            error.setText("Please fill all fields.");
            return;
        }

        if (createBtn != null) {
            createBtn.setDisable(true);
        }

        try {
            JsonObject body = new JsonObject();
            body.addProperty("name", name);
            body.addProperty("email", mail);
            body.addProperty("password", pass);
            body.addProperty("role", r);

            System.out.println("[Register] sending body: " + body);
//      JsonObject res = Api.call("REGISTER", body);
            JsonObject res = Api.call("auth.register", body);

            System.out.println("[Register] server response: " + res);

            if (!isOk(res)) {
                String msg = coalesce(
                        getStr(res, "message"),
                        getStr(res, "error"),
                        getStr(getObj(res, "data"), "message"),
                        "Registration failed."
                );
                error.setText(msg);
                return;
            }

            new Alert(Alert.AlertType.INFORMATION, "Account created. Please log in.").showAndWait();
            App.load("login.fxml", "THS — Login");

        } catch (Exception ex) {
            ex.printStackTrace();
            error.setText("Network/Server error: " + ex.getMessage());
        } finally {
            if (createBtn != null) {
                createBtn.setDisable(false);
            }
        }
    }

    @FXML
    void toLogin() {
        App.load("login.fxml", "THS — Login");
    }

    // --- helpers ---------------------------------------------------------------
    private static boolean isOk(JsonObject res) {
        String s = getStr(res, "status");
        if ("ok".equalsIgnoreCase(s) || "success".equalsIgnoreCase(s)) {
            return true;
        }
        if (getBool(res, "ok") || getBool(res, "success")) {
            return true;
        }
        JsonObject data = getObj(res, "data");
        return data != null && (getBool(data, "ok") || getBool(data, "success"));
    }

    private static boolean getBool(JsonObject o, String k) {
        try {
            return o != null && o.has(k) && !o.get(k).isJsonNull() && o.get(k).getAsBoolean();
        } catch (Exception ignore) {
            return false;
        }
    }

    private static String getStr(JsonObject o, String k) {
        try {
            return (o != null && k != null && o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsString() : null;
        } catch (Exception ignore) {
            return null;
        }
    }

    private static JsonObject getObj(JsonObject o, String k) {
        try {
            return (o != null && k != null && o.has(k) && o.get(k).isJsonObject()) ? o.getAsJsonObject(k) : null;
        } catch (Exception ignore) {
            return null;
        }
    }

    private static String coalesce(String... xs) {
        for (String x : xs) {
            if (x != null && !x.isBlank()) {
                return x;
            }
        }
        return null;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
