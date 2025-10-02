package dev.lmv.lmvac.api.modules.checks.badpackets.bukkitoff;

import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.BukkitCheck;
import dev.lmv.lmvac.api.implement.utils.FluidUtil;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.plugin.Plugin;

@SettingCheck(
   value = "BadPacketsA",
   cooldown = Cooldown.COOLDOWN
)
public class ClickPearl extends Check implements BukkitCheck {
   private final ConcurrentHashMap<UUID,Long> lastSlotChange = new ConcurrentHashMap();

   public ClickPearl(Plugin plugin) {
      super(plugin);
   }

   @EventHandler
   public void onPlayerItemHeld(PlayerItemHeldEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      long now = System.currentTimeMillis();
      this.lastSlotChange.put(uuid, now);
   }

   @EventHandler
   public void onPlayerInteract(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      if (event.getItem() != null) {
         int id = player.getEntityId();
         LmvPlayer targetPlayer = (LmvPlayer)LmvPlayer.players.get(id);
         if (targetPlayer != null) {
            if (!player.isInsideVehicle()) {
               if (!targetPlayer.hasBypass(this.getName())) {
                  long now = System.currentTimeMillis();
                  if (event.getAction().toString().contains("RIGHT_CLICK")) {
                     Long lastChange = (Long)this.lastSlotChange.get(uuid);
                     if (lastChange != null) {
                        long diff = now - lastChange;
                        if (player.hasCooldown(event.getItem().getType())) {
                           return;
                        }

                        if (this.checkMove(player) && diff <= 1L) {
                           event.setCancelled(true);
                           this.flag(player);
                        }
                     }
                  }

               }
            }
         }
      }
   }

   private Boolean checkMove(Player plr) {
      long now = System.currentTimeMillis();
      int id = plr.getEntityId();
      LmvPlayer targetPlayer = (LmvPlayer)LmvPlayer.players.get(id);
      if (targetPlayer == null) {
         return false;
      } else {
         return !plr.isGliding() && !plr.isFlying() ? now - targetPlayer.lastMoveTime < 13L || plr.isSprinting() && !FluidUtil.isInFluid(plr) && !plr.isGliding() || plr.isSneaking() || plr.isSleeping() || plr.isBlocking() || plr.isJumping() || plr.isDead() || plr.isHandRaised() : true;
      }
   }
}
