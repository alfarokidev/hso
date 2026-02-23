package utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

// ============================================
// 5. ENCRYPTION/HASH UTILITIES
// ============================================
@UtilityClass
@Slf4j
public class CryptoUtils {

    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("MD5 hashing failed", e);
            return null;
        }
    }
    public boolean verifyMD5(String password, String storedHash) {
        try {
            String hashOfInput = md5(password); // use your md5() method
            return hashOfInput != null && hashOfInput.equalsIgnoreCase(storedHash);
        } catch (Exception e) {
            return false;
        }
    }

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("SHA-256 hashing failed", e);
            return null;
        }
    }

    public static String generateToken(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashPassword(String password) {
        // Simple implementation - in production use BCrypt or similar
        return sha256(password + "salt_key_here");
    }



    public static boolean verifyPassword(String password, String hash) {
        return hashPassword(password).equals(hash);
    }
}
