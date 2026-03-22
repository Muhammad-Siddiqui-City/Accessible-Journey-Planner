package com.example.ajp.utils;

/**
 * Duration display formatting. Add in Commit 5 (or with first UI that shows durations).
 * PURPOSE: One place for "Xm" / "1hr" / "1hr Xm" so 59m+ show as hour+min (e.g. 74m -> "1hr 14m").
 * WHY: Used in ArrivalsAdapter, RouteAdapter, RouteDetailsActivity, StepsAdapter, Analytics, share text.
 * ISSUES: AnalyticsViewModel must import TimeFormatUtil (missing import caused compile error once).
 */
public final class TimeFormatUtil {

    private TimeFormatUtil() { }

    /* --- BLOCK: formatMinutesToHourMin ---
     * PURPOSE: <60 -> "Xm"; 60 -> "1hr"; >60 -> "Nhr Xm" (e.g. 74 -> "1hr 14m").
     * WHY: User requested "1hr 14m" instead of "74m" for readability.
     * ISSUES: None.
     */
    public static String formatMinutesToHourMin(int totalMinutes) {
        if (totalMinutes < 0) totalMinutes = 0;
        if (totalMinutes < 60) return totalMinutes + "m";
        if (totalMinutes == 60) return "1hr";
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        if (mins == 0) return hours + "hr";
        return hours + "hr " + mins + "m";
    }

    /* --- BLOCK: formatRelativeTime ---
     * PURPOSE: Convert timestamp (milliseconds since epoch) to relative time string (e.g. "2 hours ago", "3 days ago").
     * WHY: Used in SavedRouteAdapter to show when a route was saved.
     * ISSUES: None.
     */
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
