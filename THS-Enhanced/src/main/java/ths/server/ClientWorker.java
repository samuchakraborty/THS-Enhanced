package ths.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ths.server.security.Jwt;

import java.io.*;
import java.net.Socket;
import java.util.Locale;
import java.util.Set;
import ths.server.Protocol;

public class ClientWorker implements Runnable {

    private static final Gson G = new Gson();
    private static final Set<String> PUBLIC_ACTIONS = Set.of(
            "REGISTER", "LOGIN",
            "AUTH.REGISTER", "AUTH.LOGIN",
            "PING", "HEALTH"
    );

    private final Socket socket;

    public ClientWorker(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            String line;
            while ((line = in.readLine()) != null) {
                try {
                    // Parse incoming line → JSON object
                    JsonObject req = G.fromJson(line, JsonObject.class);
                    if (req == null || !req.has("action") || req.get("action").isJsonNull()) {
                        sendError(out, "Validation error: Missing field: action");
                        continue;
                    }

                    // Keep the original action for routing
                    String rawAction = req.get("action").getAsString();
                    if (rawAction == null || rawAction.isBlank()) {
                        sendError(out, "Validation error: Missing field: action");
                        continue;
                    }

                    // Normalize for auth gate only (do not change Router keys)
                    String normAction = rawAction.trim().toUpperCase(Locale.ROOT);

                    // Token (top-level)
                    String token = (req.has("token") && !req.get("token").isJsonNull())
                            ? req.get("token").getAsString()
                            : null;

                    JsonObject res;

                    // Public actions skip JWT check
                    if (PUBLIC_ACTIONS.contains(normAction)) {
                        res = Router.route(rawAction, req, null);
                    } else {
                        // Private actions require a valid token → userId
                        Long userId = Jwt.require(token); // should throw with clear message if null/invalid
                        res = Router.route(rawAction, req, userId);
                    }

                    sendJson(out, res);

                } catch (Exception e) {
                    // Convert any thrown exception to a JSON error response
//                    sendRaw(out, Protocol.error(e.getMessage()));
                    sendJson(out, Protocol.error(e.getMessage()));

                }
            }
        } catch (Exception ignored) {
            // socket closed / network error — safe to ignore
        }
    }

    /* ------------ send helpers ------------ */

 /* ------------ send helpers ------------ */
    private static void sendJson(BufferedWriter out, JsonObject obj) throws IOException {
        out.write(G.toJson(obj));
        out.newLine();
        out.flush();
    }

    private static void sendError(BufferedWriter out, String message) throws IOException {
        // OLD: sendRaw(out, Protocol.error(message));  // wrote a String
        sendJson(out, Protocol.error(message));         // ✅ send a proper JSON object
    }

    private static void sendRaw(BufferedWriter out, String json) throws IOException {
        out.write(json);
        out.newLine();
        out.flush();
    }

}
