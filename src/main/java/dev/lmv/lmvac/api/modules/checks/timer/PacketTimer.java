package dev.lmv.lmvac.api.modules.checks.timer;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Client;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.LmvAC;
import dev.lmv.lmvac.api.implement.api.Geyser;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.BukkitCheck;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

// Таймер/FakeLag/Blink, и тд.
@SettingCheck(
        value = "PacketTimer",
        cooldown = Cooldown.COOLDOWN
)
public class PacketTimer extends Check implements BukkitCheck, PacketCheck {
    private final ConcurrentHashMap<UUID, Integer> currentSecondPackets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Deque<Integer>> packetHistory = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastUpdateTime = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, List<Long>> suspends = new ConcurrentHashMap<>();
    private static final double MIN_AVERAGE_PACKETS = 2.18;
    private static final double MOVEMENT_THRESHOLD = 0.2;
    private static final double MIN_AVERAGE_SPEED = 0.05;
    private static final int WINDOW_SIZE = 10;
    private static final long WINDOW_UPDATE_TICKS = 2L;

    private BukkitTask timerTask;
    private volatile boolean isShutdown = false;

    public PacketTimer(Plugin plugin) {
        super(plugin);
        this.startTimerTask(plugin);
    }

    private void startTimerTask(Plugin plugin) {
        this.timerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (this.isShutdown || !this.isEnabled()) {
                return;
            }

            try {
                for (UUID uuid : this.currentSecondPackets.keySet()) {
                    if (this.isShutdown) {
                        break;
                    }

                    double plus = 2.18;
                    int packetsThisWindow = this.currentSecondPackets.getOrDefault(uuid, 0);
                    this.packetHistory.putIfAbsent(uuid, new ArrayDeque<>());

                    List<Long> playerSuspends = suspends.computeIfAbsent(uuid, k -> new ArrayList<>());
                    playerSuspends.removeIf(k -> System.currentTimeMillis() - k > 2350L);

                    Deque<Integer> history = this.packetHistory.get(uuid);
                    if (history.size() >= 10) {
                        history.pollFirst();
                    }

                    history.offerLast(packetsThisWindow);
                    double avgPerWindow = history.stream().mapToInt(i -> i).average().orElse(0.0);
                    double avgPacketsPerSecond = avgPerWindow * 7.5;
                    Player player = Bukkit.getPlayer(uuid);

                    if (player != null && player.isOnline() && !player.isDead()) {
                        if (player.isFlying() || player.isGliding()) {
                            plus += 0.63;
                        }

                        if (this.hasMovedEnough(player) && avgPacketsPerSecond > 20.0 + plus && avgPacketsPerSecond > 0.0) {
                            int id = player.getEntityId();
                            LmvPlayer targetPlayer = LmvPlayer.players.get(id);
                            if (targetPlayer == null) {
                                return;
                            }

                            if (targetPlayer.hasBypass(this.getName())) {
                                return;
                            }

                            playerSuspends.add(System.currentTimeMillis());
                            if (Bukkit.getServer().getTPS()[0] > 18.6) {
                                if (!this.isShutdown && plugin.isEnabled()) {
                                    Bukkit.getScheduler().runTask(LmvAC.getInstance(), () -> {
                                        if (!this.isShutdown) {
                                            this.flag(player);
                                        }
                                    });
                                }
                            }
                        }
                    }
                }

                if (!this.isShutdown) {
                    this.currentSecondPackets.clear();
                }
            } catch (Exception ignored) {
            }
        }, 2L, 2L);
    }

    public void shutdown() {
        this.isShutdown = true;

        if (this.timerTask != null && !this.timerTask.isCancelled()) {
            this.timerTask.cancel();
        }

        this.currentSecondPackets.clear();
        this.packetHistory.clear();
        this.lastLocations.clear();
        this.lastUpdateTime.clear();
        suspends.clear();
    }

    public void restart() {
        this.shutdown();
        this.isShutdown = false;
        this.startTimerTask(this.getPlugin());
    }

    public void setEnabled(boolean enabled) {
        isShutdown = !enabled;

        if (!enabled) {
            if (this.timerTask != null && !this.timerTask.isCancelled()) {
                this.timerTask.cancel();
            }
        } else {
            if (this.timerTask == null || this.timerTask.isCancelled()) {
                this.startTimerTask(this.getPlugin());
            }
        }
    }

    public void onPacketReceiving(PacketEvent event) {
        if (this.isShutdown || !Geyser.isBedrockPlayer(event.getPlayer().getUniqueId())) {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            if (player.isOnline() && !player.isDead()) {
                PacketType type = event.getPacketType();
                if (this.isMovementPacket(type)) {
                    this.currentSecondPackets.put(uuid, this.currentSecondPackets.getOrDefault(uuid, 0) + 1);
                    this.updateLastLocation(player);
                }

                if (type == Client.POSITION) {
                    List<Long> list = suspends.get(uuid);
                    if (list == null) {
                        return;
                    }

                    boolean shouldCancel = true;
                    int maxSize = 10;
                    if (shouldCancel && list.size() > maxSize) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    private boolean isMovementPacket(PacketType type) {
        return type == Client.POSITION;
    }

    private void updateLastLocation(Player player) {
        if (this.isShutdown) return;

        UUID uuid = player.getUniqueId();
        Location current = player.getLocation();
        this.lastLocations.put(uuid, current.clone());
        this.lastUpdateTime.put(uuid, System.currentTimeMillis());
    }

    private boolean hasMovedEnough(Player player) {
        if (this.isShutdown) return false;

        UUID uuid = player.getUniqueId();
        Location current = player.getLocation();
        Location previous = this.lastLocations.get(uuid);
        Long lastTime = this.lastUpdateTime.get(uuid);
        if (previous != null && lastTime != null && current.getWorld().equals(previous.getWorld())) {
            long now = System.currentTimeMillis();
            long deltaTime = now - lastTime;
            if (deltaTime <= 0L) {
                return false;
            } else {
                double dx = current.getX() - previous.getX();
                double dy = current.getY() - previous.getY();
                double dz = current.getZ() - previous.getZ();
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double seconds = (double)deltaTime / 1850.0;
                double speed = distance / seconds;
                return distance > 0.2 && speed > 0.05;
            }
        } else {
            return false;
        }
    }

    public void onPacketSending(PacketEvent event) {
    }

    public ListeningWhitelist getSendingWhitelist() {
        return ListeningWhitelist.newBuilder().build();
    }

    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().types(new PacketType[]{Client.POSITION}).build();
    }

    public Plugin getPlugin() {
        return LmvAC.getInstance();
    }
}