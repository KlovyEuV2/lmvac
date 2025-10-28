package dev.lmv.lmvac.api.implement.ai.data;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AimData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String playerName;
    private final UUID playerUUID;
    private final List<AimSnapshot> snapshots;
    private long sessionStart;
    private boolean isCheat;

    public AimData(Player player, boolean isCheat) {
        this.playerName = player.getName();
        this.playerUUID = player.getUniqueId();
        this.snapshots = new ArrayList<>();
        this.sessionStart = System.currentTimeMillis();
        this.isCheat = isCheat;
    }

    public void addSnapshot(Player player, Player target, long timeSinceLastSnapshot) {
        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();

        Vector direction = targetLoc.toVector().subtract(playerLoc.toVector()).normalize();
        double yawToTarget = Math.toDegrees(Math.atan2(direction.getZ(), direction.getX())) - 90;
        double pitchToTarget = -Math.toDegrees(Math.asin(direction.getY()));

        float currentYaw = normalizeYaw(playerLoc.getYaw());
        float currentPitch = playerLoc.getPitch();
        yawToTarget = normalizeYaw((float) yawToTarget);

        float yawDiff = normalizeAngle((float) yawToTarget - currentYaw);
        float pitchDiff = (float) pitchToTarget - currentPitch;

        float yawSpeed = 0;
        float pitchSpeed = 0;

        if (!snapshots.isEmpty()) {
            AimSnapshot last = snapshots.get(snapshots.size() - 1);
            long timeDiff = System.currentTimeMillis() - last.getTimestamp();
            if (timeDiff > 0) {
                yawSpeed = normalizeAngle(currentYaw - last.getYaw()) / (timeDiff / 1000.0f);
                pitchSpeed = (currentPitch - last.getPitch()) / (timeDiff / 1000.0f);
            }
        }

        double distance = playerLoc.distance(targetLoc);

        AimSnapshot snapshot = new AimSnapshot(
                currentYaw, currentPitch, yawDiff, pitchDiff,
                yawSpeed, pitchSpeed, distance, timeSinceLastSnapshot,
                player.isSprinting(), player.isSneaking(),
                System.currentTimeMillis() - sessionStart
        );

        snapshots.add(snapshot);
    }

    private float normalizeYaw(float yaw) {
        yaw %= 360;
        if (yaw < 0) yaw += 360;
        return yaw;
    }

    private float normalizeAngle(float angle) {
        angle %= 360;
        if (angle > 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }

    public String getPlayerName() {
        return playerName;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public List<AimSnapshot> getSnapshots() {
        return snapshots;
    }

    public boolean isCheat() {
        return isCheat;
    }

    public void setCheat(boolean cheat) {
        isCheat = cheat;
    }

    public int getSnapshotCount() {
        return snapshots.size();
    }

    public long getSessionDuration() {
        return System.currentTimeMillis() - sessionStart;
    }

    public static class AimSnapshot implements Serializable {
        private static final long serialVersionUID = 1L;

        private final float yaw;
        private final float pitch;
        private final float yawDiff;
        private final float pitchDiff;
        private final float yawSpeed;
        private final float pitchSpeed;
        private final double distance;
        private final long timeSinceLastSnapshot;
        private final boolean sprinting;
        private final boolean sneaking;
        private final long timestamp;

        public AimSnapshot(float yaw, float pitch, float yawDiff, float pitchDiff,
                           float yawSpeed, float pitchSpeed, double distance,
                           long timeSinceLastSnapshot, boolean sprinting, boolean sneaking,
                           long timestamp) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.yawDiff = yawDiff;
            this.pitchDiff = pitchDiff;
            this.yawSpeed = yawSpeed;
            this.pitchSpeed = pitchSpeed;
            this.distance = distance;
            this.timeSinceLastSnapshot = timeSinceLastSnapshot;
            this.sprinting = sprinting;
            this.sneaking = sneaking;
            this.timestamp = timestamp;
        }

        public float getYaw() {
            return yaw;
        }

        public float getPitch() {
            return pitch;
        }

        public float getYawDiff() {
            return yawDiff;
        }

        public float getPitchDiff() {
            return pitchDiff;
        }

        public float getYawSpeed() {
            return yawSpeed;
        }

        public float getPitchSpeed() {
            return pitchSpeed;
        }

        public double getDistance() {
            return distance;
        }

        public long getTimeSinceLastSnapshot() {
            return timeSinceLastSnapshot;
        }

        public boolean isSprinting() {
            return sprinting;
        }

        public boolean isSneaking() {
            return sneaking;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public double[] toFeatures() {
            return new double[] {
                    distance,
                    Math.abs(yawDiff),
                    Math.abs(pitchDiff),
                    timeSinceLastSnapshot,
                    Math.abs(yawSpeed),
                    Math.abs(pitchSpeed),
                    sprinting ? 1.0 : 0.0,
                    sneaking ? 1.0 : 0.0,
                    Math.abs(yawSpeed - pitchSpeed),
                    Math.abs(yawDiff * pitchDiff),
                    Math.abs(yawSpeed / (timeSinceLastSnapshot > 0 ? timeSinceLastSnapshot : 1)),
                    Math.abs(pitchSpeed / (timeSinceLastSnapshot > 0 ? timeSinceLastSnapshot : 1)),
                    Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff),
                    Math.sqrt(yawSpeed * yawSpeed + pitchSpeed * pitchSpeed)
            };
        }
    }
}