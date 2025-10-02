package dev.lmv.lmvac.api.modules.checks.badpackets.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Client;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.lmv.lmvac.api.implement.utils.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

// Обычный чек на FastSwap
@SettingCheck(
   value = "BadPacketsA",
   cooldown = Cooldown.COOLDOWN
)
public class BadPacketsA extends Check implements PacketCheck {
   private final ConcurrentHashMap<UUID,ChangeData> lastSlotChange = new ConcurrentHashMap();

   public BadPacketsA(Plugin plugin) {
      super(plugin);
   }

   public void onPacketReceiving(PacketEvent packetEvent) {
      Player player = packetEvent.getPlayer();
      PacketType packetType = packetEvent.getPacketType();
      UUID uuid = player.getUniqueId();
      int id = player.getEntityId();
      LmvPlayer targetPlayer = (LmvPlayer)LmvPlayer.players.get(id);
      if (targetPlayer != null) {
         long now = System.currentTimeMillis();
         if (!player.isInsideVehicle()) {
            if (!targetPlayer.hasBypass(this.getName())) {
               if (packetType == Client.HELD_ITEM_SLOT) {
                  PacketContainer packet = packetEvent.getPacket();
                  int newSlot = (Integer)packet.getIntegers().read(0);
                  int currentSlot = player.getInventory().getHeldItemSlot();
                  ChangeData changeData = new ChangeData(currentSlot, newSlot);
                  ChangeData lastData = (ChangeData)this.lastSlotChange.get(uuid);
                  ItemStack lastItem = null;
                  ItemStack currItem = player.getInventory().getItem(currentSlot);
                  if (lastData != null && lastData.from == changeData.to && changeData.time - lastData.time < 2L) {
                     long diff = changeData.time - lastData.time;
                     lastItem = player.getInventory().getItem(lastData.from);
                      String reason = locales.getOrDefault("1", "Suspicious slot movement in inventory. DFrom[%0] -> LTo[%1] / NSlot[%2] in %3 ms.");
                      String pReason = reason
                              .replace("%0", String.valueOf(lastData.from))
                              .replace("%1", String.valueOf(lastData.to))
                              .replace("%2", String.valueOf(newSlot))
                              .replace("%3", String.valueOf(diff));
                      this.flag(player,pReason);
                  }

                   this.lastSlotChange.put(uuid, changeData);
               }

            }
         }
      }
   }

   public ListeningWhitelist getReceivingWhitelist() {
      return ListeningWhitelist.newBuilder().types(new PacketType[]{Client.HELD_ITEM_SLOT}).build();
   }

   public static class ChangeData {
      public int from = -1;
      public int to = -1;
      public long time = -1L;

      public ChangeData(int from, int to) {
         this.from = from;
         this.to = to;
         this.time = System.currentTimeMillis();
      }
   }
}
