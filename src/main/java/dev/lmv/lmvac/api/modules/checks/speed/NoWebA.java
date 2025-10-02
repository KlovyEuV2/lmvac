package dev.lmv.lmvac.api.modules.checks.speed;

import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.BukkitCheck;
import dev.lmv.lmvac.api.implement.utils.WebUtil;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

// чек на NoWeb
@SettingCheck(
   value = "NoWebA",
   cooldown = Cooldown.COOLDOWN
)
public class NoWebA extends Check implements BukkitCheck {
   private static Plugin plugin;
   private static final ConcurrentHashMap sendFlag = new ConcurrentHashMap();
   private static final ConcurrentHashMap lastLocation = new ConcurrentHashMap();

   public NoWebA(Plugin plugin) {
      super(plugin);
      NoWebA.plugin = plugin;
   }

   @EventHandler
   private void onMove(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      int id = event.getPlayer().getEntityId();
      LmvPlayer targetPlayer = (LmvPlayer)LmvPlayer.players.get(id);
       if (targetPlayer == null) return;
      if (targetPlayer.hasBypass(getName())) return;
      if (!WebUtil.isInWeb(player)) {
         lastLocation.put(uuid, player.getLocation());
      } else if (!checkUnfair(player)) {
         Location from = event.getFrom();
         Location to = event.getTo();
         boolean fromWeb = isInWeb(from) || isInWeb(from.clone().add(0.0, 1.1321, 0.0));
         boolean toWeb = isInWeb(to) || isInWeb(to.clone().add(0.0, 1.1321, 0.0));
         if (fromWeb && toWeb) {
            double dx = Math.abs(from.getX() - to.getX());
            double dz = Math.abs(from.getZ() - to.getZ());
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            double dy = to.getY() - from.getY();
            double allowedSpeed = getMaxWebSpeed(player);
            if (horizontalDistance > allowedSpeed + 0.05 || dy < -(allowedSpeed + 0.05) || dy > 0.08) {
               this.flag(player);
               if (lastLocation.containsKey(uuid)) {
                  Location tpLoc = (Location)lastLocation.get(uuid);
                  event.setTo(tpLoc);
               } else {
                  event.setCancelled(true);
               }
            }
         }

      }
   }

   private static boolean isInWeb(Location loc) {
      Material type = loc.getBlock().getType();
      return type == Material.COBWEB || type.name().toLowerCase().contains("web");
   }

   private static double getMaxWebSpeed(Player player) {
      double baseSpeed = 0.1;
      AttributeInstance attr = (AttributeInstance)Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED));
      baseSpeed = attr.getBaseValue();
      PotionEffect speedEffect = player.getPotionEffect(PotionEffectType.SPEED);
      if (speedEffect != null) {
         baseSpeed += (double)(1 + ((PotionEffect)Objects.requireNonNull(player.getPotionEffect(PotionEffectType.SPEED))).getAmplifier()) * 0.015;
      }

      if (player.isSprinting()) {
         baseSpeed *= 1.29;
      }

      return baseSpeed;
   }

   private static boolean checkUnfair(Player player) {
      return player.isFlying() || player.isGliding() || player.getVelocity().getY() > 0.8 || player.getVelocity().getX() > 0.25 || player.getVelocity().getZ() > 0.25 || player.isInsideVehicle() || player.isDead() || player.isSleeping() || !player.isOnline();
   }
}
