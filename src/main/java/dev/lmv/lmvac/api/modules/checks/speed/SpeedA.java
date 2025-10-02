package dev.lmv.lmvac.api.modules.checks.speed;

import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.BukkitCheck;
import dev.lmv.lmvac.api.implement.utils.FluidUtil;
import dev.lmv.lmvac.api.implement.utils.WebUtil;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

// Чек на Speed, детектит многие Speed, бывают ложные...
@SettingCheck(
   value = "SpeedA",
   cooldown = Cooldown.COOLDOWN
)
public class SpeedA extends Check implements BukkitCheck {
   private final ConcurrentHashMap<UUID,Long> lastGroundTime = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<UUID,Location> lastGroundLocation = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<UUID,Long> lastFlight = new ConcurrentHashMap<>();

   public SpeedA(Plugin plugin) {
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
   private void move(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
       int id = event.getPlayer().getEntityId();
       LmvPlayer targetPlayer = (LmvPlayer)LmvPlayer.players.get(id);
       if (targetPlayer == null) return;
       if (targetPlayer.hasBypass(getName())) return;
      Location from = event.getFrom();
      Location to = event.getTo();
      long now = System.currentTimeMillis();
      if (!player.isFlying() && player.isTicking() && !player.isGliding() && !player.isInsideVehicle() && !this.hasClimbableNearby(to) && !FluidUtil.isInFluid(player) && !WebUtil.isInWeb(player) && !this.isInLiquid(to) && !this.isInLiquid(from)) {
         if (player.isOnGround()) {
            this.lastGroundTime.put(uuid, now);
            this.lastGroundLocation.put(uuid, player.getLocation());
         }

         if (!this.lastFlight.containsKey(uuid) || now - (Long)this.lastFlight.get(uuid) >= 350L) {
            double distX = Math.abs(to.getX() - from.getX());
            double distZ = Math.abs(to.getZ() - from.getZ());
            double maxSpeed = this.getCalculatedMaxSpeed(player);
            Location groundLoc = (Location)this.lastGroundLocation.get(uuid);
            if (!(player.getVelocity().getY() > 0.8) && !(player.getVelocity().getX() > 0.25) && !(player.getVelocity().getZ() > 0.25)) {
               if (distX > maxSpeed || distZ > maxSpeed) {
                  if (groundLoc != null && groundLoc.getWorld().equals(player.getWorld())) {
                     event.setTo(groundLoc.clone());
                  } else {
                     event.setCancelled(true);
                  }

                  this.flag(player);
               }

            }
         }
      } else {
         this.lastFlight.put(uuid, now);
         this.lastGroundTime.put(uuid, now);
         this.lastGroundLocation.put(uuid, player.getLocation());
      }
   }

   private double getCalculatedMaxSpeed(Player player) {
      double baseSpeed = 0.46;
      if (player.isSprinting()) {
         baseSpeed *= 1.3;
      }

      if (player.hasPotionEffect(PotionEffectType.SPEED)) {
         int level = 0;
         Iterator var5 = player.getActivePotionEffects().iterator();

         while(var5.hasNext()) {
            PotionEffect effect = (PotionEffect)var5.next();
            if (effect.getType().equals(PotionEffectType.SPEED)) {
               level = effect.getAmplifier();
               break;
            }
         }

         baseSpeed *= 1.0 + 0.2 * (double)level;
      }

      double attributeSpeed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();
      double defaultAttributeSpeed = 0.1;
      double attributeMultiplier = attributeSpeed / defaultAttributeSpeed;
      baseSpeed *= attributeMultiplier;
      return baseSpeed;
   }
}
