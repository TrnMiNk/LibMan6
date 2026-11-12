package dao6;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.*;
import java.util.*;

public final class PasswordHash6 {
    private static final int ITERATIONS = 600_000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String DUMMY = hash("invalid-account-timing-only".toCharArray());
    private PasswordHash6() {}

    public static String hash(char[] password) {
        byte[] salt = new byte[16]; RANDOM.nextBytes(salt);
        return "pbkdf2_sha256$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }
    public static boolean verify(char[] password, String encoded) {
        boolean supported = encoded != null && encoded.startsWith("pbkdf2_sha256$");
        String candidate = supported ? encoded : DUMMY;
        try {
            String[] parts = candidate.split("\\$");
            if (parts.length != 4) return false;
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            if (iterations < ITERATIONS || iterations > 2_000_000 || salt.length != 16 || expected.length != 32) return false;
            boolean matches = MessageDigest.isEqual(expected, derive(password, salt, iterations));
            return supported && matches;
        } catch (IllegalArgumentException exception) { return false; }
    }
    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException exception) { throw new IllegalStateException("Password hashing unavailable", exception); }
        finally { spec.clearPassword(); }
    }
}
