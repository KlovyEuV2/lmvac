package dev.lmv.lmvac.api.modules.checks.timer;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Client;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.LmvAC;
import dev.lmv.lmvac.api.implement.api.Geyser;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

// Негатив таймер
@SettingCheck(
        value = "NegativeTimer",
        cooldown = Cooldown.COOLDOWN
)
public class NegativeTimer extends Check implements PacketCheck {
    private final ConcurrentHashMap<UUID, Integer> currentSecondPackets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Deque<Integer>> packetHistory = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastUpdateTime = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, List<Long>> suspends = new ConcurrentHashMap<>();
    private static final double MOVEMENT_THRESHOLD = 0.2;
    private static final double MIN_AVERAGE_SPEED = 0.05;
    private final ConcurrentHashMap<UUID,Long> movementStartTime = new ConcurrentHashMap();
    private static final long MOVEMENT_GRACE_PERIOD_MS = 850L;
    private static final int WINDOW_SIZE = 10;
    private static final long WINDOW_UPDATE_TICKS = 2L;
    public static final ConcurrentHashMap<UUID, List<Long>> suspendsT = new ConcurrentHashMap();

    private BukkitTask timerTask;
    private volatile boolean isShutdown = false;

    public NegativeTimer(Plugin plugin) {
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

                    double minus = 0.86;
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
                    double avgPacketsPerSecond = avgPerWindow * 10.0;
                    Player player = Bukkit.getPlayer(uuid);

                    if (player != null && player.isOnline() && !player.isDead()) {
                        if (player.isFlying() || player.isGliding()) {
                            minus += 0.73;
                        }

                        if (this.hasMovedEnough(player) && avgPacketsPerSecond < Bukkit.getServer().getTPS()[0] - minus && avgPacketsPerSecond > 0.0) {
                            List<Long> playerSuspendsT = suspendsT.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
                            playerSuspendsT.removeIf(k -> System.currentTimeMillis() - k > 850L);
                            playerSuspendsT.add(System.currentTimeMillis());

                            if (playerSuspends.size() < 3) {
                                return;
                            }

                            playerSuspends.add(System.currentTimeMillis());

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
        this.movementStartTime.clear();
        suspends.clear();
        suspendsT.clear();
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
        return type == Client.POSITION || type == Client.POSITION_LOOK || type == Client.LOOK;
    }

    private void updateLastLocation(Player player) {
        if (this.isShutdown) return;

        UUID uuid = player.getUniqueId();
        Location current = player.getLocation();
        Location previous = this.lastLocations.get(uuid);
        if (previous != null && current.getWorld().equals(previous.getWorld())) {
            double dx = current.getX() - previous.getX();
            double dy = current.getY() - previous.getY();
            double dz = current.getZ() - previous.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > 0.2) {
                if (!this.movementStartTime.containsKey(uuid)) {
                    this.movementStartTime.put(uuid, System.currentTimeMillis());
                }
            } else {
                this.movementStartTime.remove(uuid);
            }
        } else {
            this.movementStartTime.put(uuid, System.currentTimeMillis());
        }

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
            Long movementStart = this.movementStartTime.get(uuid);
            if (movementStart == null) {
                return false;
            } else {
                long now = System.currentTimeMillis();
                if (now - movementStart < 850L) {
                    return false;
                } else {
                    long deltaTime = now - lastTime;
                    if (deltaTime <= 0L) {
                        return false;
                    } else {
                        double dx = current.getX() - previous.getX();
                        double dy = current.getY() - previous.getY();
                        double dz = current.getZ() - previous.getZ();
                        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        double seconds = (double)deltaTime / 1000.0;
                        double speed = distance / seconds;
                        return distance > 0.2 && speed > 0.05;
                    }
                }
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
        return ListeningWhitelist.newBuilder().types(new PacketType[]{Client.POSITION, Client.POSITION_LOOK, Client.LOOK}).build();
    }

    public Plugin getPlugin() {
        return LmvAC.getInstance();
    }
}