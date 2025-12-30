package dev.lmv.lmvac.api.implement.utils.math;

import org.bukkit.Location;

import java.util.List;

public class MathUtil {
    public static double distanceXZ(double x1, double x2, double z1, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dz * dz);
    }
    public static double averageDelta(List<Location> values) {
        double sum = 0;
        for (int i = 1; i < values.size(); i++) {
            double prevXZ = getXZ(values.get(i - 1));
            double currXZ = getXZ(values.get(i));
            sum += Math.abs(currXZ - prevXZ);
        }
        return sum / (values.size() - 1);
    }
    public static double getXZ(Location loc) {
        double x = loc.getX();
        double z = loc.getZ();
        return Math.sqrt(x * x + z * z);
    }
}
