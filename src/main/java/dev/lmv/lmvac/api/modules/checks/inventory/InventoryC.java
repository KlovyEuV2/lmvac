package dev.lmv.lmvac.api.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.api.settings.LocaleManager;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import dev.lmv.lmvac.api.implement.utils.TimeUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Много-кратные быстрые и резкие клики после сброса спринта
@SettingCheck(
        value = "InventoryC",
        cooldown = Cooldown.NO_COOLDOWN
)
public class InventoryC extends Check implements PacketCheck {
    public InventoryC(Plugin plugin) {
        super(plugin);
        reloadCfg(plugin);
    }

    public static ConcurrentHashMap<UUID, ClickData> lastClicks = new ConcurrentHashMap<>();

    public static long sprintTime = 599;
    public static long diffClicks = 20;

    public static class ClickData {
        public long time = -1;
        public ItemStack item = null;

        public ClickData(ItemStack item) {
            this.time = System.currentTimeMillis();
            this.item = item;
        }
    }

    public static void reloadCfg(Plugin plugin) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("checks.inventory.c");
        try {
            if (section != null) {
                sprintTime = section.getLong("sprint", 599);
                diffClicks = section.getLong("diff", 20);
            } else {
                sprintTime = 599;
                diffClicks = 20;
            }
        } catch (Exception e) {
            sprintTime = 599;
            diffClicks = 20;
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        Player player = packetEvent.getPlayer();
        PacketType type = packetEvent.getPacketType();
        int id = player.getEntityId();
        LmvPlayer client = LmvPlayer.players.get(id);

        if (client == null) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (type == PacketType.Play.Client.WINDOW_CLICK) {
            try {
                int slot = packetEvent.getPacket().getIntegers().read(1);
                ItemStack clickedItem = packetEvent.getPacket().getItemModifier().readSafely(0);

                if (!lastClicks.containsKey(uuid)) {
                    lastClicks.put(uuid, new ClickData(clickedItem));
                    return;
                }

                if (now - client.lastSprint > sprintTime) {
                    lastClicks.put(uuid, new ClickData(clickedItem));
                    return;
                }

                ClickData lastData = lastClicks.get(uuid);
                if (lastData == null) {
                    lastClicks.put(uuid, new ClickData(clickedItem));
                    return;
                }

                long timeDiff = now - lastData.time;

                if (timeDiff <= diffClicks) {
                    String unknown = LocaleManager.current_.getString("unknown","unknown");
                    String lastSprint = client.lastSprint > 0 ?
                            String.valueOf(TimeUtil.formatDuration(now - client.lastSprint)) :
                            unknown;

                    String reason = locales.getOrDefault("1","Suspicious multi-clicks in inventory. Sprint[%0] / TimeDiff[%1ms].");
                    String pReason = reason
                            .replaceAll("%0",lastSprint)
                            .replaceAll("%1", String.valueOf(timeDiff));


                    packetEvent.setCancelled(true);
                    flag(player, pReason);
                }

                lastClicks.put(uuid, new ClickData(clickedItem));
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка в BadPacketsC: " + e.getMessage());
            }
        }
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder()
                .types(PacketType.Play.Client.WINDOW_CLICK)
                .build();
    }
}