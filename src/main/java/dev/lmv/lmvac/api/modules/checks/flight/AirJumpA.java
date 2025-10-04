package dev.lmv.lmvac.api.modules.checks.flight;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import dev.lmv.lmvac.api.implement.utils.FluidUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Замечены возможные лаги!
@SettingCheck(
        value = "AirJumpA",
        cooldown = Cooldown.COOLDOWN
)
public class AirJumpA extends Check implements PacketCheck {
    private final ConcurrentHashMap<UUID, Integer> violations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> wasOnGround = new ConcurrentHashMap<>();

    public AirJumpA(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PacketType packetType = event.getPacketType();

        LmvPlayer targetPlayer = LmvPlayer.players.get(player.getEntityId());
        if (targetPlayer == null || targetPlayer.hasBypass(getName())) {
            return;
        }

        if (player.isFlying() || player.isGliding() || player.isInsideVehicle() ||
                player.getAllowFlight() || !player.isTicking()) {
            violations.remove(uuid);
            wasOnGround.remove(uuid);
            return;
        }

        if (packetType.equals(PacketType.Play.Client.ENTITY_ACTION)) {
            PacketContainer packet = event.getPacket();
            int action = packet.getIntegers().read(1);

            if (action == 3) {
                Location loc = player.getLocation();
                boolean actuallyOnGround = isOnGround(loc, player);

                if (!actuallyOnGround && !wasOnGround.getOrDefault(uuid, true)) {
                    int vl = violations.getOrDefault(uuid, 0) + 1;
                    violations.put(uuid, vl);

                    if (vl >= 2) {
                        event.setCancelled(true);
                        flag(player);
                        violations.put(uuid, 0);
                    }
                }

                wasOnGround.put(uuid, actuallyOnGround);
            }
        }
        else if (packetType.equals(PacketType.Play.Client.POSITION) ||
                packetType.equals(PacketType.Play.Client.POSITION_LOOK)) {
            PacketContainer packet = event.getPacket();
            boolean packetOnGround = packet.getBooleans().read(0);

            double x = packet.getDoubles().read(0);
            double y = packet.getDoubles().read(1);
            double z = packet.getDoubles().read(2);
            Location loc = new Location(player.getWorld(), x, y, z);

            boolean actuallyOnGround = isOnGround(loc, player);

            if (actuallyOnGround) {
                wasOnGround.put(uuid, true);
                violations.put(uuid, Math.max(0, violations.getOrDefault(uuid, 0) - 1));
            } else {
                wasOnGround.put(uuid, false);
            }
        }
    }

    public static boolean isOnGround(Location from_, Player player) {
        for (double offset = 0; offset <= 0.1; offset += 0.01) {
            Location checkLoc = from_.clone().subtract(0, offset, 0);

            if (isSolidOrLiquid(checkLoc.getBlock(), player)) {
                return true;
            }

            for (double x = -0.3; x <= 0.3; x += 0.6) {
                for (double z = -0.3; z <= 0.3; z += 0.6) {
                    Location cornerLoc = checkLoc.clone().add(x, 0, z);
                    if (isSolidOrLiquid(cornerLoc.getBlock(), player)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isSolidOrLiquid(Block block, Player player) {
        Material type = block.getType();
        return type.isSolid() ||
                type == Material.WATER ||
                type == Material.LAVA ||
                type == Material.LADDER ||
                type == Material.VINE ||
                type == Material.SCAFFOLDING ||
                FluidUtil.isInFluid(player);
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().types(
                PacketType.Play.Client.ENTITY_ACTION,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK
        ).build();
    }
}