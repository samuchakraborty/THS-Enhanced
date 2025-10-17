package ths.client.net;

import com.google.gson.JsonObject;
import java.io.IOException;

/**
 * High-level API wrapper. Builds {"action", "body", "token"} and sends via SocketClient.
 */
public final class Api {
  private static SocketClient client;
  private static String token;

  private Api() {}

  // --- lifecycle -------------------------------------------------------------

  public static synchronized void init(String host, int port) throws IOException {
    if (client == null) {
      client = new SocketClient(host, port);
      if (token != null) {
        try { client.setToken(token); } catch (Throwable ignore) {}
      }
    }
  }

  private static synchronized void ensureClient() throws IOException {
    if (client == null) {
      String host = System.getProperty("SERVER_HOST", "127.0.0.1");
      // Use parseString to support default properly unless -DSERVER_PORT provided
      int port = Integer.parseInt(System.getProperty("SERVER_PORT", "7777"));
      init(host, port);
    }
  }

  // --- calls -----------------------------------------------------------------

  /**
   * Sends {"action": action, "body": body, "token": token?} to the server.
   */
  public static JsonObject call(String action, JsonObject body) throws Exception {
    ensureClient();
    return client.send(action, body);
  }

  // --- token management ------------------------------------------------------

  public static synchronized void setToken(String t) {
    token = t;
    if (client != null) {
      try { client.setToken(t); } catch (Throwable ignore) {}
    }
    if (t == null) {
      System.clearProperty("JWT_TOKEN");
    } else {
      System.setProperty("JWT_TOKEN", t); // optional persistence across runs
    }
  }

  public static synchronized String getToken() {
    return token;
  }

  public static synchronized void clearToken() {
    setToken(null);
  }
}
