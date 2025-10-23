package dev.lmv.lmvac.api.modules.checks.order.actions;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import dev.lmv.lmvac.api.implement.utils.FluidUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.List;

// Ложные на 1.19+ клиентах, чек на стоп пакетов Movement / AirStuck / FreeCamera
@SettingCheck(
        value = "PacketOrderA",
        cooldown = Cooldown.COOLDOWN
)
public class PacketOrderA extends Check implements PacketCheck {
    public PacketOrderA(Plugin plugin) {
        super(plugin);
    }
    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        PacketType packetType = event.getPacketType();
        PacketContainer packet = event.getPacket();
        int id = player.getEntityId();
        LmvPlayer client = LmvPlayer.players.get(id);

        long now = System.currentTimeMillis();
        long tick = 50;

        Location from = player.getLocation();

//         1.16.4-1.16.5 и тд.
//        if (player.getProtocolVersion() != 754) return;
        if (!player.isOnGround() && (now-client.lastGround>tick && !client.isMovePacketing(1, tick*20)
                && !client.isLastLagging && player.isTicking())) {
            flag(player,locales.getOrDefault("1","Возможное подозрительное зависание в воздухе."));
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                if (!(player.isOnline() && player.isTicking())) return;
                Location to = player.getLocation();
                if (to.equals(from) && !client.isLastLagging) {
                    flag(player,locales.getOrDefault("2","Подозрительное поведение на Velocity от сервера при зависаний в воздухе (возможна отмена)."));
                }
            });
        }
    }

    public ListeningWhitelist getSendingWhitelist() {
        return ListeningWhitelist.newBuilder().types(new PacketType[]{PacketType.Play.Server.UPDATE_TIME}).build();
    }
}
