package dev.lmv.lmvac.api.implement.animations;

import dev.lmv.lmvac.LmvAC;
import dev.lmv.lmvac.api.ConfigManager;
import dev.lmv.lmvac.api.implement.animations.api.Blocker;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DefaultAnimation {
   public static void play(final Player player, final List commands) {
      Blocker.players.put(player.getUniqueId(), commands);
      if (!player.isDead()) {
         player.setAllowFlight(false);
         player.setFlying(false);
         player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 100, 1));
         player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 1));
         player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 90, 1));
         Bukkit.getScheduler().runTaskLater(LmvAC.getInstance(), new Runnable() {
            public void run() {
               if (player.isOnline()) {
                  Location loc = player.getLocation();
                  player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation(), 1);
                  player.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 10, 2.0, 1.0, 2.0, 0.1);
                  player.getWorld().spawnParticle(Particle.FLAME, loc, 15, 1.5, 0.5, 1.5, 0.05);
                  player.getWorld().spawnParticle(Particle.LAVA, loc, 8, 1.0, 0.5, 1.0, 0.0);
                  player.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.8F);
               }
            }
         }, 90L);
      }

      Bukkit.getScheduler().runTaskLater(LmvAC.getInstance(), new Runnable() {
         public void run() {
            Blocker.players.remove(player.getUniqueId());
            if (player.isOnline()) {
               if (!commands.isEmpty()) {
                  Iterator var1 = commands.iterator();

                  while(var1.hasNext()) {
                     String command = (String)var1.next();
                     String cmdNew = command.replace("%prefix%", ConfigManager.prefix).replace("%player%", player.getName());
                     Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdNew);
                  }
               }

            }
         }
      }, 100L);
   }
}
