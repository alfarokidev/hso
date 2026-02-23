package utils;

import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// ============================================
// 2. NUMBER UTILITIES
// ============================================
@UtilityClass
public class NumberUtils {
    private static final Random RANDOM = new Random();
    private static final Set<Integer> usedBotIds = ConcurrentHashMap.newKeySet();
    private static final Queue<Integer> POOL;

    static {
        List<Integer> ids = IntStream.rangeClosed(1000, 9999)
                .boxed()
                .collect(Collectors.toList());
        Collections.shuffle(ids);
        POOL = new LinkedList<>(ids);
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int randomInt(int min, int max) {
        return RANDOM.nextInt(max - min + 1) + min;
    }

    public static int percent() {
        return RANDOM.nextInt(1, 101);
    }
    public static double randomDouble(double min, double max) {
        return min + (max - min) * new Random().nextDouble();
    }

    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    public static int percentage(int value, int total) {
        if (total == 0) return 0;
        return (value * 100) / total;
    }

    public static String formatNumber(long number) {
        if (number >= 1_000_000_000) return String.format("%.1fB", number / 1_000_000_000.0);
        if (number >= 1_000_000) return String.format("%.1fM", number / 1_000_000.0);
        if (number >= 1_000) return String.format("%.1fK", number / 1_000.0);
        return String.valueOf(number);
    }

    public static int increaseBy(int baseValue, int value) {
        return baseValue + (int) ((long) baseValue * value / 10000);
    }
    public static int increaseByPercent(int baseValue, int basePercent, int perPlusPercent, int plus) {
        double totalPercent = basePercent + perPlusPercent * plus;
        return (int) (baseValue * (1 + totalPercent / 100.0));
    }



    public static int next() {
        if (POOL.isEmpty()) throw new IllegalStateException("No available IDs.");
        return POOL.poll();
    }

    public static void release(int id) {
        if (id < 1000 || id > 9999) throw new IllegalArgumentException("Invalid ID: " + id);
        POOL.offer(id);
    }

    public static int available() {
        return POOL.size();
    }

}
