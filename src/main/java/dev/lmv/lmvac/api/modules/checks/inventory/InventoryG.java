package dev.lmv.lmvac.api.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Резкие Клики после движения/во время движения.
@SettingCheck(
        value = "InventoryG",
        cooldown = Cooldown.NO_COOLDOWN
)
public class InventoryG extends Check implements PacketCheck {
    public InventoryG(Plugin plugin) {
        super(plugin);
        reloadCfg(plugin);
    }
    public static ConcurrentHashMap<UUID, ClickData> lastClicks = new ConcurrentHashMap<>();
    private static long threshold = 50;
    public static long diffClicks = 20;
    public static void reloadCfg(Plugin plugin) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("checks.inventory.g");
        try {
            threshold = section.getLong("threshold",50);
            diffClicks = section.getLong("diff", 20);
        } catch (Exception e) {
            threshold = 50;
            diffClicks = 20;
        }
    }
    public static class ClickData {
        public long time = -1;
        public ItemStack item = null;

        public ClickData(ItemStack item) {
            this.time = System.currentTimeMillis();
            this.item = item;
        }
    }
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PacketType packetType = event.getPacketType();
        PacketContainer packet = event.getPacket();
        if (packetType.equals(PacketType.Play.Client.WINDOW_CLICK)) {
            try {
                int slot = event.getPacket().getIntegers().read(1);
                ItemStack clickedItem = event.getPacket().getItemModifier().readSafely(0);
                int id = event.getPlayer().getEntityId();
                LmvPlayer client = (LmvPlayer) LmvPlayer.players.get(id);
                if (client == null) return;
                UUID uuid = player.getUniqueId();
                long now = System.currentTimeMillis();
                double diffPLook = now-client.lastPositionLook;
                double diffLook = now-client.lastLook;
                double diffUseEntity = now-client.lastUseEntity;
                double diffEntityAction = now-client.lastEntityAction;
                double diffUseItem = now-client.lastUseItem;
                boolean isLastMoved = diffPLook <= threshold || diffLook <= threshold || diffUseEntity <= threshold ||
                        diffEntityAction <= threshold || diffUseItem <= threshold;
                ClickData lastData = lastClicks.get(uuid);
                if (lastData == null) {
                    lastClicks.put(uuid, new ClickData(clickedItem));
                    return;
                }

                long timeDiff = now - lastData.time;
                if (timeDiff <= diffClicks) {
                    if (isLastMoved) {
                        event.setCancelled(true);
                        flag(player);
                    }
                }
                lastClicks.put(uuid, new ClickData(clickedItem));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().types(new PacketType[]{PacketType.Play.Client.WINDOW_CLICK}).build();
    }
}
