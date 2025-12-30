package dev.lmv.lmvac.api.modules.checks.sprint;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.lmv.lmvac.LmvAC;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.other.DoubleBuffer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.Configurable;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import dev.lmv.lmvac.api.implement.utils.FluidUtil;
import dev.lmv.lmvac.api.implement.utils.IceUtil;
import dev.lmv.lmvac.api.implement.utils.listeners.MovementListener;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

// Детект сброса спринта
@SettingCheck(
        value = "SprintB",
        cooldown = Cooldown.COOLDOWN
)
public class SprintB extends Check implements PacketCheck, Configurable {

    public DoubleBuffer buffer;

    public void reloadConfiguration(Plugin plugin) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection bufferSection = config.getConfigurationSection("buffer.sprintb");

        boolean enabled;
        double threshold;
        double increment;
        double decrement;
        try {
            enabled = bufferSection.getBoolean("enabled", true);
            threshold = bufferSection.getDouble("threshold", 6.0);
            increment = bufferSection.getDouble("increment", 1.0);
            decrement = bufferSection.getDouble("decrement", 0.5);
        } catch (NullPointerException ex) {
            enabled = true;
            threshold = 6.0;
            increment = 1.0;
            decrement = 0.5;
        }

        buffer = new DoubleBuffer(enabled, threshold, increment, decrement);
    }

    public SprintB(Plugin plugin) {
        super(plugin);
        reloadConfiguration(plugin);
    }

    public void onPacketReceiving(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        Player player = event.getPlayer();
        PacketType packetType = event.getPacketType();

        int id = event.getPlayer().getEntityId();
        LmvPlayer client = LmvPlayer.players.get(id);

        if (client == null) return;
        if (packetType == PacketType.Play.Client.USE_ENTITY) {
            try {
                EnumWrappers.EntityUseAction action = packet.getEntityUseActions().readSafely(0);
                if (action == EnumWrappers.EntityUseAction.ATTACK) {
                    if (!FluidUtil.isInFluid(player) && !IceUtil.isOnIce(player)) {
                        if (isCritReady(player)) {
                            this.handleHardSimulation(event);
                        }
                    } else {
                        if (buffer.enabled) buffer.reduceBuffer(player);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().types(new PacketType[]{PacketType.Play.Client.USE_ENTITY}).build();
    }

    private void handleHardSimulation(PacketEvent event) {
        Player player = event.getPlayer();
        if (!FluidUtil.isInFluid(player)) {
            if (player.isSprinting() || player.isInsideVehicle()) {
                if (buffer.enabled) buffer.reduceBuffer(player);
                return;
            }

            Bukkit.getScheduler().runTaskLater(LmvAC.getInstance(), () -> {
                if (!player.isOnline()) return;
                if (player.isSprinting() && !event.isCancelled() && isSpeedSimulation(player) && !player.isJumping()) {
                    if (buffer.enabled) {
                        buffer.addViolation(player);
                        if (buffer.getBuffer(player) >= buffer.threshold) {
                            this.flag(player, "suspend cheat-simulation sprint-reset. buffer: " + String.format("%.2f", buffer.getBuffer(player)));
                        }
                    } else {
                        this.flag(player, "suspend cheat-simulation sprint-reset. ");
                    }
                } else if (buffer.enabled) {
                    buffer.reduceBuffer(player);
                }

            }, hasPingTimeOut(player));
        } else if (buffer.enabled) {
            buffer.reduceBuffer(player);
        }
    }

    private static boolean isCritReady(Player player) {
        return player.getAttackCooldown() >= 0.9F;
    }

    private static boolean isSpeedSimulation(Player player) {
        List<Location> locs = MovementListener.lastLocations.getOrDefault(player.getUniqueId(), new ArrayList<>());
        Location from = (Location)locs.get(locs.size() - 2);
        Location to = player.getLocation();
        return Math.abs(from.getX() - to.getX()) > 0.21 || Math.abs(from.getZ() - to.getZ()) > 0.21;
    }

    public static long hasPingTimeOut(Player player) {
        int max = 2;
        return (long)Math.max(0, Math.min(player.getPing() / 50, max));
    }
}