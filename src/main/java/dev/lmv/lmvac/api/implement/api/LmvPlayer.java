package dev.lmv.lmvac.api.implement.api;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.PacketType.Play.Client;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.lmv.lmvac.LmvAC;
import dev.lmv.lmvac.api.ConfigManager;
import dev.lmv.lmvac.api.commands.Main_Command;
import dev.lmv.lmvac.api.implement.checks.other.CheckManager;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.utils.text.ColorUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class LmvPlayer implements PacketListener, Listener {
   public static final ConcurrentHashMap<Integer,Player> plID = new ConcurrentHashMap<>();
   public static final ConcurrentHashMap<Integer, LmvPlayer> players = new ConcurrentHashMap<>();
   private static final long CONSOLE_COOLDOWN;
   private static final long ADMIN_COOLDOWN;
   private static final long PLAYER_JOIN_COOLDOWN;
   private static final String BRAND_PERMISSION = "lmvac.notify.brand";
   private static long lastConsoleMsgTime;
   private static final Map<UUID,Long> lastAdminMsgTime;
   private static final Map<UUID,Long> lastPlayerJoinTime;
   public Player player;
   public long lastSneak, lastSprint, lastRidingJump, lastInventoryOpen, lastWindowClick, lastWindowClose, lastMoveTime, lastPositionLook, lastLook, lastBlockDig, lastBlockPlace, lastArmAnimation, lastAbilities, lastEntityAction, lastHeldItem, lastPickItem, lastUseItem, lastTabComplete, lastVehicleMove, lastTeleportAccept, lastUseEntity, lastEnchantItem, lastBoatMove, lastSetCreativeSlot, lastGround = 0L;
   public final List<Location> locations = new ArrayList<>();
   public final List<Location> groundLocations = new ArrayList<>();
   public Location currentLocation;
   public Location groundLocation;
   public List<LookInformation> looks = new ArrayList<>();
   public final Set<String> bypass = ConcurrentHashMap.newKeySet();
   private BukkitTask bypassUpdateTask;
   public boolean collision = false;

   public boolean isInventoryOpened = false;
   public boolean isServerInventoryOpened = false;

   public List<PositionInfo> positions = new ArrayList<>();

   public static long orderA_maxPing = 929;

   public static class PositionInfo {
       public PacketEvent event;
       public long time;
       public PositionInfo(PacketEvent moveEvent) {
           this.event = moveEvent;
           time = System.currentTimeMillis();
       }
   }

   public int inventoryMoves = -1;
   public ConcurrentHashMap<PacketType,Integer> inventoryMovesP = new ConcurrentHashMap<>();

   public boolean alerts = true;
   public long lastUpdateTarget = -1L;

   public double lastYaw;
   public double lastPitch;

   public boolean isLastLagging = false;

   public static Long savePT_Look = -1L;

   public ClientSettings clientSettings;

    public static class ClientSettings {
        private final String locale;
        private final int viewDistance;
        private final EnumWrappers.ChatVisibility chatVisibility;
        private final boolean chatColors;
        private final byte skinParts;
        private final EnumWrappers.Hand mainHand;
        private final boolean textFilteringEnabled;
        private final boolean allowsServerListings;

        public ClientSettings(String locale, int viewDistance,
                              EnumWrappers.ChatVisibility chatVisibility,
                              boolean chatColors, byte skinParts,
                              EnumWrappers.Hand mainHand,
                              boolean textFilteringEnabled, boolean allowsServerListings) {
            this.locale = locale;
            this.viewDistance = viewDistance;
            this.chatVisibility = chatVisibility;
            this.chatColors = chatColors;
            this.skinParts = skinParts;
            this.mainHand = mainHand;
            this.textFilteringEnabled = textFilteringEnabled;
            this.allowsServerListings = allowsServerListings;
        }

        @Override
        public String toString() {
            return "ClientSettings{" +
                    "locale='" + locale + '\'' +
                    ", viewDistance=" + viewDistance +
                    ", chatVisibility=" + chatVisibility +
                    ", chatColors=" + chatColors +
                    ", skinParts=" + skinParts +
                    ", mainHand=" + mainHand +
                    ", textFilteringEnabled=" + textFilteringEnabled +
                    ", allowsServerListings=" + allowsServerListings +
                    '}';
        }
    }

    static Plugin plugin;
    private LmvPlayer(Player player) {
      this.player = player;
      this.players.put(player.getEntityId(), this);
      this.bypassUpdateTask = Bukkit.getScheduler().runTaskTimer(LmvAC.getInstance(), this::updateBypassList, 0L, 600L);
   }

   public LmvPlayer(Plugin plugin) {
      this.plugin = plugin;
      Bukkit.getPluginManager().registerEvents(this, plugin);
      ProtocolLibrary.getProtocolManager().addPacketListener(this);
      reload();
   }

   public static void reload() {
       ConfigurationSection orderA = plugin.getConfig().getConfigurationSection("checks.packetorder.a");
       savePT_Look = plugin.getConfig().getLong("look.savePT",-1L);
       orderA_maxPing = orderA.getLong("max-ping",929);
   }

   public static LmvPlayer get(Player player) {
      return (LmvPlayer)players.get(player.getEntityId());
   }

   public boolean hasBypass(String checkName) {
      return this.bypass.contains(checkName.toLowerCase());
   }

    public void updateBypassList() {
        if (this.player == null || !this.player.isOnline()) {
            if (this.bypassUpdateTask != null) {
                this.bypassUpdateTask.cancel();
            }
            return;
        }

        this.bypass.clear();

        for (Check check : CheckManager.getChecks()) {
            String checkName = check.getName().toLowerCase();

            if (this.player.hasPermission("lmvac.bypass." + checkName)) {
                this.bypass.add(checkName);
            }
        }
    }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      int id = player.getEntityId();
      plID.put(id, player);
      LmvPlayer client = (LmvPlayer)players.get(id);
      if (client == null) {
         client = new LmvPlayer(player);
      }

      client.updateBypassList();
      this.handlePlayerBrandNotification(player);
      this.handleAdvertisingNotification(player);
   }

   private void handleAdvertisingNotification(Player player) {
        if (!ConfigManager.advertising) return;
        if (player.hasPermission("lmvac.notify.advertising")) {
            player.sendMessage(ColorUtil.setColorCodes(""));
            player.sendMessage(ColorUtil.setColorCodes("&b█░░ █▀▄▀█ █░█ ▄▀█ █▀▀"));
            player.sendMessage(ColorUtil.setColorCodes("&b█▄▄ █░▀░█ ▀▄▀ █▀█ █▄▄"));
            player.sendMessage(ColorUtil.setColorCodes("&b"+LmvAC.version+" &f: "+LmvAC.supportLink));
            player.sendMessage(ColorUtil.setColorCodes(""));
            player.sendMessage(ColorUtil.setColorCodes("&bСпасибо за использование, буду рад если оставите отзыв !"));
            player.sendMessage(ColorUtil.setColorCodes("&4Данное сообщение вы можете отключить в конфиге. ;)"));
            player.sendMessage(ColorUtil.setColorCodes(""));
        }
   }

    private boolean isClimbable(Block block) {
        Material type = block.getType();
        return type == Material.LADDER || type == Material.VINE || type == Material.SCAFFOLDING || type == Material.WEEPING_VINES || type == Material.TWISTING_VINES || type == Material.WEEPING_VINES_PLANT || type == Material.TWISTING_VINES_PLANT;
    }

    private boolean hasClimbableNearby(Location location) {
        for(int x = -1; x <= 1; ++x) {
            for(int z = -1; z <= 1; ++z) {
                for(int y = -1; y <= 2; ++y) {
                    Block block = location.clone().add((double)x, (double)y, (double)z).getBlock();
                    if (this.isClimbable(block)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
    
    public boolean isMovePacketing() {
        long now = System.currentTimeMillis();
        List<LmvPlayer.PositionInfo> positions = this.positions;
        positions.removeIf(k -> now - k.time >= 1000);
        return !positions.isEmpty();
    }

   private void handlePlayerBrandNotification(Player player) {
      String brandName = (String)Optional.ofNullable(player.getClientBrandName()).map(String::toLowerCase).orElse("&8# unknown");
      if (!brandName.equals("vanilla") && !brandName.equals("&8# unknown")) {
         long now = System.currentTimeMillis();
         UUID playerUUID = player.getUniqueId();
         if (now - (Long)lastPlayerJoinTime.getOrDefault(playerUUID, 0L) >= PLAYER_JOIN_COOLDOWN) {
            lastPlayerJoinTime.put(playerUUID, now);
            String prefix = ConfigManager.prefix.isEmpty() ? "&#54DAF4а&#54BAD5ч&#5499B6: &r" : ConfigManager.prefix;
            String message = ColorUtil.setColorCodes(String.format("%s&f%s &bjoined with &f%s", prefix, player.getName(), brandName));
            if (now - lastConsoleMsgTime >= CONSOLE_COOLDOWN) {
               Bukkit.getConsoleSender().sendMessage(message);
               lastConsoleMsgTime = now;
            }

            if (now - (Long)lastAdminMsgTime.getOrDefault(playerUUID, 0L) >= ADMIN_COOLDOWN) {
               Bukkit.broadcast(message, "lmvac.notify.brand");
               lastAdminMsgTime.put(playerUUID, now);
            }
         }

      }
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      int id = event.getPlayer().getEntityId();
      LmvPlayer client = (LmvPlayer)players.get(id);
      if (client != null && client.bypassUpdateTask != null) {
         client.bypassUpdateTask.cancel();
      }

      plID.remove(id);
      players.remove(id);
      Player player = event.getPlayer();
      if (Main_Command.spectatorsUUIDS.containsKey(player.getUniqueId())) {
         UUID senderUUID = player.getUniqueId();
         Location returnLoc = (Location)Main_Command.spectatorsLOCS.get(senderUUID);
         GameMode returnGM = (GameMode)Main_Command.spectatorsGMS.get(senderUUID);
         if (returnLoc != null && returnGM != null) {
            player.teleport(returnLoc);
            player.setGameMode(returnGM);
            Bukkit.getOnlinePlayers().forEach((onlinePlayer) -> {
               onlinePlayer.showPlayer(LmvAC.getInstance(), player);
            });
         }

         Main_Command.spectatorsUUIDS.remove(senderUUID);
         Main_Command.spectatorsLOCS.remove(senderUUID);
         Main_Command.spectatorsGMS.remove(senderUUID);
      }

   }

   public void onPacketReceiving(PacketEvent event) {
      Player player = event.getPlayer();
      LmvPlayer client = get(player);
      if (client != null) {
         PacketType type = event.getPacketType();
         long now = System.currentTimeMillis();
         if (type.equals(Client.ARM_ANIMATION)) {
            client.lastArmAnimation = now;
         } else if (type.equals(Client.ABILITIES)) {
            client.lastAbilities = now;
         } else if (type.equals(Client.BLOCK_DIG)) {
            client.lastBlockDig = now;
         } else if (type.equals(Client.BLOCK_PLACE)) {
            client.lastBlockPlace = now;
         } else if (type.equals(Client.CLOSE_WINDOW)) {
            client.lastWindowClose = now;
         } else if (type.equals(Client.ENTITY_ACTION)) {
            client.lastEntityAction = now;
            EnumWrappers.PlayerAction action = (EnumWrappers.PlayerAction)event.getPacket().getPlayerActions().read(0);
            switch (action) {
               case START_SNEAKING:
               case STOP_SNEAKING:
                  client.lastSneak = now;
                  break;
               case START_SPRINTING:
               case STOP_SPRINTING:
                  client.lastSprint = now;
                  break;
               case START_RIDING_JUMP:
                  client.lastRidingJump = now;
                  break;
               case OPEN_INVENTORY:
                  client.lastInventoryOpen = now;
            }
         } else if (type.equals(Client.HELD_ITEM_SLOT)) {
            client.lastHeldItem = now;
         } else if (type.equals(Client.LOOK)) {
            client.lastLook = now;
         } else if (type.equals(Client.PICK_ITEM)) {
            client.lastPickItem = now;
         } else if (type.equals(Client.POSITION_LOOK)) {
            client.lastPositionLook = now;
            if (savePT_Look > 0) {
                if (now-lastUpdateTarget>=savePT_Look) {
                    try {
                        client.looks.add(new LookInformation(event, player.getLocation()));
                    } catch (Exception e1) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            client.looks.add(new LookInformation(event, player.getLocation()));
                        });
                    }
                    lastUpdateTarget = now;
                }
            } else {
                try {
                    client.looks.add(new LookInformation(event, player.getLocation()));
                } catch (Exception e1) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        client.looks.add(new LookInformation(event, player.getLocation()));
                    });
                }
                lastUpdateTarget = now;
            }
         } else if (type.equals(Client.POSITION)) {
            Location location = player.getLocation();
            if (!player.isInsideVehicle() && player.isTicking() && player.getPing()<orderA_maxPing && !player.isDead() && player.isOnline()) {
                client.positions.add(new PositionInfo(event));
            } else {
                isLastLagging = true;
            }
            if (client.currentLocation == null) {
               client.currentLocation = location.clone();
            } else {
               double dx = player.getLocation().getX() - client.currentLocation.getX();
               double dz = player.getLocation().getZ() - client.currentLocation.getZ();
               double distanceXZ = Math.sqrt(dx * dx + dz * dz);
               if (distanceXZ > 0.0) {
                  client.currentLocation = location.clone();
                  client.locations.add(client.currentLocation);
                  client.lastMoveTime = now;
                  if (player.isOnGround() || player.isGliding() || player.isFlying() || hasClimbableNearby(player.getLocation())) {
                     client.groundLocation = location.clone();
                     client.groundLocations.add(client.groundLocation);
                     client.lastGround = now;
                  }
               }
            }
         } else if (type.equals(Client.SET_CREATIVE_SLOT)) {
            client.lastSetCreativeSlot = now;
         } else if (type.equals(Client.TAB_COMPLETE)) {
            client.lastTabComplete = now;
         } else if (type.equals(Client.TELEPORT_ACCEPT)) {
            client.lastTeleportAccept = now;
         } else if (type.equals(Client.USE_ENTITY)) {
            client.lastUseEntity = now;
         } else if (type.equals(Client.USE_ITEM)) {
            client.lastUseItem = now;
         } else if (type.equals(Client.VEHICLE_MOVE)) {
            client.lastVehicleMove = now;
         } else if (type.equals(Client.WINDOW_CLICK)) {
            client.lastWindowClick = now;
         } else if (type.equals(Client.ENCHANT_ITEM)) {
            client.lastEnchantItem = now;
         } else if (type.equals(Client.BOAT_MOVE)) {
            client.lastBoatMove = now;
         }

         if (client.looks.size() > 25) {
            client.looks.remove(0);
         }

      }
   }

   public Vector getGroundVelocity(Double max, Location input) {
      for(double i = 0.0; i < max; i += 0.1) {
         Location output = input.subtract(0.0, i, 0.0);
         if (output.getBlock().isSolid()) {
            return new Vector(0.0, -i, 0.0);
         }
      }

      return new Vector(0.0, -0.1, 0.0);
   }

   public Location getLastGroundLocation(Double max, Location def) {
      Location lastLocation = this.groundLocation;
      return lastLocation.distance(def) <= max ? lastLocation : def;
   }

   public Location getLastGroundLocation() {
      return this.groundLocation;
   }

   public Location getLastLocation() {
      return (Location)this.locations.get(this.locations.size() - 2);
   }

   public void onPacketSending(PacketEvent event) {
   }

   public ListeningWhitelist getSendingWhitelist() {
      return ListeningWhitelist.EMPTY_WHITELIST;
   }

   public ListeningWhitelist getReceivingWhitelist() {
      return ListeningWhitelist.newBuilder().types(new PacketType[]{Client.ARM_ANIMATION, Client.ABILITIES, Client.BLOCK_DIG, Client.BLOCK_PLACE, Client.CLOSE_WINDOW, Client.ENTITY_ACTION, Client.HELD_ITEM_SLOT, Client.LOOK, Client.POSITION, Client.POSITION_LOOK, Client.PICK_ITEM, Client.SET_CREATIVE_SLOT, Client.TAB_COMPLETE, Client.TELEPORT_ACCEPT, Client.USE_ENTITY, Client.USE_ITEM, Client.VEHICLE_MOVE, Client.WINDOW_CLICK, Client.ENCHANT_ITEM, Client.BOAT_MOVE}).build();
   }

   public Plugin getPlugin() {
      return LmvAC.getInstance();
   }

   static {
      CONSOLE_COOLDOWN = TimeUnit.SECONDS.toMillis(15L);
      ADMIN_COOLDOWN = TimeUnit.SECONDS.toMillis(5L);
      PLAYER_JOIN_COOLDOWN = TimeUnit.MINUTES.toMillis(3L);
      lastConsoleMsgTime = 0L;
      lastAdminMsgTime = new ConcurrentHashMap();
      lastPlayerJoinTime = new ConcurrentHashMap();
   }

   public static class LookInformation {
      public PacketEvent packetEvent;
      public Location location;
      public Entity target;
      public Long time;

      public LookInformation(PacketEvent packetEvent, Location location) {
         this.packetEvent = packetEvent;
         if (packetEvent != null && packetEvent.getPlayer() != null && !packetEvent.getPlayer().isDead()) {
            Player packetPlayer = packetEvent.getPlayer();
            this.target = packetPlayer.getTargetEntity(4);
         }

         this.location = location;
         this.time = System.currentTimeMillis();
      }
   }
}
