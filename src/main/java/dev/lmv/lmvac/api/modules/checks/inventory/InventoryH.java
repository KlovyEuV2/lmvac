package dev.lmv.lmvac.api.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.DescType;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import dev.lmv.lmvac.api.implement.utils.inventory.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SettingCheck(
        value = "InventoryH",
        cooldown = Cooldown.NO_COOLDOWN,
        descType = DescType.ALPHA
)
public class InventoryH extends Check implements PacketCheck {
    public static ConcurrentHashMap<UUID,Boolean> flag = new ConcurrentHashMap<>();
    public static long maxDiff = 0;

    public InventoryH(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PacketType packetType = event.getPacketType();

        LmvPlayer client = LmvPlayer.get(player);
        if (client == null) return;

        long now = System.currentTimeMillis();

        if (packetType.equals(PacketType.Play.Client.CLOSE_WINDOW)) {
            try {
                if (now - client.lastWindowClick <= maxDiff) {
                    flag.put(player.getUniqueId(),true);
                    flag(player);
                }
            } catch (Exception ignored) {}
        } else if (packetType.equals(PacketType.Play.Client.WINDOW_CLICK)) {
            if (flag.containsKey(player.getUniqueId())
                    && flag.get(player.getUniqueId())) {
                event.setCancelled(true);
                InventoryUtil.updateSlot(client,event);
                flag.remove(player.getUniqueId());
            }
        }
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder()
                .types(PacketType.Play.Client.CLOSE_WINDOW,
                        PacketType.Play.Client.WINDOW_CLICK)
                .build();
    }
}