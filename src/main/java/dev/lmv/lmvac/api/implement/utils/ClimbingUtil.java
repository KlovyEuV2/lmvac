package dev.lmv.lmvac.api.implement.utils;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.Set;

public class ClimbingUtil {

    private static final Set<Material> CLIMBABLE;

    static {
        CLIMBABLE = EnumSet.of(
                Material.LADDER,
                Material.VINE,
                Material.SCAFFOLDING,
                Material.WEEPING_VINES,
                Material.WEEPING_VINES_PLANT,
                Material.TWISTING_VINES,
                Material.TWISTING_VINES_PLANT
        );
    }

    public static boolean isClimbing(Player player) {
        Block b = player.getLocation().getBlock();
        if (CLIMBABLE.contains(b.getType())) return true;
        return CLIMBABLE.contains(b.getRelative(0, 1, 0).getType());
    }

    public static boolean isClimbingPrecise(Player player) {
        Block b = player.getLocation().getBlock();
        if (CLIMBABLE.contains(b.getType())) return true;

        Block h = b.getRelative(0, 1, 0);
        if (CLIMBABLE.contains(h.getType())) return true;

        if (CLIMBABLE.contains(b.getRelative(1, 0, 0).getType())) return true;
        if (CLIMBABLE.contains(b.getRelative(-1, 0, 0).getType())) return true;
        if (CLIMBABLE.contains(b.getRelative(0, 0, 1).getType())) return true;
        return CLIMBABLE.contains(b.getRelative(0, 0, -1).getType());
    }

    public static boolean isOnLadder(Player player) {
        Block b = player.getLocation().getBlock();
        if (b.getType() == Material.LADDER) return true;
        return b.getRelative(0, 1, 0).getType() == Material.LADDER;
    }

    public static boolean isOnVine(Player player) {
        Block b = player.getLocation().getBlock();
        Material m = b.getType();
        if (m == Material.VINE ||
                m == Material.WEEPING_VINES ||
                m == Material.WEEPING_VINES_PLANT ||
                m == Material.TWISTING_VINES ||
                m == Material.TWISTING_VINES_PLANT) return true;

        m = b.getRelative(0, 1, 0).getType();
        return m == Material.VINE ||
                m == Material.WEEPING_VINES ||
                m == Material.WEEPING_VINES_PLANT ||
                m == Material.TWISTING_VINES ||
                m == Material.TWISTING_VINES_PLANT;
    }

    public static boolean isOnScaffolding(Player player) {
        Block b = player.getLocation().getBlock();
        if (b.getType() == Material.SCAFFOLDING) return true;
        return b.getRelative(0, 1, 0).getType() == Material.SCAFFOLDING;
    }

    public static boolean isActivelyClimbing(Player player) {
        return isClimbing(player) && Math.abs(player.getVelocity().getY()) > 0.001;
    }

    public static boolean isClimbingUp(Player player) {
        return isClimbing(player) && player.getVelocity().getY() > 0.001;
    }

    public static boolean isClimbingDown(Player player) {
        return isClimbing(player) && player.getVelocity().getY() < -0.001;
    }

    public static boolean isClimbableBlock(Block block) {
        return CLIMBABLE.contains(block.getType());
    }
}