package dev.lmv.lmvac.api.implement.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class ProtocolUtil {
    private static final AtomicInteger teleportIdCounter = new AtomicInteger(0);

    public static boolean teleport(Player player, Location location) {
        if (player == null || location == null) {
            return false;
        }

        if (!player.isOnline()) {
            return false;
        }

        try {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.POSITION);
            var modifier = packet.getModifier();

            modifier.write(0, location.getX());
            modifier.write(1, location.getY());
            modifier.write(2, location.getZ());
            modifier.write(3, location.getYaw());
            modifier.write(4, location.getPitch());

            if (VersionUtil.isVersionAtLeast("1.16")) {
                modifier.write(5, Collections.emptySet());
                modifier.write(6, teleportIdCounter.incrementAndGet());
            } else {
                modifier.write(5, (byte) 0);
                modifier.write(6, teleportIdCounter.incrementAndGet());
            }

            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}