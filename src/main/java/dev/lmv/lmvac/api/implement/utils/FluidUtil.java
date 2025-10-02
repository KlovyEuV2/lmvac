package dev.lmv.lmvac.api.implement.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;

public class FluidUtil {
   public static boolean isInFluid(Player player) {
      if (player.isSwimming()) {
         Block blockBelow = player.getLocation().add(0.0, 0.5, 0.0).getBlock();
         Block blockFront = player.getLocation().add(0.3, 0.5, 0.0).getBlock();
         Block blockBack = player.getLocation().add(-0.3, 0.5, 0.0).getBlock();
         Block blockLeft = player.getLocation().add(0.0, 0.5, 0.3).getBlock();
         Block blockRight = player.getLocation().add(0.0, 0.5, -0.3).getBlock();
         boolean isInFluid = isFluid(blockBelow) || isFluid(blockFront) || isFluid(blockBack) || isFluid(blockLeft) || isFluid(blockRight) || isFlowingWaterNearby(player);
         return isInFluid;
      } else {
         boolean headInFluid = false;

         for(double y = -0.2; y <= 0.0; y += 0.1) {
            for(double x = -0.3; x <= 0.3; x += 0.1) {
               for(double z = -0.3; z <= 0.3; z += 0.1) {
                  Block headBlock = player.getEyeLocation().subtract(x, y, z).getBlock();
                  if (isFluid(headBlock)) {
                     headInFluid = true;
                     break;
                  }
               }

               if (headInFluid) {
                  break;
               }
            }

            if (headInFluid) {
               break;
            }
         }

         boolean feetInFluid = false;

         for(double x = -0.3; x <= 0.3; x += 0.1) {
            for(double z = -0.3; z <= 0.3; z += 0.1) {
               Block feetBlock = player.getLocation().clone().add(x, 0.0, z).getBlock();
               if (isFluid(feetBlock)) {
                  feetInFluid = true;
                  break;
               }
            }

            if (feetInFluid) {
               break;
            }
         }

         boolean isInFluid = headInFluid || feetInFluid || isFlowingWaterNearby(player);
         return isInFluid;
      }
   }

   private static boolean isFluid(Block block) {
      return block.getType() == Material.WATER || block.getType() == Material.LAVA;
   }

   public static boolean isFlowingWaterNearby(Player player) {
      Location loc = player.getLocation();
      World world = player.getWorld();

      for(int x = -1; x <= 1; ++x) {
         for(int y = -1; y <= 1; ++y) {
            for(int z = -1; z <= 1; ++z) {
               Block nearby = world.getBlockAt(loc.clone().add((double)x, (double)y, (double)z));
               if (nearby.getType() == Material.WATER) {
                  BlockData data = nearby.getBlockData();
                  if (data instanceof Levelled) {
                     Levelled levelled = (Levelled)data;
                     if (levelled.getLevel() > 0) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }
}
