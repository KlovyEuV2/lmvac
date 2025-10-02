package dev.lmv.lmvac.api.modules.checks.flight;

import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.BukkitCheck;
import dev.lmv.lmvac.api.implement.utils.FluidUtil;
import dev.lmv.lmvac.api.implement.utils.WebUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

// Флай чек на Bukkit' эвентах...
@SettingCheck(
   value = "FlightA",
   cooldown = Cooldown.COOLDOWN
)
public class FlightA extends Check implements BukkitCheck {
   private final ConcurrentHashMap lasts = new ConcurrentHashMap();
   private final ConcurrentHashMap xzFlyViolations = new ConcurrentHashMap();
   private final ConcurrentHashMap lastGroundTime = new ConcurrentHashMap();
   private final ConcurrentHashMap lastGroundLocation = new ConcurrentHashMap();
   private final ConcurrentHashMap lastFlagLocation = new ConcurrentHashMap();
   private final ConcurrentHashMap lastUpTime = new ConcurrentHashMap();
   private final ConcurrentHashMap lastElytraGlidingTime = new ConcurrentHashMap();

   public FlightA(Plugin plugin) {
      super(plugin);
   }

   private boolean isClimbable(Block block) {
      Material type = block.getType();
      return type == Material.LADDER || type == Material.VINE || type == Material.SCAFFOLDING || type == Material.WEEPING_VINES || type == Material.TWISTING_VINES || type == Material.WEEPING_VINES_PLANT || type == Material.TWISTING_VINES_PLANT;
   }

   private boolean hasClimbableNearby(Location location) {
      for(int x = -1; x <= 1; ++x) {
         for(int z = -1; z <= 1; ++z) {
            for(int y = -1; y <= 2; ++y) {
               Block block = location.clone().add((double)x, (double)y, (double)z).getBlock();
               if (this.isClimbable(block)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean isInLiquid(Location location) {
      Block block = location.getBlock();
      return block.getType() == Material.WATER || block.getType() == Material.LAVA || block.getType() == Material.BUBBLE_COLUMN;
   }

   @EventHandler
   private void checkA(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
       int id = event.getPlayer().getEntityId();
       LmvPlayer targetPlayer = (LmvPlayer)LmvPlayer.players.get(id);
       if (targetPlayer == null) return;
       if (targetPlayer.hasBypass(getName())) return;
      Location from = event.getFrom();
      Location to = event.getTo();
      long now = System.currentTimeMillis();
      if (player.isGliding() || player.isFlying()) {
         this.lastElytraGlidingTime.put(uuid, now);
      }

      Long lastElytraTime = (Long)this.lastElytraGlidingTime.get(uuid);
      if (lastElytraTime != null && now - lastElytraTime < 350L) {
         this.lasts.remove(uuid);
         this.xzFlyViolations.remove(uuid);
         this.lastGroundTime.put(uuid, now);
         this.lastGroundLocation.put(uuid, player.getLocation());
         this.lastUpTime.put(uuid, now);
      } else if (!player.isFlying() && player.isTicking() && !player.isGliding() && !player.isInsideVehicle() && !this.hasClimbableNearby(to) && !FluidUtil.isInFluid(player) && !WebUtil.isInWeb(player) && !this.isInLiquid(to) && !this.isInLiquid(from)) {
         if (player.isOnGround()) {
            this.lasts.remove(uuid);
            this.xzFlyViolations.remove(uuid);
            this.lastGroundTime.put(uuid, now);
            this.lastGroundLocation.put(uuid, player.getLocation());
            this.lastUpTime.put(uuid, now);
         } else {
            if (!(to.getY() - from.getY() > 0.08) && !(to.getY() - from.getY() < -0.08)) {
               this.lastUpTime.putIfAbsent(uuid, now);
            } else {
               this.lastUpTime.put(uuid, now);
            }

            Long groundTime = (Long)this.lastGroundTime.get(uuid);
            Location groundLoc = (Location)this.lastGroundLocation.get(uuid);
            Long lastUp = (Long)this.lastUpTime.get(uuid);
            if (groundTime != null && groundLoc != null && lastUp != null) {
               long timeSinceLastUp = now - lastUp;
               double yDifference = groundLoc.getY() - to.getY();
               Location belowPlayer = to.clone().subtract(0.0, 0.1, 0.0);
               Block blockBelow = belowPlayer.getBlock();
               boolean var10000;
               if (blockBelow != null && blockBelow.getType().isSolid()) {
                  var10000 = true;
               } else {
                  var10000 = false;
               }

               boolean isIllegal = !player.isOnGround() && timeSinceLastUp > 435L && to.getY() - from.getY() < 0.08;
               if (isIllegal) {
                  if (player.getVelocity().getX() > 0.03 || player.getVelocity().getZ() > 0.03 || player.getVelocity().getY() > 0.03) {
                     this.lastFlagLocation.put(player.getUniqueId(), player.getLocation());
                     if (groundLoc.getWorld().equals(player.getWorld())) {
                        event.setTo(groundLoc.clone());
                     } else {
                        event.setCancelled(true);
                     }

                     this.flag(player);
                     return;
                  }

                  if (!this.lastFlagLocation.containsKey(player.getUniqueId())) {
                     if (groundLoc.getWorld().equals(player.getWorld())) {
                        event.setTo(groundLoc.clone());
                     } else {
                        event.setCancelled(true);
                     }

                     this.flag(player);
                     this.lastFlagLocation.put(player.getUniqueId(), player.getLocation());
                     return;
                  }

                  if (((Location)this.lastFlagLocation.get(player.getUniqueId())).distance(player.getLocation()) > 1.38) {
                     if (groundLoc.getWorld().equals(player.getWorld())) {
                        event.setTo(groundLoc.clone());
                     } else {
                        event.setCancelled(true);
                     }

                     this.flag(player);
                     this.lastFlagLocation.put(player.getUniqueId(), player.getLocation());
                     return;
                  }
               }

               if (player.isJumping() && !player.isOnGround()) {
                  if (groundLoc.getWorld().equals(player.getWorld())) {
                     event.setTo(groundLoc.clone());
                  } else {
                     event.setCancelled(true);
                  }

                  this.flag(player);
                  return;
               }
            }

            List locations = (List)this.lasts.computeIfAbsent(uuid, (k) -> {
               return new ArrayList();
            });
            locations.add(to.clone());
            if (locations.size() > 10) {
               locations.remove(0);
            }

            if (locations.size() >= 2) {
               this.checkXZFlight(player, from, to, uuid, event);
               this.checkVerticalFlight(player, locations, to, from, event);
            }
         }
      } else {
         this.lasts.remove(uuid);
         this.lastGroundTime.put(uuid, now);
         this.lastUpTime.remove(uuid);
         this.xzFlyViolations.remove(uuid);
      }
   }

   private void checkXZFlight(Player player, Location from, Location to, UUID uuid, PlayerMoveEvent event) {
      double deltaY = to.getY() - from.getY();
      double deltaXZ = Math.sqrt(Math.pow(to.getX() - from.getX(), 2.0) + Math.pow(to.getZ() - from.getZ(), 2.0));
      long timeSinceGround = System.currentTimeMillis() - (Long)this.lastGroundTime.getOrDefault(uuid, System.currentTimeMillis());
      if (deltaXZ > 0.1 && Math.abs(deltaY) < 0.05 && timeSinceGround > 1000L) {
         int violations = (Integer)this.xzFlyViolations.getOrDefault(uuid, 0) + 1;
         this.xzFlyViolations.put(uuid, violations);
         if (violations >= 5) {
            Location lastGround = (Location)this.lastGroundLocation.get(uuid);
            if (lastGround != null && lastGround.getWorld().equals(player.getWorld())) {
               event.setTo(lastGround.clone());
            } else {
               event.setCancelled(true);
            }

            this.flag(player);
            this.xzFlyViolations.put(uuid, violations - 2);
         }
      } else {
         this.xzFlyViolations.put(uuid, Math.max(0, (Integer)this.xzFlyViolations.getOrDefault(uuid, 0) - 1));
      }

   }

   private void checkVerticalFlight(Player player, List locations, Location to, Location from, PlayerMoveEvent event) {
      List distances = new ArrayList();

      for(int i = 1; i < locations.size(); ++i) {
         distances.add(((Location)locations.get(i)).distance((Location)locations.get(i - 1)));
      }

      UUID uuid = player.getUniqueId();
      if (distances.size() >= 2) {
         double deltaY = to.getY() - from.getY();
         int stableCount = 0;

         for(int i = 1; i < distances.size(); ++i) {
            if (Math.abs((Double)distances.get(i) - (Double)distances.get(i - 1)) <= 0.01) {
               ++stableCount;
            }
         }

         double stablePercentage = (double)stableCount / (double)distances.size() * 100.0;
         if (player.hasPotionEffect(PotionEffectType.LEVITATION)) {
            if (!player.isFlying()) {
               double maxDeltaY = 0.93 * (double)player.getPotionEffect(PotionEffectType.LEVITATION).getAmplifier();
               if (deltaY > maxDeltaY) {
                  event.setCancelled(true);
                  this.flag(player);
               }

            }
         } else {
            if (stablePercentage >= 30.0 && to.getY() > from.getY()) {
               Location lastGround = (Location)this.lastGroundLocation.get(uuid);
               if (lastGround != null && lastGround.getWorld().equals(player.getWorld())) {
                  event.setTo(lastGround.clone());
               } else {
                  event.setCancelled(true);
               }

               this.flag(player);
            }

         }
      }
   }
}
