package dev.lmv.lmvac.api.modules.checks.flight;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Client;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;

import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import dev.lmv.lmvac.api.implement.utils.FluidUtil;
import dev.lmv.lmvac.api.implement.utils.WebUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

/**
 * Флай чек на ProtocolLib пакетах (FlightC), дублирующий логику FlightA. Замечены возможные лаги!
 */
@SettingCheck(
        value = "FlightA", // New check name
        cooldown = Cooldown.COOLDOWN
)
public class FlightA extends Check implements PacketCheck {
    private final ConcurrentHashMap<UUID, List<Location>> lasts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> xzFlyViolations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastGroundTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Location> lastGroundLocation = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Location> lastFlagLocation = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastUpTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastElytraGlidingTime = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Location> lastPacketLocation = new ConcurrentHashMap<>();

    public FlightA(Plugin plugin) {
        super(plugin);
    }

    private boolean isClimbable(Block block) {
        Material type = block.getType();
        return type == Material.LADDER || type == Material.VINE || type == Material.SCAFFOLDING || type == Material.WEEPING_VINES || type == Material.TWISTING_VINES || type == Material.WEEPING_VINES_PLANT || type == Material.TWISTING_VINES_PLANT;
    }

    private boolean hasClimbableNearby(Location location) {
        for (int x = -1; x <= 1; ++x) {
            for (int z = -1; z <= 1; ++z) {
                for (int y = -1; y <= 2; ++y) {
                    Block block = location.clone().add((double) x, (double) y, (double) z).getBlock();
                    if (this.isClimbable(block)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isInLiquid(Location location) {
        Block block = location.getBlock();
        return block.getType() == Material.WATER || block.getType() == Material.LAVA || block.getType() == Material.BUBBLE_COLUMN;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PacketType packetType = event.getPacketType();

        if (packetType.equals(Client.POSITION) || packetType.equals(Client.POSITION_LOOK) || packetType.equals(Client.LOOK)) {
            PacketContainer packet = event.getPacket();

            LmvPlayer targetPlayer = LmvPlayer.players.get(player.getEntityId());
            if (targetPlayer == null || targetPlayer.hasBypass(getName())) {
                return;
            }

            double x = packet.getDoubles().read(0);
            double y = packet.getDoubles().read(1);
            double z = packet.getDoubles().read(2);
            boolean onGround = packet.getBooleans().read(0);
            long now = System.currentTimeMillis();

            Location to = packetType.equals(Client.LOOK) ? player.getLocation() : new Location(player.getWorld(), x, y, z);
            Location from = this.lastPacketLocation.getOrDefault(uuid, to.clone());
            if (packetType.equals(Client.LOOK)) {
                this.lastPacketLocation.put(uuid, to.clone());
                return;
            }
            if (player.isGliding() || player.isFlying()) {
                this.lastElytraGlidingTime.put(uuid, now);
            }

            Long lastElytraTime = this.lastElytraGlidingTime.get(uuid);
            if (lastElytraTime != null && now - lastElytraTime < 350L) {
                this.resetMovementTracking(uuid, player.getLocation(), now);
            } else if (!player.isFlying() && player.isTicking() && !player.isGliding() && !player.isInsideVehicle()
                    && !this.hasClimbableNearby(to) && !FluidUtil.isInFluid(player) && !WebUtil.isInWeb(player)
                    && !this.isInLiquid(to) && !this.isInLiquid(from)) {

                if (onGround) {
                    this.resetMovementTracking(uuid, to, now);
                } else {
                    double deltaY = to.getY() - from.getY();

                    if (!(deltaY > 0.08) && !(deltaY < -0.08)) {
                        this.lastUpTime.putIfAbsent(uuid, now);
                    } else {
                        this.lastUpTime.put(uuid, now);
                    }

                    Long groundTime = this.lastGroundTime.get(uuid);
                    Location groundLoc = this.lastGroundLocation.get(uuid);
                    Long lastUp = this.lastUpTime.get(uuid);

                    if (groundTime != null && groundLoc != null && lastUp != null) {
                        long timeSinceLastUp = now - lastUp;

                        boolean isIllegal = timeSinceLastUp > 435L && deltaY < 0.08;

                        if (isIllegal) {
                            if (this.checkVelocityEquivalent(player)) {
                                this.flagAndRollback(player, groundLoc, event);
                                return;
                            }

                            if (!this.lastFlagLocation.containsKey(uuid)) {
                                this.flagAndRollback(player, groundLoc, event);
                                this.lastFlagLocation.put(uuid, player.getLocation());
                                return;
                            }

                            if (this.lastFlagLocation.get(uuid).distance(player.getLocation()) > 1.38) {
                                this.flagAndRollback(player, groundLoc, event);
                                this.lastFlagLocation.put(uuid, player.getLocation());
                                return;
                            }
                        }
                    }

                    List<Location> locations = this.lasts.computeIfAbsent(uuid, k -> new ArrayList<>());
                    locations.add(to.clone());
                    if (locations.size() > 10) {
                        locations.remove(0);
                    }

                    if (locations.size() >= 2) {
                        this.checkXZFlight(player, from, to, uuid, event);
                        this.checkVerticalFlight(player, locations, to, from, event);
                    }
                }
            } else {
                this.lasts.remove(uuid);
                this.lastGroundTime.put(uuid, now);
                this.lastUpTime.remove(uuid);
                this.xzFlyViolations.remove(uuid);
            }

            this.lastPacketLocation.put(uuid, to.clone());
        }
    }

    private void resetMovementTracking(UUID uuid, Location location, long now) {
        this.lasts.remove(uuid);
        this.xzFlyViolations.remove(uuid);
        this.lastGroundTime.put(uuid, now);
        this.lastGroundLocation.put(uuid, location.clone());
        this.lastUpTime.put(uuid, now);
    }

    private void flagAndRollback(Player player, Location rollbackLoc, PacketEvent event) {
        this.flag(player);

        event.setCancelled(true);
        player.teleport(rollbackLoc);
    }

    private boolean checkVelocityEquivalent(Player player) {
        return true;
    }

    private void checkXZFlight(Player player, Location from, Location to, UUID uuid, PacketEvent event) {
        double deltaY = to.getY() - from.getY();
        double deltaXZ = Math.sqrt(Math.pow(to.getX() - from.getX(), 2.0) + Math.pow(to.getZ() - from.getZ(), 2.0));

        Long lastGroundTimeValue = this.lastGroundTime.getOrDefault(uuid, System.currentTimeMillis());
        long timeSinceGround = System.currentTimeMillis() - lastGroundTimeValue;

        if (deltaXZ > 0.1 && Math.abs(deltaY) < 0.05 && timeSinceGround > 1000L) {
            int violations = this.xzFlyViolations.getOrDefault(uuid, 0) + 1;
            this.xzFlyViolations.put(uuid, violations);

            if (violations >= 5) {
                Location lastGround = this.lastGroundLocation.get(uuid);

                this.flag(player);
                this.xzFlyViolations.put(uuid, violations - 2);

                if (lastGround != null && lastGround.getWorld().equals(player.getWorld())) {
                    event.setCancelled(true);
                } else {
                    event.setCancelled(true);
                }
            }
        } else {
            this.xzFlyViolations.put(uuid, Math.max(0, this.xzFlyViolations.getOrDefault(uuid, 0) - 1));
        }
    }

    private void checkVerticalFlight(Player player, List<Location> locations, Location to, Location from, PacketEvent event) {
        List<Double> distances = new ArrayList<>();

        for (int i = 1; i < locations.size(); ++i) {
            distances.add(locations.get(i).distance(locations.get(i - 1)));
        }

        UUID uuid = player.getUniqueId();
        if (distances.size() >= 2) {
            double deltaY = to.getY() - from.getY();
            int stableCount = 0;

            for (int i = 1; i < distances.size(); ++i) {
                if (Math.abs(distances.get(i) - distances.get(i - 1)) <= 0.01) {
                    ++stableCount;
                }
            }

            double stablePercentage = (double) stableCount / (double) distances.size() * 100.0;

            if (player.hasPotionEffect(PotionEffectType.LEVITATION)) {
                if (!player.isFlying() && player.getPotionEffect(PotionEffectType.LEVITATION) != null) {
                    double maxDeltaY = 0.93 * (double) player.getPotionEffect(PotionEffectType.LEVITATION).getAmplifier();
                    if (deltaY > maxDeltaY) {
                        event.setCancelled(true);
                        this.flag(player);
                    }
                }
            } else {
                if (stablePercentage >= 30.0 && to.getY() > from.getY()) {
                    Location lastGround = this.lastGroundLocation.get(uuid);

                    this.flag(player);

                    if (lastGround != null && lastGround.getWorld().equals(player.getWorld())) {
                        event.setCancelled(true);
                    } else {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().types(
                Client.POSITION,
                Client.POSITION_LOOK,
                Client.LOOK
        ).build();
    }
}