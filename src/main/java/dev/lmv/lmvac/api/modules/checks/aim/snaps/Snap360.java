package dev.lmv.lmvac.api.modules.checks.aim.snaps;

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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

// Обычный чек на снапы
@SettingCheck(
   value = "Snap360",
   cooldown = Cooldown.COOLDOWN
)
public class Snap360 extends Check implements PacketCheck, Listener {
   private static final ConcurrentHashMap<Player,List<AimInformation>> playerAimData = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<Player,List<Long>> detects = new ConcurrentHashMap<>();

   public Snap360(Plugin plugin) {
      super(plugin);
   }

   @EventHandler
   private void onPlayerQuit(PlayerQuitEvent event) {
       Player player = event.getPlayer();
       playerAimData.remove(player);
       detects.remove(player);
   }

   public void onPacketReceiving(PacketEvent packetEvent) {
      Player player = packetEvent.getPlayer();
      PacketType packetType = packetEvent.getPacketType();
      LmvPlayer targetPlayer = LmvPlayer.get(player);
      PacketContainer packet = packetEvent.getPacket();
      if (targetPlayer != null) {
         long now = System.currentTimeMillis();
         detects.computeIfAbsent(player, k -> new ArrayList<>()).removeIf(k -> now-k > 1500);
         if (packetType == Client.LOOK || packetType == Client.POSITION_LOOK) {
            List looks = targetPlayer.looks;
            if (looks.size() >= 2) {
               LmvPlayer.LookInformation lastLook = (LmvPlayer.LookInformation)looks.get(looks.size() - 2);
               LmvPlayer.LookInformation currentLook = (LmvPlayer.LookInformation)looks.get(looks.size() - 1);
               if (lastLook != null && currentLook != null) {
                  if (lastLook.location != null && currentLook.location != null) {
                     Location lastLocation = lastLook.location.clone();
                     Location currentLocation = currentLook.location.clone();
                     float yawDiff = Math.abs(lastLocation.getYaw() - currentLocation.getYaw());
                     if (yawDiff > 180.0F) {
                        yawDiff = 360.0F - yawDiff;
                     }

                     boolean targetChanged = false;
                     if (lastLook.target != null && currentLook.target != null) {
                        targetChanged = !lastLook.target.equals(currentLook.target);
                     } else if (lastLook.target != currentLook.target) {
                        targetChanged = true;
                     }

                     boolean snap = targetChanged && yawDiff > 85.0F;
                     AimInformation thisAim = new AimInformation(packetEvent, lastLook, currentLook, snap, yawDiff);
                     List lastAims = (List)playerAimData.computeIfAbsent(player, (k) -> {
                        return new ArrayList();
                     });
                     if (lastAims.size() > 2) {
                        AimInformation lastAim = (AimInformation)lastAims.get(lastAims.size() - 1);
                        if (lastAim.isSnap && !thisAim.isSnap && yawDiff < 30.0F && currentLook.target != null || lastAim.isSnap && thisAim.isSnap && lastAim.thisLook.target != null && currentLook.target == null && Math.abs(lastAim.yawDiff - thisAim.yawDiff) < 15.0F) {
                           this.flag(player);
                           detects.computeIfAbsent(player,k -> new ArrayList<>()).add(now);
                        }
                     }

                     lastAims.add(thisAim);
                     if (lastAims.size() > 20) {
                        lastAims.remove(0);
                     }

                  }
               }
            }
         } else if (packetType == Client.USE_ENTITY) {
//             int attackedEntityId = packet.getSpecificModifier(int.class).read(0);
             if (!detects.getOrDefault(player, new ArrayList<>()).isEmpty()) {
                 packetEvent.setCancelled(true);
                 detects.remove(player);
             }
         }
      }
   }

   public ListeningWhitelist getReceivingWhitelist() {
      return ListeningWhitelist.newBuilder().types(new PacketType[]{Client.LOOK, Client.POSITION_LOOK, Client.USE_ENTITY}).build();
   }

   public static class AimInformation {
      public PacketEvent packetEvent;
      public LmvPlayer.LookInformation lastLook;
      public LmvPlayer.LookInformation thisLook;
      public Boolean isSnap;
      public Float yawDiff;
      public Long time;

      public AimInformation(PacketEvent packetEvent, LmvPlayer.LookInformation lastLook, LmvPlayer.LookInformation thisLook, Boolean snap, Float yawDiff) {
         this.packetEvent = packetEvent;
         this.lastLook = lastLook;
         this.thisLook = thisLook;
         this.isSnap = snap;
         this.yawDiff = yawDiff;
         this.time = System.currentTimeMillis();
      }
   }
}
