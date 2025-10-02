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
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

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

        if (!client.isMovePacketing() && !client.isLastLagging) {
            flag(player);
        }
    }

    public ListeningWhitelist getSendingWhitelist() {
        return ListeningWhitelist.newBuilder().types(new PacketType[]{PacketType.Play.Server.UPDATE_TIME}).build();
    }
}
