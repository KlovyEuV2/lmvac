package dev.lmv.lmvac.api.implement.ai.data;

import dev.lmv.lmvac.LmvAC;
import dev.lmv.lmvac.api.implement.ai.listener.AimPacketListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AimDataCollector {
    private final LmvAC plugin;
    public final Map<UUID, AimData> recordingSessions;
    private final File dataFolder;
    private int completedSessionsCount;

    public AimDataCollector(LmvAC plugin) {
        this.plugin = plugin;
        this.recordingSessions = new HashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "AI/aim_data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.completedSessionsCount = countExistingDataFiles();

        new BukkitRunnable() {
            @Override
            public void run() {
                collectSnapshots();
            }
        }.runTaskTimer(plugin, AimPacketListener.per, AimPacketListener.per);
    }

    public void startRecording(Player player, boolean isCheat) {
        UUID uuid = player.getUniqueId();
        if (recordingSessions.containsKey(uuid)) {
            player.sendMessage("§c[LMVAC] Вы уже записываете сессию!");
            return;
        }

        recordingSessions.put(uuid, new AimData(player, isCheat));
        player.sendMessage("§a[LMVAC] Начата запись сессии. Цельтесь на игроков для сбора данных.");
        player.sendMessage("§7Используйте /lmvai stop для остановки записи.");
    }

    public void stopRecording(Player player) {
        UUID uuid = player.getUniqueId();
        if (!recordingSessions.containsKey(uuid)) {
            player.sendMessage("§c[LMVAC] У вас нет активной записи!");
            return;
        }

        AimData data = recordingSessions.remove(uuid);
        if (data.getSnapshotCount() < AimPacketListener.minSnapshots) {
            player.sendMessage("§c[LMVAC] Слишком мало данных для сохранения! Нужно минимум "+AimPacketListener.minSnapshots+" снапшотов.");
            return;
        }

        String fileName = (data.isCheat() ? "cheat_" : "legit_") +
                System.currentTimeMillis() + ".aim";
        File file = new File(dataFolder, fileName);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(data);
            completedSessionsCount++;
            player.sendMessage("§a[LMVAC] Сессия сохранена! (" + data.getSnapshotCount() + " снапшотов)");
        } catch (IOException e) {
            player.sendMessage("§c[LMVAC] Ошибка сохранения данных!");
            e.printStackTrace();
        }
    }

    private void collectSnapshots() {
        for (Map.Entry<UUID, AimData> entry : recordingSessions.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                recordingSessions.remove(entry.getKey());
                continue;
            }

            Player target = findTarget(player);
            if (target == null) continue;

            AimData data = entry.getValue();
            long timeSinceLastSnapshot = 0;

            if (!data.getSnapshots().isEmpty()) {
                timeSinceLastSnapshot = System.currentTimeMillis() -
                        data.getSnapshots().get(data.getSnapshots().size() - 1).getTimestamp();
            }

            data.addSnapshot(player, target, timeSinceLastSnapshot);
        }
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
                    if (distance < closestDistance) {
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

        return toTarget.dot(playerDirection) > AimPacketListener.lookingThreshold;
    }

    public int getCompletedSessionsCount() {
        return completedSessionsCount;
    }

    private int countExistingDataFiles() {
        if (!dataFolder.exists()) return 0;
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".aim"));
        return files != null ? files.length : 0;
    }
}