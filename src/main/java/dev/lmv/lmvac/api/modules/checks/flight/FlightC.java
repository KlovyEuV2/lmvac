package dev.lmv.lmvac.api.modules.checks.flight;

import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.BukkitCheck;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

// fly-speed / DragonFlight / FlightStrafe
@SettingCheck(
   value = "FlightC",
   cooldown = Cooldown.COOLDOWN
)
public class FlightC extends Check implements BukkitCheck {
   public static ConcurrentHashMap<UUID,Long> lastFlight = new ConcurrentHashMap<>();

   public FlightC(Plugin plugin) {
      super(plugin);
   }

   @EventHandler
   private void checkA(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      if (!player.isInsideVehicle()) {
         long now = System.currentTimeMillis();
         if (!player.isFlying()) {
            lastFlight.remove(player.getUniqueId());
         } else if (!lastFlight.containsKey(player.getUniqueId())) {
            lastFlight.put(player.getUniqueId(), now);
         } else {
            this.handleEssentials(event);
         }

      }
   }

   private void handleEssentials(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      int id = event.getPlayer().getEntityId();
      LmvPlayer targetPlayer = (LmvPlayer)LmvPlayer.players.get(id);
      if (targetPlayer != null) {
         if (player.isFlying() && !player.isInsideVehicle()) {
            if (!targetPlayer.hasBypass(this.getName())) {
               Location from = event.getFrom();
               Location to = event.getTo();
               double distance = from.distance(to);
               if (distance > this.getMaxSpeed(event)) {
                  String reason = locales.getOrDefault("1", "Suspicious movement while flying. SPerEvent[%0].");
                  String pReason = reason.replace("%0", String.format("%.2f", distance));
                  event.setCancelled(true);
                  this.flag(player,pReason);
               }

            }
         }
      }
   }

   private double getMaxSpeed(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      AttributeInstance flyingAttr = player.getAttribute(Attribute.GENERIC_FLYING_SPEED);
      double attributeSpeed = flyingAttr != null ? flyingAttr.getValue() : 0.0;
      double flySpeed = (double)player.getFlySpeed();
      if (flyingAttr != null) {
         return flySpeed >= 0.1 ? attributeSpeed * 77.0 * flySpeed * 10.0 : attributeSpeed * 77.0;
      } else {
         return flySpeed >= 0.1 ? 1.191 * flySpeed * 10.0 : 1.191;
      }
   }
}
