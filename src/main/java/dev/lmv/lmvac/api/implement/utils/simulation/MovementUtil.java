package dev.lmv.lmvac.api.implement.utils.simulation;

import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.utils.FluidUtil;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

public class MovementUtil {
   public static Boolean isPlayerInSolidBlockOrBeingPushed(Player player) {
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

   public static Boolean isInventoryOpened(Player p) {
      int id = p.getEntityId();
      LmvPlayer targetPlayer = (LmvPlayer)LmvPlayer.players.get(id);
      return targetPlayer == null ? false : targetPlayer.lastInventoryOpen > targetPlayer.lastWindowClose || p.getOpenInventory().getType().isCreatable();
   }

   public static Boolean checkMove(Player plr) {
      int id = plr.getEntityId();
      LmvPlayer targetPlayer = (LmvPlayer)LmvPlayer.players.get(id);
      return targetPlayer == null ? false : (plr.isSprinting() && !FluidUtil.isInFluid(plr) && !plr.isGliding()) || plr.isSneaking() || plr.isSleeping() || plr.isJumping() || plr.isDead();
   }
}
