package ths.server.handler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import ths.server.dao.UserDao;
import ths.server.model.User;
import ths.server.security.Passwords;
import ths.server.service.AuthService;

public class AuthHandler {

    private final AuthService auth = new AuthService();

    /* ------------ helpers ------------ */
    private static String reqString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            throw new IllegalArgumentException("Missing field: " + key);
        }
        String v = obj.get(key).getAsString().trim();
        if (v.isEmpty()) {
            throw new IllegalArgumentException("Missing field: " + key);
        }
        return v;
    }

    private static String optString(JsonObject obj, String key, String def) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return def;
        }
        String v = obj.get(key).getAsString().trim();
        return v.isEmpty() ? def : v;
    }
    // ---- Safe accessors (paste near top of class) ----

    private static String need(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) {
            throw new IllegalArgumentException("Missing field: " + key);
        }
        return e.getAsString();
    }

    private static String want(JsonObject o, String... keys) {
        for (String k : keys) {
            JsonElement e = o.get(k);
            if (e != null && !e.isJsonNull()) {
                return e.getAsString();
            }
        }
        return null;
    }

    // ---- Register handler (called by Router) ----
    public JsonObject register(JsonObject req) {
        try {
            // If protocol is {"method": "...", "body": {...}}, unwrap; else req is the body
            JsonObject body = (req.has("body") && req.get("body").isJsonObject())
                    ? req.getAsJsonObject("body")
                    : req;

            System.out.println("[Register] keys=" + body.keySet());

            // Accept either fullName or name from client
            String name = want(body, "fullName", "name");
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Missing field: fullName/name");
            }
            String email = need(body, "email").trim();
            String password = need(body, "password");             // client sends "password"
            String role = want(body, "role", "userType");
            if (role == null || role.isBlank()) {
                throw new IllegalArgumentException("Missing field: role/userType");
            }

            role = role.toUpperCase(Locale.ROOT);

            // ⚠️ Match DB enum exactly: PATIENT, SPECIALIST, ADMIN
            Set<String> allowed = Set.of("PATIENT", "SPECIALIST", "ADMIN");
            if (!allowed.contains(role)) {
                throw new IllegalArgumentException("Invalid role: " + role);
            }
            if (password.length() < 6) {
                throw new IllegalArgumentException("Password too short");
            }

            // Build and persist
            User u = new User();
            u.setName(name.trim());                               // ✅ maps to users.name
            u.setEmail(email);
            u.setRole(role);
            u.setPasswordHash(Passwords.hash(password));          // ✅ store hashed
            // DO NOT read created_at from JSON; DB sets it by default

            UserDao dao = new UserDao();
            dao.create(u);                                        // INSERT name,email,role,password_hash

            JsonObject ok = new JsonObject();
            ok.addProperty("status", "ok");
            ok.addProperty("message", "Registered");
            return ok;

        } catch (IllegalArgumentException ex) {
            JsonObject err = new JsonObject();
            err.addProperty("status", "error");
            err.addProperty("message", "Validation error: " + ex.getMessage());
            return err;
        } catch (Exception ex) {
            ex.printStackTrace();
            JsonObject err = new JsonObject();
            err.addProperty("status", "error");
            err.addProperty("message", "Unexpected server error");
            return err;
        }
    }

//    public JsonObject login(JsonObject req) {
//        JsonObject out = new JsonObject();
//        try {
//            JsonObject body = (req != null && req.has("body") && req.get("body").isJsonObject())
//                    ? req.getAsJsonObject("body") : req;
//
//            final String email = reqString(body, "email");
//            final String pass = reqString(body, "password");
//
//            // Call your existing service
//            JsonObject svc = auth.login(email, pass);   // might be {status:ok, userId, role, fullName, token?}
//            String status = (svc.has("status") && !svc.get("status").isJsonNull())
//                    ? svc.get("status").getAsString() : "error";
//            if (!"ok".equalsIgnoreCase(status) && !"success".equalsIgnoreCase(status)) {
//                // pass through error message
//                out.addProperty("status", "error");
//                out.addProperty("message", (svc.has("message") && !svc.get("message").isJsonNull())
//                        ? svc.get("message").getAsString() : "Invalid credentials");
//                return out;
//            }
//
//            // Build normalized data block for the client
//            JsonObject data = new JsonObject();
//            if (svc.has("token") && !svc.get("token").isJsonNull()) {
//                data.addProperty("token", svc.get("token").getAsString());
//            }
//            if (svc.has("userId") && !svc.get("userId").isJsonNull()) {
//                data.addProperty("userId", svc.get("userId").getAsLong());
//            }
//            if (svc.has("role") && !svc.get("role").isJsonNull()) {
//                data.addProperty("role", svc.get("role").getAsString());
//            }
//            if (svc.has("name") && !svc.get("name").isJsonNull()) {
//                data.addProperty("name", svc.get("name").getAsString());
//            }
//
//            JsonObject ok = new JsonObject();
//            ok.addProperty("status", "ok");
//            ok.add("data", data);        // <- client reads data.token, data.userId, etc.
//            return ok;
//
//        } catch (IllegalArgumentException e) {
//            out.addProperty("status", "error");
//            out.addProperty("message", e.getMessage());
//            return out;
//        } catch (Exception e) {
//            e.printStackTrace();
//            out.addProperty("status", "error");
//            out.addProperty("message", "Server error");
//            return out;
//        }
//    }
    public JsonObject login(JsonObject req) {
        try {
            JsonObject body = (req != null && req.has("body") && req.get("body").isJsonObject())
                    ? req.getAsJsonObject("body") : req;

            String email = reqString(body, "email");
            String pass = reqString(body, "password");

            User u = new UserDao().findByEmail(email);
            if (u == null || !Passwords.verify(pass, u.getPasswordHash())) {
                JsonObject err = new JsonObject();
                err.addProperty("status", "error");
                err.addProperty("message", "Invalid credentials");
                return err;
            }

            String token = ths.server.security.Jwt.issue(u.getId(), u.getRole());

            JsonObject data = new JsonObject();
            data.addProperty("token", token);
            data.addProperty("userId", u.getId());
            data.addProperty("role", u.getRole());
            data.addProperty("fullName", u.getName());

            JsonObject ok = new JsonObject();
            ok.addProperty("status", "ok");
            ok.add("data", data);
            return ok;

        } catch (Exception e) {
            e.printStackTrace();
            JsonObject err = new JsonObject();
            err.addProperty("status", "error");
            err.addProperty("message", "Server error");
            return err;
        }
    }

}
