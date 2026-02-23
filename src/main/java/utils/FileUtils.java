package utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// ============================================
// 8. FILE UTILITIES
// ============================================
@UtilityClass
@Slf4j
public class FileUtils {
    private static ConcurrentMap<Path, byte[]> files = new ConcurrentHashMap<>();

    public static String getFileExtension(String filename) {
        if (StringUtils.isEmpty(filename)) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    public static String getFileName(String path) {
        if (StringUtils.isEmpty(path)) return "";
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    public static String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }


    public static byte[] loadFile(String fileName) {
        Path path = Path.of(fileName).normalize().toAbsolutePath();
        if (!Files.exists(path)) {
            return null;
        }

        return files.computeIfAbsent(path, p -> {
            try {
                return Files.readAllBytes(p);
            } catch (IOException e) {
                log.error("File not found: {}", p, e);
                return null;
            }
        });
    }

}
