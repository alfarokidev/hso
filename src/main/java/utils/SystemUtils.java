package utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

// ============================================
// 9. SYSTEM UTILITIES
// ============================================
@UtilityClass
@Slf4j
public class SystemUtils {

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.error("Sleep interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    public static long getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    public static String getMemoryUsageFormatted() {
        return FileUtils.formatFileSize(getMemoryUsage());
    }

    public static void printMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        log.info("=== Memory Stats ===");
        log.info("Used: {}", FileUtils.formatFileSize(usedMemory));
        log.info("Free: {}", FileUtils.formatFileSize(freeMemory));
        log.info("Total: {}", FileUtils.formatFileSize(totalMemory));
        log.info("Max: {}", FileUtils.formatFileSize(maxMemory));
    }

    public static void gc() {
        System.gc();
        log.info("Garbage collection requested");
    }
}
