package ths.client;

import com.google.gson.JsonObject;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ths.client.net.Api;

import java.net.URL;

public class App extends Application {
    private static Stage STAGE;

    
@Override public void start(Stage stage) {
  STAGE = stage;
  // REMOVE any Api.init(...) here
  load("login.fxml", "THS — Login");
  STAGE.show();
}

//    public static void load(String fxml, String title) {
//        try {
//            String path = "/ths/client/fxml/" + fxml;
//            URL url = App.class.getResource(path);
//            if (url == null) {
//                System.err.println("FXML NOT FOUND on classpath: " + path);
//                showFallbackLogin(title, "FXML not found: " + path);
//                return;
//            }
//            Parent root = FXMLLoader.load(url);
//            Scene sc = new Scene(root, 960, 600);
//            STAGE.setTitle(title);
//            STAGE.setScene(sc);
//        } catch (Exception e) {
//            e.printStackTrace();
//            showFallbackLogin(title, "Failed to load FXML: " + e.getClass().getSimpleName());
//        }
//    }
public static void load(String fxml, String title) {
  try {
    var path = "/ths/client/fxml/" + fxml;
    var url  = App.class.getResource(path);
    if (url == null) throw new IllegalStateException("FXML not found on classpath: " + path);
    Parent root = FXMLLoader.load(url);
    STAGE.setScene(new Scene(root, 960, 600));
    STAGE.setTitle(title);
  } catch (Exception e) {
    e.printStackTrace(); // <-- see the real reason in the console
    var lbl = new javafx.scene.control.Label("Failed to load FXML: " + e);
    STAGE.setScene(new Scene(new javafx.scene.layout.StackPane(lbl), 640, 360));
    STAGE.setTitle(title + " (fallback)");
  }
}

    private static void showFallbackLogin(String title, String reason) {
        TextField email = new TextField();
        email.setPromptText("Email");
        email.setText("patient@demo.local");
        PasswordField pass = new PasswordField();
        pass.setPromptText("Password");
        pass.setText("pass123");
        Label err = new Label(reason);
        err.setStyle("-fx-text-fill: red;");
        Button btn = new Button("Login");
        btn.setOnAction(a -> {
            try {
                JsonObject d = new JsonObject();
                d.addProperty("email", email.getText().trim());
                d.addProperty("password", pass.getText());
                JsonObject res = Api.call("auth.login", d);
                if (!"ok".equals(res.get("status").getAsString())) {
                    err.setText(res.get("message").getAsString()); return;
                }
                ths.client.ui.DashboardController dc = new ths.client.ui.DashboardController();
                // If FXML is available later, load it; else just change title.
                STAGE.setTitle("THS — Dashboard");
                // You can call App.load("dashboard.fxml",...) here if resources are fixed.
            } catch (Exception ex) {
                err.setText("Login failed: " + ex.getMessage());
            }
        });
        VBox box = new VBox(10, new Label("THS — Login (fallback)"), email, pass, btn, err);
        box.setPadding(new Insets(16));
        Scene sc = new Scene(box, 600, 400);
        STAGE.setTitle(title + " (fallback)");
        STAGE.setScene(sc);
    }

    public static void main(String[] args) { launch(args); }
}
