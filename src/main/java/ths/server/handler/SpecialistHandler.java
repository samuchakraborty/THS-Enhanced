// ths.server.handler.SpecialistHandler.java
package ths.server.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ths.server.Protocol;
import ths.server.dao.UserDao;
import ths.server.model.User;

import java.util.List;

public class SpecialistHandler {
  public static JsonObject list(JsonObject req, Long userId) {
    try {
      List<User> specs = UserDao.listByRole("SPECIALIST");
      JsonArray items = new JsonArray();
      for (User u : specs) {
        JsonObject o = new JsonObject();
        o.addProperty("id", u.getId());
        // fall back to email if fullName is null/blank
        String display = (u.getName() != null && !u.getName().isBlank())
                ? u.getName() : u.getEmail();
        o.addProperty("name", display);
        o.addProperty("email", u.getEmail());
        items.add(o);
      }
      JsonObject data = new JsonObject();
      data.add("items", items);
      return Protocol.ok("ok", data);
    } catch (Exception e) {
      e.printStackTrace();
      return Protocol.error("Failed to load specialists");
    }
  }
}
