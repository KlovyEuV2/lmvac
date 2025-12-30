package dev.lmv.lmvac.api.modules.checks.sprint.spam;

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
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@SettingCheck(
        value = "ActionsSpamA",
        cooldown = Cooldown.COOLDOWN,
        descType = DescType.ALPHA
)
public class ActionsSpamA extends Check implements PacketCheck {
    public static double diff = 5;

    public ActionsSpamA(Plugin plugin) {
        super(plugin);
    }

    public void onPacketReceiving(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        Player player = event.getPlayer();
        PacketType packetType = event.getPacketType();

        int id = event.getPlayer().getEntityId();
        LmvPlayer client = LmvPlayer.get(player);

        long now = System.currentTimeMillis();

        if (client == null || !client.isInCombat()) return;
        if (packetType == PacketType.Play.Client.ENTITY_ACTION) {
            EnumWrappers.PlayerAction action = (EnumWrappers.PlayerAction)event.getPacket().getPlayerActions().read(0);
            switch (action) {
                case START_SPRINTING:
                case STOP_SPRINTING:
                    if (now - client.lastSprint <= diff) {
                        flag(player,"sprint");
                    }
                    break;
            }
        }
    }

    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().types(new PacketType[]{PacketType.Play.Client.ENTITY_ACTION}).build();
    }
}
