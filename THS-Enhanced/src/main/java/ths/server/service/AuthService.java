package ths.server.service;

import com.google.gson.JsonObject;
import ths.server.dao.UserDao;
import ths.server.model.User;
import ths.server.security.Passwords;

public class AuthService {

    private static final String DEFAULT_ROLE = "PATIENT";

    private static String normalizeRole(String role) {
        if (role == null) return DEFAULT_ROLE;
        String r = role.trim().toUpperCase();
        return switch (r) {
            case "PATIENT", "SPECIALIST", "ADMIN" -> r;
            default -> DEFAULT_ROLE;
        };
    }

    private static void require(boolean cond, String msg) {
        if (!cond) throw new IllegalArgumentException(msg);
    }

    /** Returns: {status: "ok", userId, role, fullName} or {status:"error", message} */
    public JsonObject register(String fullName, String email, String password, String role) throws Exception {
        JsonObject out = new JsonObject();

        // Validation
        require(fullName != null && !fullName.isBlank(), "Missing field: fullName");
        require(email != null && !email.isBlank(),       "Missing field: email");
        require(password != null && password.length() >= 6, "Password must be at least 6 characters");

        // Uniqueness
        if (UserDao.findByEmail(email) != null) {
            out.addProperty("status", "error");
            out.addProperty("message", "Email already in use");
            return out;
        }

        // Persist
        String hash = Passwords.hash(password);
        String r = normalizeRole(role);
        User u = UserDao.create(fullName.trim(), email.trim(), hash, r);

        out.addProperty("status",   "ok");
        out.addProperty("userId",   u.getId());
        out.addProperty("role",     u.getRole());
        out.addProperty("name", u.getName());
        return out;
    }

    /** Returns: {status:"ok", userId, role, fullName} or {status:"error", message} */
    public JsonObject login(String email, String password) throws Exception {
        JsonObject out = new JsonObject();

        require(email != null && !email.isBlank(), "Missing field: email");
        require(password != null && !password.isBlank(), "Missing field: password");

        User u = UserDao.findByEmail(email.trim());
        if (u == null) {
            out.addProperty("status", "error");
            out.addProperty("message", "Invalid credentials");
            return out;
        }

        boolean ok = Passwords.verify(password, u.getPasswordHash());
        if (!ok) {
            out.addProperty("status", "error");
            out.addProperty("message", "Invalid credentials");
            return out;
        }

        out.addProperty("status",   "ok");
        out.addProperty("userId",   u.getId());
        out.addProperty("role",     u.getRole());
        out.addProperty("name", u.getName());
        return out;
    }
}
