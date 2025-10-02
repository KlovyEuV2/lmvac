package dev.lmv.lmvac.api.implement.utils;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class WebUtil {
   public static boolean isInWeb(Player player) {
      if (player.isSwimming()) {
         Block blockBelow = player.getLocation().add(0.0, 0.5, 0.0).getBlock();
         Block blockFront = player.getLocation().add(0.3, 0.5, 0.0).getBlock();
         Block blockBack = player.getLocation().add(-0.3, 0.5, 0.0).getBlock();
         Block blockLeft = player.getLocation().add(0.0, 0.5, 0.3).getBlock();
         Block blockRight = player.getLocation().add(0.0, 0.5, -0.3).getBlock();
         return isWebOrSolid(blockBelow) || isWebOrSolid(blockFront) || isWebOrSolid(blockBack) || isWebOrSolid(blockLeft) || isWebOrSolid(blockRight);
      } else {
         boolean headInWeb = false;

         for(double y = -0.2; y <= 0.0; y += 0.1) {
            for(double x = -0.3; x <= 0.3; x += 0.1) {
               for(double z = -0.3; z <= 0.3; z += 0.1) {
                  Block headBlock = player.getEyeLocation().subtract(x, y, z).getBlock();
                  if (isWebOrSolid(headBlock)) {
                     headInWeb = true;
                     break;
                  }
               }

               if (headInWeb) {
                  break;
               }
            }

            if (headInWeb) {
               break;
            }
         }

         boolean feetInWeb = false;

         for(double x = -0.3; x <= 0.3; x += 0.1) {
            for(double z = -0.3; z <= 0.3; z += 0.1) {
               Block feetBlock = player.getLocation().clone().add(x, 0.0, z).getBlock();
               if (isWebOrSolid(feetBlock)) {
                  feetInWeb = true;
                  break;
               }
            }

            if (feetInWeb) {
               break;
            }
         }

         return headInWeb || feetInWeb;
      }
   }

   private static boolean isWebOrSolid(Block block) {
      Material type = block.getType();
      return type == Material.COBWEB || type == Material.SLIME_BLOCK || type == Material.LADDER || type == Material.VINE || type == Material.SCAFFOLDING || type == Material.WEEPING_VINES || type == Material.TWISTING_VINES || type == Material.WEEPING_VINES_PLANT || type == Material.TWISTING_VINES_PLANT;
   }
}
