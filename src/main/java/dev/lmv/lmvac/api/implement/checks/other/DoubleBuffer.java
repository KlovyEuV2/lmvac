package dev.lmv.lmvac.api.implement.checks.other;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public class DoubleBuffer {
    public final boolean enabled;
    public final double threshold;
    public final double increment;
    public final double decrement;

    private final Map<UUID, Double> bufferMap = new HashMap<>();

    public DoubleBuffer(boolean enabled, double threshold, double increment, double decrement) {
        this.enabled = enabled;
        this.threshold = threshold;
        this.increment = increment;
        this.decrement = decrement;
    }

    public void addViolation(Player player) {
        if (!enabled) return;

        UUID uuid = player.getUniqueId();
        double currentBuffer = bufferMap.getOrDefault(uuid, 0.0);
        bufferMap.put(uuid, Math.min(currentBuffer + increment, threshold * 2));
    }

    public void reduceBuffer(Player player) {
        if (!enabled) return;

        UUID uuid = player.getUniqueId();
        double currentBuffer = bufferMap.getOrDefault(uuid, 0.0);
        bufferMap.put(uuid, Math.max(currentBuffer - decrement, 0.0));

        if (bufferMap.get(uuid) <= 0) {
            bufferMap.remove(uuid);
        }
    }

    public double getBuffer(Player player) {
        if (!enabled) return 0.0;

        UUID uuid = player.getUniqueId();
        return bufferMap.getOrDefault(uuid, 0.0);
    }

    public void resetBuffer(Player player) {
        bufferMap.remove(player.getUniqueId());
    }
}