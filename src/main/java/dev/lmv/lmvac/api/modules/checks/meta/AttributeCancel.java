package dev.lmv.lmvac.api.modules.checks.meta;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

// Отменяет пакет узнавания атрибутов (от <не бить голых>, а также что-бы игроки били нпс)
@SettingCheck(
        value = "AttributeCancel",
        cooldown = Cooldown.COOLDOWN
)
public class AttributeCancel extends Check implements PacketCheck {
    public AttributeCancel(Plugin plugin) {
        super(plugin);
    }
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        PacketType packetType = event.getPacketType();
        PacketContainer packet = event.getPacket();
        if (packet != null && !packet.getIntegers().getValues().isEmpty()) {
            int entityId = packet.getIntegers().read(0);
            if (entityId != player.getEntityId()) {
                if (packetType == PacketType.Play.Server.UPDATE_ATTRIBUTES) {
                    event.setCancelled(true);
                }
            }
        }
    }
    public ListeningWhitelist getSendingWhitelist() {
        try {
            return ListeningWhitelist.newBuilder()
                    .types(PacketType.Play.Server.UPDATE_ATTRIBUTES)
                    .build();
        } catch (Exception e) {
            return ListeningWhitelist.EMPTY_WHITELIST;
        }
    }
}
