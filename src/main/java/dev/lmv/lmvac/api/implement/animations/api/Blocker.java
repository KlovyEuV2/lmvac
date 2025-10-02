package dev.lmv.lmvac.api.implement.animations.api;

import dev.lmv.lmvac.api.ConfigManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class Blocker implements Listener {
   public static final ConcurrentHashMap players = new ConcurrentHashMap();
   private static Plugin plugin;
//   private static final double MIN_MOVEMENT_THRESHOLD = 0.001;

   public Blocker(Plugin plugin) {
      Blocker.plugin = plugin;

      try {
         Bukkit.getPluginManager().registerEvents(this, plugin);
      } catch (Exception var3) {
         plugin.getLogger().log(Level.SEVERE, "Ошибка при регистрации Blocker listener", var3);
      }

   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   private void onMove(PlayerMoveEvent event) {
      if (!event.isCancelled()) {
         Player player = event.getPlayer();
         if (player != null) {
            UUID playerId = player.getUniqueId();
            if (players.containsKey(playerId)) {
               try {
                  Location to = event.getTo();
                  Location from = event.getFrom();
                  if (to == null || from == null) {
                     return;
                  }

                  if (!to.getWorld().equals(from.getWorld())) {
                     event.setCancelled(true);
                     return;
                  }

                  double dx = to.getX() - from.getX();
                  double dz = to.getZ() - from.getZ();
                  double dy = to.getY() - from.getY();
                  double distXZ = Math.sqrt(dx * dx + dz * dz);
                  boolean movedXZ = distXZ > 0.001;
                  if (movedXZ || dy < -0.001) {
                     event.setCancelled(true);
                  }
               } catch (Exception var15) {
                  plugin.getLogger().log(Level.WARNING, "Ошибка в onMove для игрока " + player.getName(), var15);
               }

            }
         }
      }
   }

   @EventHandler
   private void onQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      if (player != null) {
         UUID playerId = player.getUniqueId();
         if (players.containsKey(playerId)) {
            try {
               List commands = (List)players.get(playerId);
               if (commands != null && !commands.isEmpty()) {
                  this.executeCommands(commands, player);
               }
            } catch (Exception var8) {
               plugin.getLogger().log(Level.WARNING, "Ошибка при выполнении команд для игрока " + player.getName(), var8);
            } finally {
               players.remove(playerId);
            }

         }
      }
   }

   private void executeCommands(List commands, Player player) {
      String playerName = player.getName();
      String prefix = ConfigManager.prefix != null ? ConfigManager.prefix : "";
      Iterator var5 = commands.iterator();

      while(var5.hasNext()) {
         String command = (String)var5.next();
         if (command != null && !command.trim().isEmpty()) {
            try {
               String cmdNew = command.replace("%prefix%", prefix).replace("%player%", playerName);
               Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdNew);
            } catch (Exception var8) {
               plugin.getLogger().log(Level.WARNING, "Не удалось выполнить команду '" + command + "' для игрока " + playerName, var8);
            }
         }
      }

   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   private void onInventory(InventoryClickEvent event) {
      if (!event.isCancelled()) {
         if (event.getWhoClicked() instanceof Player) {
            Player player = (Player)event.getWhoClicked();
            if (players.containsKey(player.getUniqueId())) {
               event.setCancelled(true);
            }
         }

      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   private void onDrop(PlayerDropItemEvent event) {
      if (!event.isCancelled()) {
         Player player = event.getPlayer();
         if (player != null && players.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
         }

      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   private void onPickUp(PlayerPickupItemEvent event) {
      if (!event.isCancelled()) {
         Player player = event.getPlayer();
         if (player != null && players.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
         }

      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   private void onChat(AsyncPlayerChatEvent event) {
      if (!event.isCancelled()) {
         Player player = event.getPlayer();
         if (player != null && players.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
         }

      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   private void onCommand(PlayerCommandPreprocessEvent event) {
      if (!event.isCancelled()) {
         Player player = event.getPlayer();
         if (player != null && players.containsKey(player.getUniqueId())) {
            String command = event.getMessage().toLowerCase();
            if (this.isAllowedCommand(command)) {
               return;
            }

            event.setCancelled(true);
         }

      }
   }

   private boolean isAllowedCommand(String command) {
      String[] allowedCommands = new String[]{"/help", "/rules", "/info", "/spawn"};
      String[] var3 = allowedCommands;
      int var4 = allowedCommands.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         String allowed = var3[var5];
         if (command.startsWith(allowed)) {
            return true;
         }
      }

      return false;
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   private void onBlockPlace(BlockPlaceEvent event) {
      if (!event.isCancelled()) {
         Player player = event.getPlayer();
         if (player != null && players.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
         }

      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   private void onBlockBreak(BlockBreakEvent event) {
      if (!event.isCancelled()) {
         Player player = event.getPlayer();
         if (player != null && players.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
         }

      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   private void onDamage(EntityDamageEvent event) {
      if (!event.isCancelled()) {
         if (event.getEntity() instanceof Player) {
            Player player = (Player)event.getEntity();
            if (players.containsKey(player.getUniqueId())) {
               event.setCancelled(true);
            }
         }

      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   private void onInteract(PlayerInteractEvent event) {
      if (!event.isCancelled()) {
         Player player = event.getPlayer();
         if (player != null && players.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
         }

      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   private void onInteractEntity(PlayerInteractEntityEvent event) {
      if (!event.isCancelled()) {
         Player player = event.getPlayer();
         if (player != null && players.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
         }

      }
   }

   public static void blockPlayer(UUID playerId, List commands) {
      if (playerId != null) {
         players.put(playerId, commands != null ? new ArrayList(commands) : new ArrayList());
      }

   }

   public static void unblockPlayer(UUID playerId) {
      if (playerId != null) {
         players.remove(playerId);
      }

   }

   public static boolean isPlayerBlocked(UUID playerId) {
      return playerId != null && players.containsKey(playerId);
   }

   public static void addCommandToPlayer(UUID playerId, String command) {
      if (playerId != null && command != null && !command.trim().isEmpty()) {
         ((List)players.computeIfAbsent(playerId, (k) -> {
            return new ArrayList();
         })).add(command);
      }

   }

   public static void clearAllBlocked() {
      players.clear();
   }

   public static Set getBlockedPlayers() {
      return new HashSet(players.keySet());
   }
}
