package ths.server.security;

import org.mindrot.jbcrypt.BCrypt;

public final class Passwords {
    public static String hash(String raw) { return BCrypt.hashpw(raw, BCrypt.gensalt(12)); }
    public static boolean verify(String raw, String hash) { return BCrypt.checkpw(raw, hash); }
}
