package dev.lmv.lmvac.api.implement.utils.simulation;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

public class CollideUtil {

    public static boolean isColliding(Player player) {
        BoundingBox playerBox = player.getBoundingBox();
        return isCollidingWithSolidBlocks(player.getWorld(), playerBox);
    }

    private static boolean isCollidingWithSolidBlocks(World world, BoundingBox playerBox) {
        int minX = (int) Math.floor(playerBox.getMinX());
        int minY = (int) Math.floor(playerBox.getMinY());
        int minZ = (int) Math.floor(playerBox.getMinZ());
        int maxX = (int) Math.floor(playerBox.getMaxX());
        int maxY = (int) Math.floor(playerBox.getMaxY());
        int maxZ = (int) Math.floor(playerBox.getMaxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);

                    if (block.getType().isSolid()) {
                        if (block.getBoundingBox().overlaps(playerBox)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}