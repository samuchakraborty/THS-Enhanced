// ths/server/Protocol.java
package ths.server;

import com.google.gson.JsonObject;

public final class Protocol {
  private Protocol(){}

  public static JsonObject ok() {
    JsonObject o = new JsonObject();
    o.addProperty("status", "ok");
    o.add("data", new JsonObject());
    return o;
  }

  public static JsonObject ok(JsonObject data) {
    JsonObject o = new JsonObject();
    o.addProperty("status", "ok");
    o.add("data", data != null ? data : new JsonObject());
    return o;
  }

  public static JsonObject ok(String message, JsonObject data) {
    JsonObject o = ok(data);
    if (message != null && !message.isBlank()) o.addProperty("message", message);
    return o;
  }

  public static JsonObject error(String msg) {
    JsonObject o = new JsonObject();
    o.addProperty("status", "error");
    o.addProperty("message", (msg == null || msg.isBlank()) ? "error" : msg);
    return o;
  }
}
