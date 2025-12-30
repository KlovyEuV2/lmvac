package dev.lmv.lmvac.api.implement.utils;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class FluidUtil {

    public static boolean isInFluid(Player player) {
        Block b = player.getLocation().getBlock();
        Material m = b.getType();

        if (m == Material.WATER || m == Material.LAVA) return true;

        Block h = b.getRelative(0, 1, 0);
        m = h.getType();
        return m == Material.WATER || m == Material.LAVA;
    }

    public static boolean isInFluidPrecise(Player player) {
        Block b = player.getLocation().getBlock();
        Material m = b.getType();

        if (m == Material.WATER || m == Material.LAVA) return true;

        Block h = b.getRelative(0, 1, 0);
        m = h.getType();
        if (m == Material.WATER || m == Material.LAVA) return true;

        m = b.getRelative(1, 0, 0).getType();
        if (m == Material.WATER || m == Material.LAVA) return true;

        m = b.getRelative(-1, 0, 0).getType();
        if (m == Material.WATER || m == Material.LAVA) return true;

        m = b.getRelative(0, 0, 1).getType();
        if (m == Material.WATER || m == Material.LAVA) return true;

        m = b.getRelative(0, 0, -1).getType();
        return m == Material.WATER || m == Material.LAVA;
    }

    public static boolean isInWater(Player player) {
        Block b = player.getLocation().getBlock();
        if (b.getType() == Material.WATER) return true;
        return b.getRelative(0, 1, 0).getType() == Material.WATER;
    }

    public static boolean isInLava(Player player) {
        Block b = player.getLocation().getBlock();
        if (b.getType() == Material.LAVA) return true;
        return b.getRelative(0, 1, 0).getType() == Material.LAVA;
    }
}