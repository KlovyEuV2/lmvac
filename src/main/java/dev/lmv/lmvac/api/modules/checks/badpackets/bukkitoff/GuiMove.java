package dev.lmv.lmvac.api.modules.checks.badpackets.bukkitoff;

import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.BukkitCheck;
import dev.lmv.lmvac.api.implement.utils.FluidUtil;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;

@SettingCheck(
   value = "BadPacketsC",
   cooldown = Cooldown.COOLDOWN
)
public class GuiMove extends Check implements BukkitCheck {
   public GuiMove(Plugin plugin) {
      super(plugin);
   }

   private boolean isPlayerInSolidBlockOrBeingPushed(Player player) {
      BoundingBox playerBox = player.getBoundingBox();
      World world = player.getWorld();
      int minX = playerBox.getMin().getBlockX();
      int maxX = playerBox.getMax().getBlockX();
      int minY = playerBox.getMin().getBlockY();
      int maxY = playerBox.getMax().getBlockY();
      int minZ = playerBox.getMin().getBlockZ();
      int maxZ = playerBox.getMax().getBlockZ();

      for(int x = minX; x <= maxX; ++x) {
         for(int y = minY; y <= maxY; ++y) {
            for(int z = minZ; z <= maxZ; ++z) {
               Block block = world.getBlockAt(x, y, z);
               Material type = block.getType();
               if (!type.isAir() && !type.name().contains("WATER") && !type.name().contains("LAVA") && (type.isSolid() || type.name().contains("PISTON") || type.name().contains("MOVING_PISTON") || type.name().contains("SLIME") || type.name().contains("HONEY"))) {
                  BoundingBox blockBox = BoundingBox.of(block);
                  if (playerBox.overlaps(blockBox)) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   private void check(InventoryClickEvent ev) {
      HumanEntity clicker = ev.getWhoClicked();
      if (clicker instanceof Player) {
         Player player = (Player)clicker;
         int id = player.getEntityId();
         LmvPlayer targetPlayer = (LmvPlayer)LmvPlayer.players.get(id);
         if (targetPlayer == null) {
            return;
         }

         if (player.isInsideVehicle()) {
            return;
         }

         if (targetPlayer.hasBypass(this.getName())) {
            return;
         }

         if (this.checkMove(player)) {
            ev.setCancelled(true);
            this.flag(player);
         }
      }

   }

   private Boolean isInventoryOpened(Player p) {
      int id = p.getEntityId();
      LmvPlayer targetPlayer = (LmvPlayer)LmvPlayer.players.get(id);
      return targetPlayer == null ? false : targetPlayer.lastInventoryOpen > targetPlayer.lastWindowClose || p.getOpenInventory().getType().isCreatable();
   }

   private Boolean checkMove(Player plr) {
      int id = plr.getEntityId();
      LmvPlayer targetPlayer = (LmvPlayer)LmvPlayer.players.get(id);
      return targetPlayer == null ? false : plr.isSprinting() && !FluidUtil.isInFluid(plr) && !plr.isGliding() || plr.isSneaking() || plr.isSleeping() || plr.isBlocking() || plr.isJumping() || plr.isDead() || plr.isHandRaised();
   }
}
