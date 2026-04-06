package com.example.ajp.utils;

/**
 * Shared utility class for TimeFormatUtil.
 * Encapsulates reusable behavior that would otherwise be duplicated across features.
 * Centralizing this logic keeps edge-case handling consistent and easier to test.
 */

public final class TimeFormatUtil {

    private TimeFormatUtil() { }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public static String formatMinutesToHourMin(int totalMinutes) {
        if (totalMinutes < 0) totalMinutes = 0;
        if (totalMinutes < 60) return totalMinutes + "m";
        if (totalMinutes == 60) return "1hr";
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        if (mins == 0) return hours + "hr";
        return hours + "hr " + mins + "m";
    }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public static String formatRelativeTime(long timestamp) {
        if (timestamp <= 0) return "Unknown";

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 0) return "In the future";

        long seconds = diff / 1000;
        if (seconds < 60) return "Just now";

        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return hours == 1 ? "1 hour ago" : hours + " hours ago";
        }

        long days = hours / 24;
        if (days < 7) {
            return days == 1 ? "1 day ago" : days + " days ago";
        }

        long weeks = days / 7;
        if (weeks < 4) {
            return weeks == 1 ? "1 week ago" : weeks + " weeks ago";
        }

        long months = days / 30;
        if (months < 12) {
            return months == 1 ? "1 month ago" : months + " months ago";
        }

        long years = days / 365;
        return years == 1 ? "1 year ago" : years + " years ago";
    }
}


