package ths.server.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public final class Jwt {
    private static String conf(String key, String def) {
        return System.getProperty(key, System.getenv().getOrDefault(key, def));
    }
    private static final String SECRET = conf("JWT_SECRET", "change-this");
    private static final long TTL_SECONDS = 60L * 60L * 8L; // 8 hours

    private record Payload(long uid, String role, long exp) {
        public String serialize() { return uid + "|" + role + "|" + exp; }
        public static Payload parse(String s) {
            String[] p = s.split("\\|");
            return new Payload(Long.parseLong(p[0]), p[1], Long.parseLong(p[2]));
        }
    }

    private static String hmac(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    public static String issue(long userId, String role) throws Exception {
        Payload p = new Payload(userId, role, Instant.now().getEpochSecond() + TTL_SECONDS);
        String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(p.serialize().getBytes(StandardCharsets.UTF_8));
        String sig = hmac(b64);
        return b64 + "." + sig;
    }

    public static Long require(String token) throws Exception {
        if (token == null || !token.contains(".")) throw new IllegalArgumentException("Missing token");
        String[] parts = token.split("\\.");
        String b64 = parts[0], sig = parts[1];
        if (!hmac(b64).equals(sig)) throw new IllegalArgumentException("Bad signature");
        Payload p = Payload.parse(new String(Base64.getUrlDecoder().decode(b64), StandardCharsets.UTF_8));
        if (Instant.now().getEpochSecond() > p.exp) throw new IllegalArgumentException("Token expired");
        return p.uid;
    }
}
