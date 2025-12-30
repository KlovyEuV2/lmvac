package dev.lmv.lmvac.api.modules.checks.aura;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.DescType;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@SettingCheck(
        value = "AuraA",
        cooldown = Cooldown.COOLDOWN,
        descType = DescType.BETA
)
public class AuraA extends Check implements PacketCheck {
    public static long MAX_ANIMATION_DIFF = 50L;
    public AuraA(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PacketContainer packet = event.getPacket();
        PacketType packetType = event.getPacketType();
        LmvPlayer client = LmvPlayer.get(player);
        if (client == null) return;
        if (packetType.equals(PacketType.Play.Client.USE_ENTITY)) {
            try {
                EnumWrappers.EntityUseAction action = packet.getEntityUseActions().readSafely(0);
                if (action.equals(EnumWrappers.EntityUseAction.ATTACK)) {
                    Bukkit.getScheduler().runTask(plugin,() -> {
                        long now1 = System.currentTimeMillis();
                        long diff = now1-client.lastArmAnimation;
                        if (diff > MAX_ANIMATION_DIFF) {
                            flag(player);
                        }
                    });
                }
            } catch (Exception ignored) {}
        }
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().types(PacketType.Play.Client.USE_ENTITY).build();
    }
}
