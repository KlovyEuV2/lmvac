package dev.lmv.lmvac.api.implement.api.packetListeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.temporary.TemporaryPlayer;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class InventoryListener{
    static Plugin plugin;
    public static int iMoves = 5;
    public static boolean updClose = true;
    public static boolean updateInventory = true;
    public InventoryListener(Plugin plugin) {
        InventoryListener.plugin = plugin;
        reload();
    }
    public static void reload() {
        ConfigurationSection inventory = plugin.getConfig().getConfigurationSection("checks.inventory");
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("checks.invL");
        try {
            iMoves = section.getInt("position-reset",5);
            updClose = section.getBoolean("upd-close",true);
            updateInventory = inventory.getBoolean("update-inventory", true);
        } catch (Exception e) {
            iMoves = 5;
            updClose = true;
            updateInventory = true;
        }
    }
    public static void onPacketSending(PacketEvent event) {
        if (event.getPlayer() == null || !event.getPlayer().isOnline() || (event.getPlayer() instanceof TemporaryPlayer)) return;
        Player player = event.getPlayer();
        PacketType type = event.getPacketType();

        int id = player.getEntityId();
        LmvPlayer client = LmvPlayer.players.get(id);
        if (client == null) return;

        if (type == PacketType.Play.Server.OPEN_WINDOW) {
            client.invTick = 0;
            client.isServerInventoryOpened = true;
            client.isInventoryOpened = true;
            client.isWindowOpen = true;
        } else if (type == PacketType.Play.Server.CLOSE_WINDOW) {
            client.invTick = 0;
            client.isServerInventoryOpened = false;
            client.isInventoryOpened = false;
            client.inventoryMoves = -1;
            client.inventoryMovesP.clear();
            client.isSWindowClosing = true;
            if (updClose) {
                player.updateInventory();
            }
        } else if (type == PacketType.Play.Server.RESPAWN) {
            client.invTick = 0;
            client.isServerInventoryOpened = false;
            client.isInventoryOpened = false;
            client.inventoryMoves = -1;
            client.inventoryMovesP.clear();
            client.isRespawning = true;
            if (updClose) {
                player.updateInventory();
            }
        }
    }

    public static void onPacketReceiving(PacketEvent packetEvent) {
        if (packetEvent.getPlayer() == null || !packetEvent.getPlayer().isOnline() || (packetEvent.getPlayer() instanceof TemporaryPlayer)) return;
        Player player = packetEvent.getPlayer();
        PacketType type = packetEvent.getPacketType();

        int id = player.getEntityId();
        LmvPlayer client = LmvPlayer.players.get(id);
        if (client == null) return;

        if (type == PacketType.Play.Client.WINDOW_CLICK) {
            if (!client.isInventoryOpened) client.invTick = 0;
            client.isInventoryOpened = true;
        } else if (type == PacketType.Play.Client.CLOSE_WINDOW) {
            client.invTick = 0;
            client.isInventoryOpened = false;
            client.inventoryMoves = -1;
            client.inventoryMovesP.clear();
        }
    }
}
