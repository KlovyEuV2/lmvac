package dev.lmv.lmvac.api.implement.utils.inventory;

import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.LmvAC;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.api.packetListeners.InventoryListener;
import dev.lmv.lmvac.api.implement.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryUtil implements Listener {
    private static final ConcurrentHashMap<UUID, ConcurrentHashMap<Integer, ItemStack>> lastSlotItems = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, ConcurrentHashMap<Integer, Long>> lastUpdateTime = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, ConcurrentHashMap<Integer, Long>> dynamicDebounce = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> lastWindowSync = new ConcurrentHashMap<>();

    private static final long MIN_DEBOUNCE = 30L;
    private static final long MAX_DEBOUNCE = 200L;

    public InventoryUtil(LmvAC instance) {
        Bukkit.getPluginManager().registerEvents(this,instance);
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        clearData(player);
    }

    public void clearData(Player player) {
        lastSlotItems.remove(player.getUniqueId());
        lastUpdateTime.remove(player.getUniqueId());
        dynamicDebounce.remove(player.getUniqueId());
        lastWindowSync.remove(player.getUniqueId());
    }

    public static boolean isInventoryClick(Player player, int slot, boolean cursor) {
        if (player == null) {
            return false;
        } else if (slot == -999) {
            return false;
        } else if (!cursor && !player.getItemOnCursor().getType().equals(Material.AIR)) {
            return false;
        } else {
            return slot >= 0;
        }
    }

    public static void updateSlot(LmvPlayer client, PacketEvent event) {
        if (!InventoryListener.updateInventory || client.isServerInventoryOpened) return;

        try {
            Integer windowId = event.getPacket().getIntegers().readSafely(0);
            Integer slot = event.getPacket().getIntegers().readSafely(1);
            ItemStack clickedItem = event.getPacket().getItemModifier().readSafely(0);

            if (windowId == null || slot == null || windowId != 0) return;
            if (clickedItem == null) clickedItem = new ItemStack(Material.AIR);

            UUID playerId = client.player.getUniqueId();
            ConcurrentHashMap<Integer, ItemStack> playerSlots = lastSlotItems.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
            ConcurrentHashMap<Integer, Long> playerTimes = lastUpdateTime.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
            ConcurrentHashMap<Integer, Long> playerDebounce = dynamicDebounce.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());

            ItemStack lastItem = playerSlots.get(slot);
            long now = System.currentTimeMillis();
            Long lastTime = playerTimes.get(slot);
            long debounce = playerDebounce.getOrDefault(slot, MIN_DEBOUNCE);

            if (lastItem != null && lastItem.equals(clickedItem) && lastTime != null && now - lastTime < debounce) {
                return;
            }

            playerSlots.put(slot, clickedItem);
            playerTimes.put(slot, now);
            long delta = (lastTime != null) ? Math.max(now - lastTime, MIN_DEBOUNCE) : MIN_DEBOUNCE;
            playerDebounce.put(slot, Math.min(delta, MAX_DEBOUNCE));

            sendSlot(client, slot, clickedItem);

        } catch (Exception ex) {
            LmvAC.instance.getLogger().warning("Ошибка при обработке пакета в updateSlot: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static void sendSlot(LmvPlayer client, int slot, ItemStack item) {
        try {
            PacketContainer setSlot = new PacketContainer(Server.SET_SLOT);

            setSlot.getIntegers().writeSafely(0, 0);

            if (VersionUtil.isVersionAtLeast("1.17")) {
                setSlot.getIntegers().writeSafely(1, -1);
                setSlot.getIntegers().writeSafely(2, slot);
            } else {
                setSlot.getIntegers().writeSafely(1, slot);
            }

            setSlot.getItemModifier().writeSafely(0, item);

            ProtocolLibrary.getProtocolManager().sendServerPacket(client.player, setSlot);

        } catch (Exception ex) {
            LmvAC.instance.getLogger().warning("Ошибка при отправке SET_SLOT пакета: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}