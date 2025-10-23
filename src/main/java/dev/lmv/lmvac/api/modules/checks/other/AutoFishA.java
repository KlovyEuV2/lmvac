package dev.lmv.lmvac.api.modules.checks.other;

import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.BukkitCheck;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SettingCheck(
        value = "AutoFishA",
        cooldown = Cooldown.NO_COOLDOWN
)
public class AutoFishA extends Check implements BukkitCheck {
    public AutoFishA(Plugin plugin) {
        super(plugin);
    }
    public static ConcurrentHashMap<UUID,Boolean> detects = new ConcurrentHashMap<>();
    @EventHandler
    private void checkA(PlayerFishEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        LmvPlayer client = LmvPlayer.players.get(player.getEntityId());
        if (client == null) return;
        long now = System.currentTimeMillis();
        if (!(event.getState() == PlayerFishEvent.State.CAUGHT_FISH || event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY )) return;
        if (detects.getOrDefault(player.getUniqueId(),false)) { event.setCancelled(true); detects.remove(player.getUniqueId()); }
        Bukkit.getScheduler().runTaskAsynchronously(plugin,() -> {
            if (!player.isOnline()) return;
            if (now-client.lastArmAnimation > 90) {
                detects.put(player.getUniqueId(),true);
                flag(player);
            }
        });
    }
}
