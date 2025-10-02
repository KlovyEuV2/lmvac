package dev.lmv.lmvac.api.implement.utils;

public class TimeUtil {
    public static String formatDuration(long millis) {
        long ms = millis % 1000;
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = millis / (1000 * 60 * 60);

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("ч ");
        if (minutes > 0) sb.append(minutes).append("м ");
        if (seconds > 0) sb.append(seconds).append("с ");
        sb.append(ms).append("мс");

        return sb.toString().trim();
    }

}
