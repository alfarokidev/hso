package utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

// ============================================
// 10. RETRY UTILITIES
// ============================================
@UtilityClass
@Slf4j
public class RetryUtils {

    public static <T> T retry(RetryOperation<T> operation, int maxAttempts, long delayMillis) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed: {}", attempt, maxAttempts, e.getMessage());

                if (attempt < maxAttempts) {
                    SystemUtils.sleep(delayMillis);
                }
            }
        }

        log.error("All {} attempts failed", maxAttempts, lastException);
        return null;
    }

    @FunctionalInterface
    public interface RetryOperation<T> {
        T execute() throws Exception;
    }
}
