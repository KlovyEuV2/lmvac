package dev.lmv.lmvac.api.modules.checks.inventory;

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
import dev.lmv.lmvac.api.implement.utils.simulation.MovementUtil;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

// GuiMove обычный / FunTime
@SettingCheck(
   value = "InventoryB",
   cooldown = Cooldown.NO_COOLDOWN
)
public class InventoryB extends Check implements PacketCheck {
   public static ConcurrentHashMap<UUID,Boolean> flag = new ConcurrentHashMap();

   public InventoryB(Plugin plugin) {
      super(plugin);
   }

   public void onPacketReceiving(PacketEvent event) {
      Player player = event.getPlayer();
      PacketType packetType = event.getPacketType();
      PacketContainer packet = event.getPacket();
      if (packetType.equals(Client.WINDOW_CLICK)) {
         int id = event.getPlayer().getEntityId();
         LmvPlayer targetPlayer = (LmvPlayer)LmvPlayer.players.get(id);
         if (targetPlayer == null) {
            return;
         }

         if (player.isInsideVehicle()) {
            return;
         }

         if ((Boolean)flag.getOrDefault(player.getUniqueId(), false)) {
            event.setCancelled(true);
            flag.remove(player.getUniqueId());
         }

         Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (MovementUtil.checkMove(player)) {
//               event.setCancelled(true);
               String reason = locales.getOrDefault("1","Suspicious click in inventory during potential movement.");
               this.advancedFlag(player,reason);
            }

         });
      }

   }

   public void advancedFlag(Player player, String reason) {
      this.flag(player, reason);
      flag.put(player.getUniqueId(), true);
   }

    public void advancedFlag(Player player) {
        this.flag(player);
        flag.put(player.getUniqueId(), true);
    }

   public ListeningWhitelist getReceivingWhitelist() {
      return ListeningWhitelist.newBuilder().types(new PacketType[]{Client.WINDOW_CLICK}).build();
   }
}
