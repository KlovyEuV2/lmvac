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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Чек на очень частые быстрые клики в инвентаре / InventoryAutoClicker / GuiMoveFunTime/SpookyTime?
@SettingCheck(
        value = "ClickSpamA",
        cooldown = Cooldown.NO_COOLDOWN
)
public class ClickSpamA extends Check implements PacketCheck {
    public static ConcurrentHashMap<UUID,ClickData> lastClicks = new ConcurrentHashMap<>();
    public ClickSpamA(Plugin plugin) {
        super(plugin);
    }
    public static class ClickData {
        public long time;
        public ItemStack item;
        public ClickData(ItemStack item) {
            time = System.currentTimeMillis();
            this.item = item;
        }
    }
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PacketType packetType = event.getPacketType();
        PacketContainer packet = event.getPacket();
        long now = System.currentTimeMillis();
        if (packetType.equals(PacketType.Play.Client.WINDOW_CLICK)) {
            int slot = event.getPacket().getIntegers().read(1);
            ItemStack clickedItem = event.getPacket().getItemModifier().readSafely(0);
            int id = event.getPlayer().getEntityId();
            LmvPlayer client = (LmvPlayer) LmvPlayer.players.get(id);
            if (client == null) return;
            ClickData lastClick = null;
            if (lastClicks.containsKey(player.getUniqueId())) {
                lastClick = lastClicks.get(player.getUniqueId());
            }
            if (lastClick == null) {
                lastClicks.put(player.getUniqueId(),new ClickData(clickedItem));
                return;
            }
            double diff = -404;
            if (lastClick.time > -404) diff = now-lastClick.time;
            if (diff > -404 && diff <= 30) {
                aFlag(event,player);
            }
            lastClicks.put(player.getUniqueId(),new ClickData(clickedItem));
        }
    }
    public void aFlag(PacketEvent event, Player player) {
        event.setCancelled(true);
        flag(player);
    }
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().types(new PacketType[]{PacketType.Play.Client.WINDOW_CLICK}).build();
    }
}
