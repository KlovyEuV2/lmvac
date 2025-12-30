package dev.lmv.lmvac.api.implement.utils;

import org.bukkit.Bukkit;

public class VersionUtil {
    public static boolean isVersionAtLeast(String version) {
        try {
            String serverVersion = Bukkit.getBukkitVersion().split("-")[0];
            return compareVersions(serverVersion, version) >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) return Integer.compare(p1, p2);
        }
        return 0;
    }
}
