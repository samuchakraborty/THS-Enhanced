package ths.server.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ths.server.Protocol;
import ths.server.dao.UserDao;
import ths.server.model.User;

import java.util.List;

public class AdminUsersHandler {

    private static JsonObject body(JsonObject req){
        return (req!=null && req.has("body") && req.get("body").isJsonObject()) ? req.getAsJsonObject("body") : req;
    }

    // GET: admin.users.list { role? }
    public static JsonObject list(JsonObject req, Long userId){
        // Optional: verify admin role here
        try {
            JsonObject b = body(req);
            String role = (b!=null && b.has("role") && !b.get("role").isJsonNull()) ? b.get("role").getAsString() : null;
            List<User> users = (role==null || role.isBlank()) ? UserDao.listByRole("PATIENT") : UserDao.listByRole(role);
            // If you want *all* roles when role==null, add a DAO method; here we reuse listByRole
            if (role==null || role.isBlank()){
                // quick concat all roles
                users = new java.util.ArrayList<>();
                users.addAll(UserDao.listByRole("PATIENT"));
                users.addAll(UserDao.listByRole("SPECIALIST"));
                users.addAll(UserDao.listByRole("ADMIN"));
            }
            JsonArray items = new JsonArray();
            for (User u : users){
                JsonObject o = new JsonObject();
                o.addProperty("id", u.getId());
                o.addProperty("email", u.getEmail());
                o.addProperty("role", u.getRole());
                o.addProperty("name", u.getName());
                items.add(o);
            }
            JsonObject ok = Protocol.ok();
            ok.getAsJsonObject("data").add("items", items);
            return ok;
        } catch (Exception e){
            e.printStackTrace();
            return Protocol.error("Failed to load users");
        }
    }

    // POST: admin.user.updateName { id, name }
    public static JsonObject updateName(JsonObject req, Long userId){
        try {
            JsonObject b = body(req);
            long id = b.get("id").getAsLong();
            String name = b.get("name").getAsString();
            try (var cx = ths.server.db.Sql.get();
                 var ps = cx.prepareStatement("UPDATE users SET name=? WHERE id=?")) {
                ps.setString(1, name);
                ps.setLong(2, id);
                ps.executeUpdate();
            }
            return Protocol.ok();
        } catch (Exception e){
            e.printStackTrace();
            return Protocol.error("Update failed");
        }
    }
}
