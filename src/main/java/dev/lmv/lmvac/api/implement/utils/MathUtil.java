package dev.lmv.lmvac.api.implement.utils;

import org.bukkit.Location;

public class MathUtil {
    public static double distanceXZ(Location loc1, Location loc2) {
        double dx = loc1.getX() - loc2.getX();
        double dz = loc1.getZ() - loc2.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
