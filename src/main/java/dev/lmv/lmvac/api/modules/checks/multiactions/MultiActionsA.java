package dev.lmv.lmvac.api.modules.checks.multiactions;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import dev.lmv.lmvac.api.implement.utils.FluidUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

// Бить когда ешь.
@SettingCheck(value = "MultiActionsA", cooldown = Cooldown.COOLDOWN)
public class MultiActionsA extends Check implements PacketCheck {
    public MultiActionsA(Plugin plugin) {
        super(plugin);
    }
    public void onPacketReceiving(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        Player player = event.getPlayer();
        PacketType packetType = event.getPacketType();

        int id = event.getPlayer().getEntityId();
        LmvPlayer client = LmvPlayer.players.get(id);

        if (client == null) return;
        if (packetType == PacketType.Play.Client.USE_ENTITY) {
            try {
                EnumWrappers.EntityUseAction action = packet.getEntityUseActions().readSafely(0);
                if (action == EnumWrappers.EntityUseAction.ATTACK) {
                    if (player.isHandRaised()) {
                        event.setCancelled(true);
                        flag(player);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
//    public static boolean isCorrectMovementPacket(PacketEvent event) {
//        Player player = event.getPlayer();
//        double x = event.getPacket().getDoubles().read(0);
//        double y = event.getPacket().getDoubles().read(1);
//        double z = event.getPacket().getDoubles().read(2);
//        double deltaX = Math.abs(x - player.getLocation().getX());
//        double deltaY = Math.abs(y - player.getLocation().getY());
//        double deltaZ = Math.abs(z - player.getLocation().getZ());
//        return !(deltaX < 0.001 && deltaY < 0.001 && deltaZ < 0.001);
//    }
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().types(new PacketType[]{PacketType.Play.Client.USE_ENTITY}).build();
    }
}
