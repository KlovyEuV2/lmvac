package dev.lmv.lmvac.api.implement.utils;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.PacketContainer;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ProtocolUtil {
   private static final AtomicInteger teleportIdCounter = new AtomicInteger(1);

   public static void sendPositionCorrection(Player player, Location location) {
      PacketContainer packet = new PacketContainer(Server.POSITION);
      packet.getDoubles().write(0, location.getX());
      packet.getDoubles().write(1, location.getY());
      packet.getDoubles().write(2, location.getZ());
      packet.getFloat().write(0, location.getYaw());
      packet.getFloat().write(1, location.getPitch());
      packet.getBytes().write(0, (byte)0);
      int teleportId = teleportIdCounter.getAndIncrement();
      packet.getIntegers().write(0, teleportId);

      try {
         packet.getBooleans().write(0, false);
      } catch (Exception var6) {
      }

      try {
         ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
      } catch (Exception var5) {
         var5.printStackTrace();
      }

   }
}
