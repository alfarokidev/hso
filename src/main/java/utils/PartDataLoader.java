package utils;

import com.github.benmanes.caffeine.cache.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;
import java.util.stream.Collectors;

@Slf4j
public class PartDataLoader {

    private static final String BASE_DIR = "data/part";

    // ---------------- CACHE ----------------
    private static final LoadingCache<String, PartData> PART_CACHE =
            Caffeine.newBuilder()
                    .maximumSize(2000)                  // cache frequently used parts
                    .expireAfterAccess(30, TimeUnit.MINUTES)
                    .build(PartDataLoader::loadFromDisk);

    // cache key = "variant:type:id"
    private static String key(String variant, byte type, short id) {
        return variant + ":" + type + ":" + id;
    }

    // zoomLv -> variantName
    private static String resolveVariant(int zoomLv) {
        return switch (zoomLv) {
            case 2 -> "x2";
            case 3 -> "x3";
            case 4 -> "x4";
            default -> "x1";
        };
    }

    // ---------------- PUBLIC API ----------------

    /**
     * Get a specific part by zoom level, type, and id
     */
    public static PartData getByZoom(int zoomLv, byte type, short id) {
        String variant = resolveVariant(zoomLv);
        return PART_CACHE.get(key(variant, type, id));
    }

    /**
     * Save a part to disk and invalidate cache
     */
    public static Path savePart(int zoomLv, byte type, short id, byte[] imageBytes, byte[] dataBytes)
            throws IOException {
        String variant = resolveVariant(zoomLv);
        String imageDir = BASE_DIR + "/" + variant + "/img";
        String dataDir = BASE_DIR + "/" + variant + "/data";

        String baseName = type + "_" + id;
        Path imagePath = Paths.get(imageDir, baseName + ".png");
        Path dataPath = Paths.get(dataDir, baseName);

        Files.createDirectories(imagePath.getParent());
        Files.createDirectories(dataPath.getParent());

        Files.write(imagePath, imageBytes);
        Files.write(dataPath, dataBytes);

        // Invalidate cache for this part
        PART_CACHE.invalidate(key(variant, type, id));

        return imagePath;
    }

    // ---------------- LAZY LOADER ----------------

    /**
     * Load a specific part from disk (called by Caffeine cache)
     * Loads BOTH the .png image file and the data file (no extension)
     */
    private static PartData loadFromDisk(String key) {
        try {
            String[] parts = key.split(":");
            String variant = parts[0];
            byte type = Byte.parseByte(parts[1]);
            short id = Short.parseShort(parts[2]);

            String imageDir = BASE_DIR + "/" + variant + "/img";
            String dataDir = BASE_DIR + "/" + variant + "/data";

            String baseName = type + "_" + id;
            Path imagePath = Paths.get(imageDir, baseName + ".png");    // Image with .png
            Path dataPath = Paths.get(dataDir, baseName);                // Data with NO extension

            // Check if BOTH files exist
            if (!Files.exists(imagePath)) {
                log.warn("[PartDataLoader][{}] Image not found: {}", variant, imagePath);
                return null;
            }
            if (!Files.exists(dataPath)) {
                log.warn("[PartDataLoader][{}] Data not found: {}", variant, dataPath);
                return null;
            }

            byte[] imageBytes = Files.readAllBytes(imagePath);
            byte[] dataBytes = Files.readAllBytes(dataPath);

            PartData part = new PartData();
            part.type = type;
            part.id = id;
            part.image = imageBytes;      // PNG image bytes
            part.imageData = dataBytes;   // Data file bytes (no extension)

            return part;

        } catch (Exception e) {
            log.error("[PartDataLoader] Load failed: {}", key, e);
            return null;
        }
    }

    // ---------------- BATCH OPERATIONS ----------------

    /**
     * Load all parts of a specific type for a zoom level
     * Returns list of loaded PartData objects
     */
    public static List<PartData> loadPartsByType(int zoomLv, byte type) {
        String variant = resolveVariant(zoomLv);
        String imageDir = BASE_DIR + "/" + variant + "/img";
        String dataDir = BASE_DIR + "/" + variant + "/data";

        List<PartData> loadedParts = new ArrayList<>();
        Path imageDirPath = Paths.get(imageDir);

        if (!Files.exists(imageDirPath)) {
            log.info("[PartDataLoader][{}] Image dir not found", variant);
            return loadedParts;
        }

        log.info("[PartDataLoader] Loading type {} for {}", type, variant);

        try {
            String prefix = type + "_";
            Files.list(imageDirPath)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith(prefix) && name.endsWith(".png");
                    })
                    .forEach(imagePath -> {
                        String fileName = imagePath.getFileName().toString();
                        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));

                        Matcher matcher = Pattern.compile("(\\d+)_(\\d+)").matcher(baseName);
                        if (!matcher.matches()) return;

                        byte fileType = Byte.parseByte(matcher.group(1));
                        short id = Short.parseShort(matcher.group(2));

                        // Use cache to get the part (will load if not cached)
                        PartData part = getByZoom(zoomLv, fileType, id);
                        if (part != null) {
                            loadedParts.add(part);
                        }
                    });
        } catch (IOException e) {
            log.error("[PartDataLoader][{}] Error reading directory: {}", variant, e.getMessage());
        }

        loadedParts.sort(Comparator.comparingInt(p -> p.id));
        log.debug("[PartDataLoader] ✅ Loaded {} parts of type {}", loadedParts.size(), type);

        return loadedParts;
    }

    /**
     * Get all parts for a zoom level
     * Scans directory and loads through cache
     */
    public static List<PartData> getAllByZoom(int zoomLv) {
        String variant = resolveVariant(zoomLv);
        String imageDir = BASE_DIR + "/" + variant + "/img";
        Path imageDirPath = Paths.get(imageDir);

        if (!Files.exists(imageDirPath)) {
            log.warn("[PartDataLoader][{}] Image dir not found", variant);
            return Collections.emptyList();
        }

        List<PartData> allParts = new ArrayList<>();

        try {
            List<Path> imagePaths = Files.list(imageDirPath)
                    .filter(path -> path.toString().endsWith(".png"))
                    .collect(Collectors.toList());

            log.debug("[PartDataLoader][{}] Found {} image files", variant, imagePaths.size());

            for (Path imagePath : imagePaths) {
                String fileName = imagePath.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));

                Matcher matcher = Pattern.compile("(\\d+)_(\\d+)").matcher(baseName);
                if (!matcher.matches()) {
                    log.warn("[PartDataLoader][{}] Invalid filename format: {}", variant, baseName);
                    continue;
                }

                byte type = Byte.parseByte(matcher.group(1));
                short id = Short.parseShort(matcher.group(2));

                PartData part = getByZoom(zoomLv, type, id);
                if (part != null) {
                    allParts.add(part);
                } else {
                    log.warn("[PartDataLoader][{}] Failed to load part: {}_{}", variant, type, id);
                }
            }
        } catch (IOException e) {
            log.error("[PartDataLoader][{}] Error reading directory: {}", variant, e.getMessage());
        }

        allParts.sort(Comparator.comparingInt(p -> p.id));
        log.debug("[PartDataLoader][{}] Successfully loaded {} parts", variant, allParts.size());
        return allParts;
    }

    public static int getPartIndex(int zoomLv) {
        return switch (zoomLv) {
            case 1 -> -1701;
            case 2 -> 2040;
            case 3 -> 12248;
            case 4 -> -21362;
            default -> -1;
        };
    }
}