package dev.lmv.lmvac.api.implement.utils;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class IceUtil {
   public static boolean isOnIce(Player player) {
      Block blockBelow = player.getLocation().getBlock().getRelative(0, -1, 0);
      Block blockFront = player.getLocation().add(0.3, -1.0, 0.0).getBlock();
      Block blockBack = player.getLocation().add(-0.3, -1.0, 0.0).getBlock();
      Block blockLeft = player.getLocation().add(0.0, -1.0, 0.3).getBlock();
      Block blockRight = player.getLocation().add(0.0, -1.0, -0.3).getBlock();
      return isIce(blockBelow) || isIce(blockFront) || isIce(blockBack) || isIce(blockLeft) || isIce(blockRight);
   }

   private static boolean isIce(Block block) {
      Material blockMaterial = block.getType();
      return blockMaterial == Material.ICE || blockMaterial == Material.PACKED_ICE || blockMaterial == Material.BLUE_ICE || blockMaterial == Material.FROSTED_ICE;
   }
}
