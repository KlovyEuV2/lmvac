package dev.lmv.lmvac.api.implement.ai.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import dev.lmv.lmvac.LmvAC;
import dev.lmv.lmvac.api.implement.ai.AimAIDetector;
import dev.lmv.lmvac.api.implement.ai.data.AimData;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import dev.lmv.lmvac.api.modules.checks.aim.AimA;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SettingCheck(value = "AimML", cooldown = Cooldown.COOLDOWN)
public class AimPacketListener extends Check implements PacketCheck {
    private static LmvAC plugin;
    private final Map<UUID, Long> lastSnapshotTime;
    public static Double minMovement = 0.0001;
    public static Long per = 5L;
    public static Double maxDistance = 6.0;
    public static Double lookingThreshold = 0.8;

    public static Integer minSnapshots = 50;

    public static ConcurrentHashMap<UUID, Long> lastCheck = new ConcurrentHashMap<>();

    public AimPacketListener(LmvAC plugin) {
        super(plugin);
        AimPacketListener.plugin = plugin;
        this.lastSnapshotTime = new HashMap<>();
        reload();

//        new BukkitRunnable() {
//            @Override
//            public void run() {
//                monitorPlayers();
//            }
//        }.runTaskTimer(plugin, per, per);
    }

    public static void reloadCfg() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("aimML");
        try {
            per = section.getLong("per-ticks",5);
            minMovement = section.getDouble("min-diffs",0.0001);
            maxDistance = section.getDouble("target.max-distance",6.0);
            lookingThreshold = section.getDouble("target.looking-threshold",0.8);

            minSnapshots = section.getInt("min-snapshots",50);
        } catch (Exception e) {
            per = 5L;
            minMovement = 0.0001;
            maxDistance = 6.0;
            lookingThreshold = 0.8;
            minSnapshots = 50;
        }
    }

    private void monitorPlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            if (LmvAC.getAimDataCollector().recordingSessions.containsKey(uuid)) {
                continue;
            }

            try {
                long currentTime = System.currentTimeMillis();
                Long lastTime = lastSnapshotTime.get(uuid);
                if (lastTime == null || currentTime - lastTime > (per*50)) {
                    lastSnapshotTime.put(uuid, currentTime);

                    Player target = findTarget(player);
                    if (target == null) continue;

                    long timeSinceLastSnapshot = lastTime != null ? currentTime - lastTime : 0;
                    AimData.AimSnapshot snapshot = createSnapshot(player, target, timeSinceLastSnapshot);
                    if (snapshot.getPitchDiff() < minMovement && snapshot.getYawDiff() < minMovement && snapshot.getPitchSpeed() < minMovement) return;

                    LmvAC.getAimAIDetector().addSnapshotForAnalysis(player, snapshot, true);
                }
            } catch (Exception ignored) {}
        }
    }

    private void monitorPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        if (LmvAC.getAimDataCollector().recordingSessions.containsKey(uuid)) {
            return;
        }

        try {
            long currentTime = System.currentTimeMillis();
            Long lastTime = lastSnapshotTime.get(uuid);
            if (lastTime == null || currentTime - lastTime > (per*50)) {
                lastSnapshotTime.put(uuid, currentTime);

                Player target = findTarget(player);
                if (target == null) return;

                long timeSinceLastSnapshot = lastTime != null ? currentTime - lastTime : 0;
                AimData.AimSnapshot snapshot = createSnapshot(player, target, timeSinceLastSnapshot);
                if (snapshot.getPitchDiff() < minMovement && snapshot.getYawDiff() < minMovement && snapshot.getPitchSpeed() < minMovement) return;

                LmvAC.getAimAIDetector().addSnapshotForAnalysis(player, snapshot, true);
            }
        } catch (Exception ignored) {}
    }

    private Player findTarget(Player player) {
        Player closestTarget = null;
        double closestDistance = Double.MAX_VALUE;

        for (Player target : plugin.getServer().getOnlinePlayers()) {
            try {
                if (target.equals(player)) continue;
                if (player.getWorld() != target.getWorld()) continue;

                if (isLookingAt(player, target)) {
                    double distance = player.getLocation().distance(target.getLocation());
                    if (distance < closestDistance && distance < maxDistance) {
                        closestDistance = distance;
                        closestTarget = target;
                    }
                }
            } catch (Exception ignored) {}
        }

        return closestTarget;
    }

    private boolean isLookingAt(Player player, Player target) {
        Location eye = player.getEyeLocation();
        Vector toTarget = target.getEyeLocation().toVector().subtract(eye.toVector()).normalize();
        Vector playerDirection = eye.getDirection();

        return toTarget.dot(playerDirection) > lookingThreshold;
    }

    private AimData.AimSnapshot createSnapshot(Player player, Player target, long timeSinceLastSnapshot) {
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

        UUID uuid = player.getUniqueId();
        Long lastTime = lastSnapshotTime.get(uuid);
        if (lastTime != null && timeSinceLastSnapshot > 0) {
            yawSpeed = Math.abs(yawDiff) / (timeSinceLastSnapshot / 1000.0f);
            pitchSpeed = Math.abs(pitchDiff) / (timeSinceLastSnapshot / 1000.0f);
        }

        double distance = playerLoc.distance(targetLoc);

        return new AimData.AimSnapshot(
                currentYaw, currentPitch, yawDiff, pitchDiff,
                yawSpeed, pitchSpeed, distance, timeSinceLastSnapshot,
                player.isSprinting(), player.isSneaking(),
                System.currentTimeMillis()
        );
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

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        Player player = packetEvent.getPlayer();
        PacketType packetType = packetEvent.getPacketType();
        long now = System.currentTimeMillis();
        if (packetType == PacketType.Play.Client.POSITION_LOOK
                || packetType == PacketType.Play.Client.LOOK) {
            long from = lastCheck.getOrDefault(player.getUniqueId(),0L);
            if (now-from>=(per*50)) {
                monitorPlayer(player);
                lastCheck.put(player.getUniqueId(),now);
            }
        }
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().types(PacketType.Play.Client.POSITION_LOOK, PacketType.Play.Client.LOOK).build();
    }
}