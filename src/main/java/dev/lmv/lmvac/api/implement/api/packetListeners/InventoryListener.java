package dev.lmv.lmvac.api.implement.api.packetListeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class InventoryListener implements PacketListener {
    static Plugin plugin;
    public static int iMoves = 5;
    public static boolean updClose = true;
    public InventoryListener(Plugin plugin) {
        this.plugin = plugin;
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
        reload();
    }
    public static void reload() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("checks.invL");
        try {
            iMoves = section.getInt("position-reset",5);
            updClose = section.getBoolean("upd-close",true);
        } catch (Exception e) {
            iMoves = 5;
            updClose = true;
        }
    }
    @Override
    public void onPacketSending(PacketEvent packetEvent) {
        Player player = packetEvent.getPlayer();
        PacketType type = packetEvent.getPacketType();

        int id = player.getEntityId();
        LmvPlayer client = LmvPlayer.players.get(id);
        if (client == null) return;

        if (type == PacketType.Play.Server.OPEN_WINDOW) {
            client.isServerInventoryOpened = true;
            client.isInventoryOpened = true;
        } else if (type == PacketType.Play.Server.CLOSE_WINDOW) {
            client.isServerInventoryOpened = false;
            client.isInventoryOpened = false;
            client.inventoryMoves = -1;
            client.inventoryMovesP.clear();
            if (updClose) {
                player.updateInventory();
            }
        } else if (type == PacketType.Play.Server.RESPAWN) {
            client.isServerInventoryOpened = false;
            client.isInventoryOpened = false;
            client.inventoryMoves = -1;
            client.inventoryMovesP.clear();
            if (updClose) {
                player.updateInventory();
            }
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        Player player = packetEvent.getPlayer();
        PacketType type = packetEvent.getPacketType();

        int id = player.getEntityId();
        LmvPlayer client = LmvPlayer.players.get(id);
        if (client == null) return;

        if (type == PacketType.Play.Client.WINDOW_CLICK) {
            client.isInventoryOpened = true;
        } else if (type == PacketType.Play.Client.CLOSE_WINDOW) {
            client.isInventoryOpened = false;
            client.inventoryMoves = -1;
            client.inventoryMovesP.clear();
        }
    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return ListeningWhitelist.newBuilder().types(
                PacketType.Play.Server.OPEN_WINDOW,
                PacketType.Play.Server.CLOSE_WINDOW,
                PacketType.Play.Server.POSITION,
                PacketType.Play.Server.RESPAWN
        ).build();
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().types(
                PacketType.Play.Client.WINDOW_CLICK,
                PacketType.Play.Client.CLOSE_WINDOW
        ).build();
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }
}
