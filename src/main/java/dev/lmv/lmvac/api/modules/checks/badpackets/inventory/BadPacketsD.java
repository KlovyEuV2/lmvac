package dev.lmv.lmvac.api.modules.checks.badpackets.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Client;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.LmvAC;
import dev.lmv.lmvac.api.implement.api.Geyser;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

// NoServerSwap detect / Детектит NoSrvSwapSlot
@SettingCheck(
   value = "BadPacketsD",
   cooldown = Cooldown.COOLDOWN
)
public class BadPacketsD extends Check implements PacketCheck {
   private final ConcurrentHashMap<UUID,PlayerSwapState> playerStates = new ConcurrentHashMap();
   private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1);
   private static final long MAX_SWAP_DELAY = 80L;
   private static final long CLEANUP_INTERVAL = 30000L;
   private static final long VIOLATION_RESET_TIME = 5000L;

   public BadPacketsD(Plugin plugin) {
      super(plugin);
      this.cleanupExecutor.scheduleAtFixedRate(this::cleanupInactiveData, 30000L, 30000L, TimeUnit.MILLISECONDS);
   }

   public void onPacketSending(PacketEvent event) {
      if (!Geyser.isBedrockPlayer(event.getPlayer().getUniqueId())) {
         Player player = event.getPlayer();
         PacketContainer packet = event.getPacket();
         if (event.getPacketType() == Server.HELD_ITEM_SLOT) {
            int newSlot = (Integer)packet.getIntegers().read(0);
            long now = System.currentTimeMillis();
            PlayerSwapState state = (PlayerSwapState)this.playerStates.computeIfAbsent(player.getUniqueId(), (k) -> {
               return new PlayerSwapState();
            });
            int previousSlot = state.getCurrentHeldSlot();
            state.setCurrentHeldSlot(newSlot);
            if (newSlot == previousSlot) {
               return;
            }

            state.setLastServerSwap(new SwapData(now, newSlot, previousSlot));
            if (this.isDebugEnabled()) {
               player.sendMessage(String.valueOf(ChatColor.BLUE) + "Server swap: " + previousSlot + " -> " + newSlot);
            }
         }

      }
   }

   public void onPacketReceiving(PacketEvent event) {
      Player player = event.getPlayer();
      PacketContainer packet = event.getPacket();
      if (event.getPacketType() == Client.HELD_ITEM_SLOT) {
         int newSlot = (Integer)packet.getIntegers().read(0);
         long now = System.currentTimeMillis();
         if (newSlot < 0 || newSlot > 8) {
            this.flag(player);
            return;
         }

         PlayerSwapState state = (PlayerSwapState)this.playerStates.computeIfAbsent(player.getUniqueId(), (k) -> {
            return new PlayerSwapState();
         });
         SwapData serverSwap = state.getLastServerSwap();
         if (this.isBackswapDetected(serverSwap, newSlot, now, player)) {
            this.sendAlert(player);
            state.setLastViolationTime(now);
         }

         state.setCurrentHeldSlot(newSlot);
      }

   }

   private boolean isBackswapDetected(SwapData serverSwap, int clientSlot, long now, Player player) {
      if (serverSwap == null) {
         return false;
      } else {
         long delay = now - serverSwap.getTimestamp();
         int safeDelay = Math.min(100, Math.max(0, player.getPing()));
         return delay > 0L && delay < ((long)player.getPing() > 80L ? (long)safeDelay : 80L) && clientSlot == serverSwap.getPreviousSlot();
      }
   }

   private void sendAlert(Player player) {
      this.flag(player);
      if (this.isDebugEnabled()) {
         player.sendMessage(String.valueOf(ChatColor.RED) + "[DEBUG] ItemSwapFix alert sent.");
      }

   }

   private void cleanupInactiveData() {
      long now = System.currentTimeMillis();
      this.playerStates.entrySet().removeIf((entry) -> {
         PlayerSwapState state = (PlayerSwapState)entry.getValue();
         return state.getLastViolationTime() > 0L && now - state.getLastViolationTime() > 300000L || state.getLastServerSwap() != null && now - state.getLastServerSwap().getTimestamp() > 300000L;
      });
   }

   private boolean isDebugEnabled() {
      return false;
   }

   public void resetPlayerViolations(UUID playerId) {
      this.playerStates.remove(playerId);
   }

   public void onPlayerQuit(UUID playerId) {
      this.playerStates.remove(playerId);
   }

   public ListeningWhitelist getSendingWhitelist() {
      return ListeningWhitelist.newBuilder().types(new PacketType[]{Server.HELD_ITEM_SLOT}).build();
   }

   public ListeningWhitelist getReceivingWhitelist() {
      return ListeningWhitelist.newBuilder().types(new PacketType[]{Client.HELD_ITEM_SLOT}).build();
   }

   public Plugin getPlugin() {
      return LmvAC.getInstance();
   }

   public void shutdown() {
      this.cleanupExecutor.shutdown();

      try {
         if (!this.cleanupExecutor.awaitTermination(5L, TimeUnit.SECONDS)) {
            this.cleanupExecutor.shutdownNow();
         }
      } catch (InterruptedException var2) {
         this.cleanupExecutor.shutdownNow();
         Thread.currentThread().interrupt();
      }

   }

   private static class PlayerSwapState {
      private SwapData lastServerSwap;
      private long lastViolationTime;
      private int currentHeldSlot;

      public SwapData getLastServerSwap() {
         return this.lastServerSwap;
      }

      public void setLastServerSwap(SwapData swap) {
         this.lastServerSwap = swap;
      }

      public long getLastViolationTime() {
         return this.lastViolationTime;
      }

      public void setLastViolationTime(long time) {
         this.lastViolationTime = time;
      }

      public int getCurrentHeldSlot() {
         return this.currentHeldSlot;
      }

      public void setCurrentHeldSlot(int slot) {
         this.currentHeldSlot = slot;
      }
   }

   private static class SwapData {
      private final long timestamp;
      private final int newSlot;
      private final int previousSlot;

      public SwapData(long timestamp, int newSlot, int previousSlot) {
         this.timestamp = timestamp;
         this.newSlot = newSlot;
         this.previousSlot = previousSlot;
      }

      public long getTimestamp() {
         return this.timestamp;
      }

      public int getNewSlot() {
         return this.newSlot;
      }

      public int getPreviousSlot() {
         return this.previousSlot;
      }
   }
}
