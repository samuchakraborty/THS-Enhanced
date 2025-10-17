package ths.client.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Minimal JSON-over-TCP client that sends/receives one JSON object per line.
 * Protocol: {"action":"REGISTER","body":{...},"token":"..."}
 */
public class SocketClient implements Closeable {

  private final String host;
  private final int port;

  private Socket socket;
  private BufferedReader reader;
  private BufferedWriter writer;

  private String token; // optional, added to each payload

  public SocketClient(String host, int port) throws IOException {
    this.host = host;
    this.port = port;
    connect();
  }

  // --- connection management -------------------------------------------------

  private synchronized void connect() throws IOException {
    if (socket != null && socket.isConnected() && !socket.isClosed()) return;

    socket = new Socket();
    socket.connect(new InetSocketAddress(host, port), 5000);
    socket.setSoTimeout(15000);

    reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
  }

  private synchronized void ensureConnected() throws IOException {
    if (socket == null || socket.isClosed() || !socket.isConnected()) {
      connect();
    }
  }

  // --- token -----------------------------------------------------------------

  public synchronized void setToken(String token) {
    this.token = token;
  }

  // --- send helpers ----------------------------------------------------------

  /**
   * Preferred entry: build wrapper and send.
   */
  public synchronized JsonObject send(String action, JsonObject body) throws Exception {
    JsonObject payload = new JsonObject();
    payload.addProperty("action", action);
    if (body != null) payload.add("body", body);
    if (token != null && !token.isBlank()) payload.addProperty("token", token);
    return send(payload);
  }

  /**
   * Low-level send of a fully-built JSON payload object.
   * Writes one line, reads one line (line-delimited JSON).
   */
  public synchronized JsonObject send(JsonObject payload) throws Exception {
    ensureConnected();

    // Log for debugging
    System.out.println("[SocketClient] sending: " + payload);

    try {
      writer.write(payload.toString());
      writer.write("\n");
      writer.flush();

      String line = reader.readLine();
      if (line == null) {
        throw new EOFException("Server closed connection");
      }

      JsonObject res = JsonParser.parseString(line).getAsJsonObject();
      System.out.println("[SocketClient] received: " + res);
      return res;

    } catch (IOException io) {
      // attempt one reconnect, then rethrow
      try {
        close();
      } catch (IOException ignore) {}
      connect();

      // retry once after reconnect
      writer.write(payload.toString());
      writer.write("\n");
      writer.flush();

      String line = reader.readLine();
      if (line == null) throw new EOFException("Server closed connection (after reconnect)");
      JsonObject res = JsonParser.parseString(line).getAsJsonObject();
      System.out.println("[SocketClient] received(after reconnect): " + res);
      return res;
    }
  }

  // --- cleanup ---------------------------------------------------------------

  @Override
  public synchronized void close() throws IOException {
    IOException first = null;
    try { if (reader != null) reader.close(); } catch (IOException e) { first = e; }
    try { if (writer != null) writer.close(); } catch (IOException e) { if (first == null) first = e; }
    try { if (socket != null) socket.close(); } catch (IOException e) { if (first == null) first = e; }
    reader = null; writer = null; socket = null;
    if (first != null) throw first;
  }
}
