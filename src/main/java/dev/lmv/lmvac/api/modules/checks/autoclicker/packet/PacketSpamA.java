package dev.lmv.lmvac.api.modules.checks.autoclicker.packet;

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
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SettingCheck(value = "PacketSpamA", cooldown = Cooldown.NO_COOLDOWN, descType = DescType.NEW)
public class PacketSpamA extends Check implements PacketCheck {
    public static ConcurrentHashMap<UUID, List<DropData>> drops = new ConcurrentHashMap<>();
    public static Long maxDiff = 1000L;
    public static Integer maxTimes = 20;

    public static class DropData {
        public Player player;
        public PacketEvent packetEvent;
        public final Long time;

        public DropData(Player player, PacketEvent packetEvent) {
            this.player = player;
            this.packetEvent = packetEvent;
            this.time = System.currentTimeMillis();
        }
    }

    public static void reloadCfg(Plugin plugin) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("checks.autoclicker.PacketSpamA");
        try {
            maxDiff = section.getLong("per",1000L);
            maxTimes = section.getInt("clicks",20);
        } catch (Exception e) {
            maxDiff = 1000L;
            maxTimes = 20;
        }
    }

    public PacketSpamA(Plugin plugin) {
        super(plugin);
        reloadCfg(plugin);
    }

    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PacketType packetType = event.getPacketType();
        PacketContainer packet = event.getPacket();

        if (packetType.equals(PacketType.Play.Client.WINDOW_CLICK)) {
            try {
                if (player.getItemOnCursor().getType() != Material.AIR) return;
                List<DropData> dropDataList = drops.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
                long now = System.currentTimeMillis();

                LmvPlayer client = LmvPlayer.players.get(player.getEntityId());
                if (client == null) return;

                dropDataList.removeIf(data -> now - data.time > maxDiff);

                DropData newDropData = new DropData(player, event);
                dropDataList.add(newDropData);

                if (dropDataList.size() >= maxTimes) {
                    aFlag(event, player);
                    if (!client.isServerInventoryOpened) player.updateInventory();
                }

                if (dropDataList.size() > 50) dropDataList.remove(0);
            } catch (Exception ignored) {}
        }
    }

    public void aFlag(PacketEvent event, Player player) {
        event.setCancelled(true);
        flag(player);
    }

    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder()
                .types(PacketType.Play.Client.WINDOW_CLICK)
                .build();
    }
}