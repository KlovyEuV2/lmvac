package dev.lmv.lmvac.api.implement.utils;

import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import org.bukkit.entity.Player;

public class PingUtil {
    public static boolean hasPingCons(LmvPlayer client, long def) {
        Player player = client.player;
        return def < (player.getPing() *1.5);
    }
}
