package dev.lmv.lmvac.api.implement.api.lmvEvents.api.listeners;

import dev.lmv.lmvac.api.implement.api.lmvEvents.events.flag.FlagEvent;
import dev.lmv.lmvac.api.implement.api.lmvEvents.events.flag.FlagType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ClientListener implements Listener {
    Plugin plugin;
    public ClientListener(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this,plugin);
    }

    @EventHandler
    public void onFlag(FlagEvent event) {
        if (event.getFlagType() == FlagType.MOVEMENT_ONE) {
            sendFlagPacket(event, null);
        } else if (event.getFlagType() == FlagType.MOVEMENT_HARD) {
            sendFlagPacket(event,null);
            new BukkitRunnable() {
                @Override
                public void run() {
                    sendFlagPacket(event, this);
                }
            }.runTaskTimer(plugin,2L,1L);
        }
    }

    private boolean sendFlagPacket(FlagEvent event, BukkitRunnable runnable) {
        Player player = event.getPlayer();
        Vector velocity = event.getFlagVelocity();
        player.setVelocity(velocity);
        event.count++;
        if (runnable != null && event.count>=event.getFlagsCount()) runnable.cancel();
        return true;
    }
}
