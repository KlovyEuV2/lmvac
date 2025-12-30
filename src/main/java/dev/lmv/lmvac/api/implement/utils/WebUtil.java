package dev.lmv.lmvac.api.implement.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.Set;

public class WebUtil {
    private static final Set<Material> WEB_OR_SOLID_BLOCKS = EnumSet.of(
            Material.COBWEB, Material.SLIME_BLOCK, Material.LADDER,
            Material.VINE, Material.SCAFFOLDING, Material.WEEPING_VINES,
            Material.TWISTING_VINES, Material.WEEPING_VINES_PLANT,
            Material.TWISTING_VINES_PLANT
    );

    private static final double[] HEAD_OFFSETS = {-0.2, -0.1, 0.0};
    private static final double[] HORIZONTAL_OFFSETS = {-0.3, -0.2, -0.1, 0.0, 0.1, 0.2, 0.3};
    private static final double[] FEET_OFFSETS = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5};

    public static boolean isInWeb(Player player) {
        if (player.isSwimming()) {
            return checkSwimmingPlayer(player);
        } else {
            return checkRegularPlayer(player);
        }
    }

    private static boolean checkSwimmingPlayer(Player player) {
        Location baseLoc = player.getLocation().add(0.0, 0.5, 0.0);

        return isWebOrSolid(baseLoc.getBlock()) ||
                isWebOrSolid(baseLoc.add(0.3, 0.0, 0.0).getBlock()) ||
                isWebOrSolid(baseLoc.add(-0.3, 0.0, 0.0).getBlock()) ||
                isWebOrSolid(baseLoc.add(0.0, 0.0, 0.3).getBlock()) ||
                isWebOrSolid(baseLoc.add(0.0, 0.0, -0.3).getBlock());
    }

    private static boolean checkRegularPlayer(Player player) {
        boolean headInWeb = checkHeadInWeb(player);
        if (headInWeb) {
            return true;
        }

        return checkFeetInWeb(player);
    }

    private static boolean checkHeadInWeb(Player player) {
        Location eyeLoc = player.getEyeLocation();

        for (double y : HEAD_OFFSETS) {
            for (double x : HORIZONTAL_OFFSETS) {
                for (double z : HORIZONTAL_OFFSETS) {
                    Block headBlock = eyeLoc.getWorld().getBlockAt(
                            eyeLoc.getBlockX() + (int) x,
                            eyeLoc.getBlockY() + (int) y,
                            eyeLoc.getBlockZ() + (int) z
                    );

                    if (isWebOrSolid(headBlock)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean checkFeetInWeb(Player player) {
        Location loc = player.getLocation();

        for (double x : HORIZONTAL_OFFSETS) {
            for (double z : HORIZONTAL_OFFSETS) {
                Block feetBlock = loc.getWorld().getBlockAt(
                        loc.getBlockX() + (int) x,
                        loc.getBlockY(),
                        loc.getBlockZ() + (int) z
                );

                if (isWebOrSolid(feetBlock)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isWebOrSolid(Block block) {
        return WEB_OR_SOLID_BLOCKS.contains(block.getType());
    }
}