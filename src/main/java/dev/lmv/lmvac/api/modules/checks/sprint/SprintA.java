/* Decompiler 67ms, total 1032ms, lines 103 */
package dev.lmv.lmvac.api.modules.checks.sprint;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Client;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.EntityUseAction;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.other.DoubleBuffer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.DescType;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.Configurable;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@SettingCheck(
        value = "SprintA",
        cooldown = Cooldown.COOLDOWN,
        descType = DescType.NEW
)
public class SprintA extends Check implements PacketCheck, Configurable {
    public double diff = 0.35D;
    public DoubleBuffer buffer;

    public void reloadConfiguration(Plugin plugin) {
        FileConfiguration config = plugin.getConfig();

        ConfigurationSection bufferSection = config.getConfigurationSection("buffer.sprinta");
        boolean enabled;
        double threshold;
        double increment;
        double decrement;

        if (bufferSection != null) {
            enabled = bufferSection.getBoolean("enabled", true);
            threshold = bufferSection.getDouble("threshold", 3.0D);
            increment = bufferSection.getDouble("increment", 1.0D);
            decrement = bufferSection.getDouble("decrement", 0.45D);
        } else {
            enabled = true;
            threshold = 3.0D;
            increment = 1.0D;
            decrement = 0.45D;
        }

        diff = config.getDouble("checks.sprint.a.diff", 0.35D);

        buffer = new DoubleBuffer(enabled, threshold, increment, decrement);
    }

    public SprintA(Plugin plugin) {
        super(plugin);
        reloadConfiguration(plugin);
    }

    public void onPacketReceiving(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        Player player = event.getPlayer();
        PacketType packetType = event.getPacketType();
        LmvPlayer client = LmvPlayer.get(player);
        long now = System.nanoTime();
        if (client != null) {
            if (packetType == Client.USE_ENTITY) {
                try {
                    EntityUseAction action = packet.getEntityUseActions().readSafely(0);
                    if (action == EntityUseAction.ATTACK && client.lastEntityAction > 0L) {
                        long tDiff = now - client.nanoEntityAction;
                        double msDiff = (double)tDiff / 1000000.0D;
                        long nDiff = (long)(diff * 1000000.0D);
                        if (tDiff <= nDiff) {
                            if (buffer.enabled) {
                                buffer.addViolation(player);
                                if (buffer.getBuffer(player) >= buffer.threshold) {
                                    event.setCancelled(true);
                                    String var10002 = String.format("%.3f", msDiff);
                                    this.flag(player, "fast sprinting-reset [" + var10002 + "ms.] buffer: " + String.format("%.2f", buffer.getBuffer(player)));
                                }
                            } else {
                                Object[] var10003 = new Object[]{msDiff};
                                this.flag(player, "fast sprinting-reset [" + String.format("%.3f", var10003) + "ms.] ");
                            }
                        } else if (buffer.enabled) {
                            buffer.reduceBuffer(player);
                        }
                    }
                } catch (Exception ignored) {
                }
            }

        }
    }

    private static boolean isCritReady(Player player) {
        return player.getAttackCooldown() >= 0.9F;
    }

    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().types(new PacketType[]{Client.USE_ENTITY}).build();
    }
}