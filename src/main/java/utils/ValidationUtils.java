package utils;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// ============================================
// 4. VALIDATION UTILITIES
// ============================================
@UtilityClass
public class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_]{3,20}$"
    );

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$"
    );

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    public static boolean isValidLength(String str, int min, int max) {
        return str != null && str.length() >= min && str.length() <= max;
    }

    public static boolean isValidRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    public static List<String> validateUser(String username, String email, String password) {
        List<String> errors = new ArrayList<>();

        if (!isValidUsername(username)) {
            errors.add("Username must be 3-20 characters (letters, numbers, underscore only)");
        }

        if (!isValidEmail(email)) {
            errors.add("Invalid email format");
        }

        if (!isValidPassword(password)) {
            errors.add("Password must be 8+ characters with uppercase, lowercase, and number");
        }

        return errors;
    }
}
