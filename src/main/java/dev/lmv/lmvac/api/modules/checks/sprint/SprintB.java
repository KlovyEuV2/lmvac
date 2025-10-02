package dev.lmv.lmvac.api.modules.checks.sprint;

import dev.lmv.lmvac.LmvAC;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.BukkitCheck;
import dev.lmv.lmvac.api.implement.utils.FluidUtil;
import dev.lmv.lmvac.api.implement.utils.IceUtil;
import dev.lmv.lmvac.api.implement.utils.listeners.MovementListener;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.Plugin;

// Детект сброса спринта
@SettingCheck(
   value = "SprintB",
   cooldown = Cooldown.COOLDOWN
)
public class SprintB extends Check implements BukkitCheck {
   public SprintB(Plugin plugin) {
      super(plugin);
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player) {
         if (event.getCause() == DamageCause.ENTITY_ATTACK) {
            Player player = (Player)event.getDamager();
            int id = player.getEntityId();
            LmvPlayer targetPlayer = (LmvPlayer)LmvPlayer.players.get(id);
            if (targetPlayer != null) {
               if (!targetPlayer.hasBypass(this.getName())) {
                  if (!FluidUtil.isInFluid(player) && !IceUtil.isOnIce(player)) {
                     if (isCritReady(player)) {
                        this.handleHardSimulation(event);
                     }

                  }
               }
            }
         }
      }
   }

   private void handleHardSimulation(EntityDamageByEntityEvent event) {
      Entity damager = event.getDamager();
      if (damager instanceof Player) {
         Player player = (Player)damager;
         UUID uuid = player.getUniqueId();
         long now = System.currentTimeMillis();
         if (!FluidUtil.isInFluid(player)) {
            if (player.isSprinting() || player.isInsideVehicle()) {
               return;
            }

            Bukkit.getScheduler().runTaskLater(LmvAC.getInstance(), () -> {
               if (player.isSprinting() && !event.isCancelled() && isSpeedSimulation(player) && !player.isJumping()) {
                  this.flag(player);
               }

            }, hasPingTimeOut(player));
         }
      }

   }

   private static boolean isCritReady(Player player) {
      return player.getAttackCooldown() >= 0.9F;
   }

   private static boolean isSpeedSimulation(Player player) {
      List locs = (List)MovementListener.lastLocations.getOrDefault(player.getUniqueId(), new ArrayList());
      Location from = (Location)locs.get(locs.size() - 2);
      Location to = player.getLocation();
      return Math.abs(from.getX() - to.getX()) > 0.21 || Math.abs(from.getZ() - to.getZ()) > 0.21;
   }

   public static long hasPingTimeOut(Player player) {
      int max = 2;
      return (long)Math.max(0, Math.min(player.getPing() / 50, max));
   }
}
