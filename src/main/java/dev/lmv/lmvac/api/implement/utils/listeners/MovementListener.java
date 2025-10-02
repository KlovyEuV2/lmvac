package dev.lmv.lmvac.api.implement.utils.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MovementListener implements Listener {
   public static ConcurrentHashMap<UUID,Long> lastMoveTime = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, List<Location>> lastLocations = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<UUID, List<PlayerMoveEvent>> lastEvents = new ConcurrentHashMap<>();

   public MovementListener(JavaPlugin plugin) {
      Bukkit.getPluginManager().registerEvents(this, plugin);
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   private void setHashMove(PlayerMoveEvent ev) {
      Player player = ev.getPlayer();
      if (ev.getFrom().distance(ev.getTo()) > 0.07999999821186066) {
         lastMoveTime.put(player.getUniqueId(), System.currentTimeMillis());
         ((List)lastEvents.computeIfAbsent(player.getUniqueId(), (k) -> {
            return new ArrayList();
         })).add(ev);
         ((List)lastLocations.computeIfAbsent(player.getUniqueId(), (k) -> {
            return new ArrayList();
         })).add(player.getLocation());
      }

   }

   public static double getMaxSpeed(Player player) {
      double baseSpeed = 0.5;
      double speed;
      if (player.isFlying()) {
         speed = baseSpeed * 2.0;
         if (player.isSprinting()) {
            speed *= 1.3;
         }

         return speed;
      } else {
         speed = baseSpeed;
         AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
         if (attribute != null) {
            speed = attribute.getValue();
         }

         PotionEffect speedEffect = player.getPotionEffect(PotionEffectType.SPEED);
         if (speedEffect != null) {
            speed += speed * 0.2 * (double)(speedEffect.getAmplifier() + 1);
         }

         PotionEffect slownessEffect = player.getPotionEffect(PotionEffectType.SLOW);
         if (slownessEffect != null) {
            speed -= speed * 0.15 * (double)(slownessEffect.getAmplifier() + 1);
         }

         if (player.isSprinting()) {
            speed *= 1.3;
         }

         return speed;
      }
   }
}
