package dev.lmv.lmvac.api.modules.checks.other;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.BukkitCheck;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Небольшой чек на SpiderA (но его изи обойти)...
@SettingCheck(
        value = "SpiderA",
        cooldown = Cooldown.COOLDOWN
)
public class SpiderA extends Check implements BukkitCheck, PacketCheck {
    private final ConcurrentHashMap<UUID, Double> velocityMap = new ConcurrentHashMap<>();
    public SpiderA(Plugin plugin) {
        super(plugin);
    }
    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        int id = player.getEntityId();
        LmvPlayer targetPlayer = (LmvPlayer) LmvPlayer.players.get(id);

        if (targetPlayer == null || targetPlayer.hasBypass(getName())) return;
        if (player.isInsideVehicle()) return;

        if (player.isOnGround() || player.isFlying() || player.isGliding()) {
            velocityMap.remove(player.getUniqueId());
            return;
        }

        double velocityStrength = velocityMap.getOrDefault(player.getUniqueId(), 0.0);
        velocityStrength = Math.min(velocityStrength, 2.0);

        double jumpBoostBonus = getJumpBoostBonus(player);

        double maxAllowedY = 3.0 + velocityStrength + jumpBoostBonus;

        if (targetPlayer.groundLocation == null) targetPlayer.groundLocation = player.getLocation();
        double yDiff = event.getTo().getY() - targetPlayer.groundLocation.getY();

        if (yDiff > maxAllowedY) {
            flag(player);
            player.teleport(targetPlayer.groundLocation);
        }
    }
    private double getJumpBoostBonus(Player player) {
        if (player.hasPotionEffect(PotionEffectType.JUMP)) {
            int amplifier = player.getPotionEffect(PotionEffectType.JUMP).getAmplifier();
            return (amplifier + 1) * 0.5;
        }
        return 0.0;
    }
    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        if (packetEvent.getPacketType() == PacketType.Play.Server.ENTITY_VELOCITY) {
            Player player = packetEvent.getPlayer();
            if (player == null) return;

            int velY = packetEvent.getPacket().getIntegers().read(2);
            double velocityY = velY / 8000.0;

            UUID uuid = player.getUniqueId();
            double current = velocityMap.getOrDefault(uuid, 0.0);
            velocityMap.put(uuid, current + velocityY);
        }
    }
    public class VelocityData {
        public final double strength;
        public final long timestamp;

        public VelocityData(double strength, long timestamp) {
            this.strength = strength;
            this.timestamp = timestamp;
        }
    }
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().types(new PacketType[]{PacketType.Play.Server.ENTITY_VELOCITY}).build();
    }
}
