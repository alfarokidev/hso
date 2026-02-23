package utils;

import lombok.experimental.UtilityClass;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

// ============================================
// 3. DATE/TIME UTILITIES
// ============================================
@UtilityClass
public class DateTimeUtils {

    private static final SimpleDateFormat DEFAULT_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static long now() {
        return System.currentTimeMillis();
    }

    public static String formatTimestamp(long timestamp) {
        return DEFAULT_FORMAT.format(new Date(timestamp));
    }

    public static String formatTimestamp(long timestamp, String pattern) {
        return new SimpleDateFormat(pattern).format(new Date(timestamp));
    }

    public static long addDays(long timestamp, int days) {
        return timestamp + TimeUnit.DAYS.toMillis(days);
    }

    public static long addHours(long timestamp, int hours) {
        return timestamp + TimeUnit.HOURS.toMillis(hours);
    }

    public static long addMinutes(long timestamp, int minutes) {
        return timestamp + TimeUnit.MINUTES.toMillis(minutes);
    }

    public static boolean isExpired(long timestamp) {
        return timestamp < now();
    }

    public static boolean isToday(long timestamp) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(timestamp);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    public static long getStartOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long getEndOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    public static String getTimeAgo(long timestamp) {
        long diff = now() - timestamp;

        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        if (seconds < 60) return seconds + " seconds ago";

        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        if (minutes < 60) return minutes + " minutes ago";

        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        if (hours < 24) return hours + " hours ago";

        long days = TimeUnit.MILLISECONDS.toDays(diff);
        if (days < 7) return days + " days ago";

        long weeks = days / 7;
        if (weeks < 4) return weeks + " weeks ago";

        long months = days / 30;
        if (months < 12) return months + " months ago";

        long years = days / 365;
        return years + " years ago";
    }
}
