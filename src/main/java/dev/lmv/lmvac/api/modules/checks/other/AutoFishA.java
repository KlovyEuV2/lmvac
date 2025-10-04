package dev.lmv.lmvac.api.modules.checks.other;

import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.BukkitCheck;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.plugin.Plugin;

@SettingCheck(
        value = "AutoFishA",
        cooldown = Cooldown.COOLDOWN
)
public class AutoFishA extends Check implements BukkitCheck {
    public AutoFishA(Plugin plugin) {
        super(plugin);
    }
    @EventHandler
    private void checkA(PlayerFishEvent event) {
        Player player = event.getPlayer();
        LmvPlayer client = LmvPlayer.players.get(player.getEntityId());
        if (client == null) return;
        long now = System.currentTimeMillis();
        if (now-client.lastArmAnimation > 50) {
            event.setCancelled(true);
            flag(player);
        }
    }
}
