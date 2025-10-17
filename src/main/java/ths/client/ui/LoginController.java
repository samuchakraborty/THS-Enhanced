package ths.client.ui;

import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ths.client.App;
import ths.client.net.Api;

public class LoginController {
    @FXML private TextField email;
    @FXML private PasswordField password;
    @FXML private Label error;
    @FXML private Button loginBtn;

    @FXML public void initialize() { if (error != null) error.setText(""); }

    @FXML void toRegister() { App.load("register.fxml", "THS — Register"); }

    @FXML private void onLogin() {
        if (loginBtn != null) loginBtn.setDisable(true);
        error.setText("");

        try {
            JsonObject body = new JsonObject();
            body.addProperty("email", safe(email.getText()));
            body.addProperty("password", password.getText());

            JsonObject res = Api.call("auth.login", body);
            if (!isOk(res)) {
                error.setText(coalesce(getStr(res, "message"), "Login failed"));
                return;
            }

            // Accept both formats: {status:ok, data:{...}} or top-level fields
            JsonObject data = getObj(res, "data");
            String token   = coalesce(getStr(res, "token"),   getStr(data, "token"));
            String role    = coalesce(getStr(res, "role"),    getStr(data, "role"));
            String name    = coalesce(getStr(res, "name"),getStr(data, "name"));
            System.out.println("[Token]role=" + token + ", name=" + token);

            // Only set token if the server provided one
            if (token != null && !token.isBlank()) {
                Api.setToken(token);
            }
            System.out.println("[After Token]role=" + token + ", name=" + token);

            // Optionally, store role/name somewhere global if you use them in UI
            System.out.println("[Login] ok; role=" + role + ", name=" + name);

            App.load("dashboard.fxml", "THS — Dashboard");

        } catch (Exception e) {
            e.printStackTrace();
            error.setText("Login failed");
        } finally {
            if (loginBtn != null) loginBtn.setDisable(false);
        }
    }

    // -------- helpers ----------
    private static boolean isOk(JsonObject o) {
        String s = getStr(o, "status");
        return "ok".equalsIgnoreCase(s) || "success".equalsIgnoreCase(s) || getBool(o, "ok") || getBool(o, "success");
    }
    private static boolean getBool(JsonObject o, String k) {
        try { return o != null && o.has(k) && !o.get(k).isJsonNull() && o.get(k).getAsBoolean(); }
        catch (Exception e) { return false; }
    }
    private static String getStr(JsonObject o, String k) {
        try { return (o != null && k != null && o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsString() : null; }
        catch (Exception e) { return null; }
    }
    private static JsonObject getObj(JsonObject o, String k) {
        try { return (o != null && k != null && o.has(k) && o.get(k).isJsonObject()) ? o.getAsJsonObject(k) : null; }
        catch (Exception e) { return null; }
    }
    private static String coalesce(String... xs) {
        for (String x : xs) if (x != null && !x.isBlank()) return x;
        return null;
    }
    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
