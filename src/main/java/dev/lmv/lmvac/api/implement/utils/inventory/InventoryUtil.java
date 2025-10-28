package dev.lmv.lmvac.api.implement.utils.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.LmvAC;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.api.packetListeners.InventoryListener;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ConcurrentHashMap;

public class InventoryUtil {

    private static final ConcurrentHashMap<LmvPlayer, ConcurrentHashMap<Integer, ItemStack>> lastSlotItems = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<LmvPlayer, ConcurrentHashMap<Integer, Long>> lastUpdateTime = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<LmvPlayer, ConcurrentHashMap<Integer, Long>> dynamicDebounce = new ConcurrentHashMap<>();

    private static final long MIN_DEBOUNCE = 30;
    private static final long MAX_DEBOUNCE = 200;

    public static boolean isInventoryClick(Player player, int slot, boolean cursor) {
        if (player == null) return false;
        if (slot == -999) return false;
        if (!cursor && !player.getItemOnCursor().getType().equals(Material.AIR)) return false;
        return slot >= 0;
    }

    public static void updateSlot(LmvPlayer client, PacketEvent event) {
        if (!InventoryListener.updateInventory || client.isServerInventoryOpened) return;

        try {
            Integer windowId = event.getPacket().getIntegers().readSafely(0);
            Integer slot = event.getPacket().getIntegers().readSafely(1);

            if (windowId == null || slot == null) {
                return;
            }

            if (windowId != 0) return;

            ItemStack clickedItem = event.getPacket().getItemModifier().readSafely(0);

            ConcurrentHashMap<Integer, ItemStack> playerSlots =
                    lastSlotItems.computeIfAbsent(client, k -> new ConcurrentHashMap<>());
            ConcurrentHashMap<Integer, Long> playerTimes =
                    lastUpdateTime.computeIfAbsent(client, k -> new ConcurrentHashMap<>());
            ConcurrentHashMap<Integer, Long> playerDebounce =
                    dynamicDebounce.computeIfAbsent(client, k -> new ConcurrentHashMap<>());

            ItemStack lastItem = playerSlots.get(slot);
            long now = System.currentTimeMillis();
            Long lastTime = playerTimes.get(slot);
            long debounce = playerDebounce.getOrDefault(slot, MIN_DEBOUNCE);

            if (lastItem != null && lastItem.equals(clickedItem) && lastTime != null && now - lastTime < debounce) {
                return;
            }

            playerSlots.put(slot, clickedItem);
            playerTimes.put(slot, now);

            if (lastTime != null) {
                long delta = now - lastTime;
                if (delta < MIN_DEBOUNCE) delta = MIN_DEBOUNCE;
                long newDebounce = Math.min(delta, MAX_DEBOUNCE);
                playerDebounce.put(slot, newDebounce);
            } else {
                playerDebounce.put(slot, MIN_DEBOUNCE);
            }

            sendSlot(client, slot, clickedItem);

        } catch (Exception ex) {
            LmvAC.instance.getLogger().warning("Ошибка при обработке пакета в updateSlot: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static void sendSlot(LmvPlayer client, int slot, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        try {
            PacketContainer setSlot = new PacketContainer(PacketType.Play.Server.SET_SLOT);
            setSlot.getIntegers().write(0, 0);
            setSlot.getIntegers().write(1, slot);
            setSlot.getItemModifier().write(0, item);
            ProtocolLibrary.getProtocolManager().sendServerPacket(client.player, setSlot);
        } catch (Exception ex) {
            LmvAC.instance.getLogger().warning("Ошибка при отправке SET_SLOT пакета: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}