package dev.lmv.lmvac.api.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.DescType;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import dev.lmv.lmvac.api.implement.utils.simulation.MovementUtil;
import dev.lmv.lmvac.api.implement.utils.inventory.InventoryUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SettingCheck(
        value = "InventoryB",
        cooldown = Cooldown.NO_COOLDOWN,
        descType = DescType.ALPHA,
        description = "Optimized InventoryMove/Click Detection"
)
public class InventoryB extends Check implements PacketCheck {
    public InventoryB(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.WINDOW_CLICK) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        LmvPlayer client = LmvPlayer.get(player);
        if (client == null || client.isFluid || client.isGliding || player.isInsideVehicle()) return;

        boolean moving = client.hasSprint;
        if (moving) {
            event.setCancelled(true);
            InventoryUtil.updateSlot(client, event);
            this.flag(player, "suspicious click in inventory during movement");
        }
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder()
                .types(PacketType.Play.Client.WINDOW_CLICK)
                .build();
    }
}
