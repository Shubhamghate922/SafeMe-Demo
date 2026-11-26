package com.safeme.app.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods for date/time formatting, countdown formatting, and GPS freshness status.
 */
public final class DateTimeUtils {

    private DateTimeUtils() {}

    public static String formatCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault());
        return sdf.format(new Date());
    }

    public static String formatTimestamp(long millis) {
        if (millis <= 0) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    public static String formatTimeOnly(long millis) {
        if (millis <= 0) return "--:--";
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    public static String formatCountdownSeconds(long totalSeconds) {
        if (totalSeconds <= 0) return "00:00";
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    public static String getLocationFreshnessText(long lastUpdateMillis) {
        if (lastUpdateMillis <= 0) {
            return "No GPS fix recorded yet";
        }
        long ageMillis = System.currentTimeMillis() - lastUpdateMillis;
        if (ageMillis < 0) ageMillis = 0;

        long seconds = TimeUnit.MILLISECONDS.toSeconds(ageMillis);
        if (seconds < 15) {
            return "Live (" + seconds + "s ago)";
        } else if (seconds < 60) {
            return "Updated " + seconds + "s ago";
        } else if (seconds < 120) {
            return "Updated 1m ago";
        } else {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(ageMillis);
            return "Stale (" + minutes + "m old - not live)";
        }
    }

    public static boolean isLocationLive(long lastUpdateMillis) {
        if (lastUpdateMillis <= 0) return false;
        long ageMillis = System.currentTimeMillis() - lastUpdateMillis;
        return ageMillis <= 90_000L;
    }
}
