package dev.lmv.lmvac.api.modules.checks.elytra;

import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.other.DoubleBuffer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.DescType;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.BukkitCheck;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.Configurable;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SettingCheck(
        value = "ElytraA",
        cooldown = Cooldown.COOLDOWN,
        descType = DescType.NEW
)
public class ElytraA extends Check implements BukkitCheck, Configurable {
    private final ConcurrentHashMap<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private DoubleBuffer buffer;
    private double baseMaxSpeed;
    private double speedMultiplierFactor;

    public void reloadConfiguration(Plugin plugin) {
        FileConfiguration config = plugin.getConfig();

        ConfigurationSection bufferSection = config.getConfigurationSection("buffer.elytraa");
        if (bufferSection == null) {
            buffer = new DoubleBuffer(false, 3.0, 1.0, 0.1);
        } else {
            buffer = new DoubleBuffer(
                    bufferSection.getBoolean("enabled", false),
                    bufferSection.getDouble("threshold", 3.0),
                    bufferSection.getDouble("increment", 1.0),
                    bufferSection.getDouble("decrement", 0.1)
            );
        }

        ConfigurationSection checkSection = config.getConfigurationSection("checks.elytra.a");
        if (checkSection == null) {
            baseMaxSpeed = 1.89;
            speedMultiplierFactor = 50.0;
        } else {
            baseMaxSpeed = checkSection.getDouble("baseMaxSpeed", 1.89);
            speedMultiplierFactor = checkSection.getDouble("speedMultiplierFactor", 50.0);
        }
    }

    public ElytraA(Plugin plugin) {
        super(plugin);
        reloadConfiguration(plugin);
    }

    private static class PlayerData {
        double lastPitch;

        PlayerData(double initialPitch) {
            this.lastPitch = initialPitch;
        }
    }

    @EventHandler
    private void check(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        LmvPlayer client = LmvPlayer.get(player);

        if (client == null || client.hasBypass(getName()) || !player.isGliding() ||
                player.isInsideVehicle() || !client.isGliding || !client.wasGliding) {
            return;
        }

        handleEssentials(event, player);
    }

    private void handleEssentials(PlayerMoveEvent event, Player player) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }

        Vector velocity = to.toVector().subtract(from.toVector());
        double horizontalDistance = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());

        double pitch = Math.toDegrees(player.getLocation().getDirection().getY());
        PlayerData data = playerDataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerData(pitch));

        double lastPitch = data.lastPitch;
        data.lastPitch = pitch;
        double deltaPitch = pitch - lastPitch;

        boolean isNearHorizontal = lastPitch > -30.0;
        boolean isSharpDive = deltaPitch < -15.0;
        boolean isAngleSuspicious = isNearHorizontal && isSharpDive;

        double speedMultiplier = pitch < 0 ? 1.0 - (pitch / speedMultiplierFactor) : 1.0;
        double dynamicMaxSpeed = baseMaxSpeed * speedMultiplier;

        if (horizontalDistance > dynamicMaxSpeed && !isAngleSuspicious) {
            boolean shouldFlag = false;

            if (buffer.enabled) {
                buffer.addViolation(player);
                shouldFlag = buffer.getBuffer(player) >= buffer.threshold;
            } else {
                shouldFlag = true;
            }

            if (shouldFlag) {
                double scale = dynamicMaxSpeed / horizontalDistance;
                Vector newVelocity = new Vector(
                        velocity.getX() * scale,
                        velocity.getY(),
                        velocity.getZ() * scale
                );

                player.setVelocity(newVelocity);
                event.setTo(from.clone().add(newVelocity));

                int ping = 0;
                try {
                    Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
                    ping = (int) entityPlayer.getClass().getField("ping").get(entityPlayer);
                } catch (Exception e) {
                    ping = -1;
                }

                this.flag(player, String.format("Speed: %.2f/%.2f, Ping: %d, Buffer: %.2f", horizontalDistance, dynamicMaxSpeed, ping, buffer.getBuffer(player)));
            }
        } else if (buffer.enabled) {
            buffer.reduceBuffer(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerDataMap.remove(event.getPlayer().getUniqueId());
    }
}