package dev.lmv.lmvac.api.modules.checks.autoclicker.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import dev.lmv.lmvac.api.implement.utils.inventory.InventoryUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SettingCheck(
        value = "ClickSpamA",
        cooldown = Cooldown.NO_COOLDOWN
)
public class ClickSpamA extends Check implements PacketCheck {
    public static ConcurrentHashMap<UUID, ClickData> lastClicks = new ConcurrentHashMap<>();

    private static Long max_diff = 30L;

    public ClickSpamA(Plugin plugin) {
        super(plugin);
        reloadCfg(plugin);
    }

    public static class ClickData {
        public long time;

        public ClickData() {
            time = System.currentTimeMillis();
        }
    }

    public static void reloadCfg(Plugin plugin) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("checks.autoclicker.ClickSpamA");
        try {
            max_diff = section.getLong("diff", 30L);
        } catch (Exception e) {
            max_diff = 30L;
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PacketType packetType = event.getPacketType();
        PacketContainer packet = event.getPacket();
        long now = System.currentTimeMillis();

        if (packetType.equals(PacketType.Play.Client.WINDOW_CLICK)) {
            int slot = packet.getIntegers().read(1);

            if (slot == -999) return;

            int mode = -1;
            try {
                mode = packet.getIntegers().read(4);
            } catch (Exception ignored) {}
            if (mode == 4 || mode == -1) return;

            int id = player.getEntityId();
            LmvPlayer client = LmvPlayer.players.get(id);
            if (client == null) return;

            ClickData lastClick = lastClicks.get(player.getUniqueId());

            if (lastClick == null) {
                lastClicks.put(player.getUniqueId(), new ClickData());
                return;
            }

            double diff = now - lastClick.time;

            if (diff <= max_diff && InventoryUtil.isInventoryClick(player, slot, false)) {
                event.setCancelled(true);
                flag(player);
            }

            lastClicks.put(player.getUniqueId(), new ClickData());
        }
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().types(PacketType.Play.Client.WINDOW_CLICK).build();
    }
}