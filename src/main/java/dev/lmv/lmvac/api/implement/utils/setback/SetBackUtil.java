package dev.lmv.lmvac.api.implement.utils.setback;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;

import java.util.concurrent.atomic.AtomicInteger;

public class SetBackUtil {
    private static final AtomicInteger teleportIdCounter = new AtomicInteger(0);
    private static final ProtocolManager protocolManager =
            ProtocolLibrary.getProtocolManager();

    public static void sendSetBack(LmvPlayer player, double x, double y, double z) {
        int teleportId = teleportIdCounter.incrementAndGet();

        PacketContainer packet = protocolManager.createPacket(
                com.comphenix.protocol.PacketType.Play.Server.POSITION
        );

        packet.getDoubles()
                .write(0, x)
                .write(1, y)
                .write(2, z);

        packet.getFloat()
                .write(0, player.yaw)
                .write(1, player.pitch);

        if (packet.getBytes().size() > 0) {
            packet.getBytes().write(0, (byte) 0);
        }

        if (packet.getIntegers().size() > 0) {
            packet.getIntegers().write(0, teleportId);
        }

        protocolManager.sendServerPacket(player.player, packet);
    }
}
