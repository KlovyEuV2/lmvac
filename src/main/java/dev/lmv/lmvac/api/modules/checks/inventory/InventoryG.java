package dev.lmv.lmvac.api.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.api.packetListeners.InventoryListener;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.DescType;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.Configurable;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import dev.lmv.lmvac.api.implement.utils.inventory.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Резкие клики после/во время движения (с %r reason).
@SettingCheck(
        value = "InventoryG",
        cooldown = Cooldown.NO_COOLDOWN,
        descType = DescType.ALPHA,
        description = "Fast inventory actions during or right after recent actions."
)
public class InventoryG extends Check implements PacketCheck, Configurable {

    public ConcurrentHashMap<UUID, ClickData> lastClicks = new ConcurrentHashMap<>();
    private boolean diffs = false;
    private long threshold = 50;
    public long diffClicks = 20;

    public InventoryG(Plugin plugin) {
        super(plugin);
        reloadConfiguration(plugin);
    }

    public void reloadConfiguration(Plugin plugin) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("checks.inventory.g");
        try {
            diffs = section.getBoolean("cSpam-required",true);
            threshold = section.getLong("threshold", 50);
            diffClicks = section.getLong("diff", 20);
        } catch (Exception e) {
            diffs = true;
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

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PacketType packetType = event.getPacketType();

        if (packetType.equals(PacketType.Play.Client.WINDOW_CLICK)) {
            try {
                int slot = event.getPacket().getIntegers().read(1);
                ItemStack clickedItem = event.getPacket().getItemModifier().readSafely(0);
                int id = player.getEntityId();
                LmvPlayer client = LmvPlayer.players.get(id);
                if (client == null) return;

                UUID uuid = player.getUniqueId();
                long now = System.currentTimeMillis();
                ClickData lastData = lastClicks.get(uuid);

                if (lastData == null) {
                    lastClicks.put(uuid, new ClickData(clickedItem));
                    return;
                }

                long timeDiff = now - lastData.time;
                if ((!diffs || timeDiff <= diffClicks) && isIsLastMoved(now,client,timeDiff)) {
                    String recentReason = getRecentActionReason(now, client);

                    if (recentReason != null) {
                        event.setCancelled(true);
                        String reason = locales.getOrDefault("1",
                                        "Fast inventory click with recent actions. %r")
                                .replace("%r", recentReason);
                        flag(player,reason);
                        InventoryUtil.updateSlot(client,event);
                    }
                }

                lastClicks.put(uuid, new ClickData(clickedItem));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isIsLastMoved(long now, LmvPlayer client, long timeDiff) {
        double diffPLook = now - client.lastPositionLook;
        double diffLook = now - client.lastLook;
        double diffUseEntity = now - client.lastUseEntity;
        double diffEntityAction = now - client.lastEntityAction;
        double diffUseItem = now - client.lastUseItem;
        return ((diffPLook <= threshold || diffLook <= threshold) && timeDiff <= diffClicks) || diffUseEntity <= threshold ||
                diffEntityAction <= threshold || diffUseItem <= threshold;
    }

    private String getRecentActionReason(long now, LmvPlayer client) {
        double diffPLook = now - client.lastPositionLook;
        double diffLook = now - client.lastLook;
        double diffUseEntity = now - client.lastUseEntity;
        double diffEntityAction = now - client.lastEntityAction;
        double diffUseItem = now - client.lastUseItem;

        StringBuilder sb = new StringBuilder();

        if (diffPLook <= threshold || diffLook <= threshold) sb.append("rotation ");
        if (diffUseEntity <= threshold) sb.append("interact ");
        if (diffEntityAction <= threshold) sb.append("entityaction ");
        if (diffUseItem <= threshold) sb.append("useitem ");

        return sb.length() > 0 ? sb.toString() : null;
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder()
                .types(PacketType.Play.Client.WINDOW_CLICK)
                .build();
    }
}
