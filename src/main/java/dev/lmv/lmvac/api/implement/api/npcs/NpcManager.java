package dev.lmv.lmvac.api.implement.api.npcs;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.*;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.modules.checks.aim.AimNpc;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.sound.midi.Track;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static dev.lmv.lmvac.api.implement.api.npcs.RandomArmorGenerator.sendRandomArmor;

public class NpcManager {

    public static NPCNameManager nameManager;

    private static final List<Check> remoteCheks = new ArrayList<>();

    public static final Map<UUID, TrackedNpc> npcMap = new ConcurrentHashMap<>();
    private static long TIMEOUT_MS = 3000;
    private static JavaPlugin plugin;

    private static MinecraftVersion version;
    private static boolean isModernVersion;
    private static boolean isVeryModernVersion;
    private static boolean isNewestVersion;

    public static boolean strafe = false;
    public static RotationMode rotationMode = RotationMode.DEFAULT;
    public static RotationMode spawnMode = RotationMode.DEFAULT;
    public static AimMode aimMode = AimMode.OLD;
    public static int lookPattern = 9;
    public static int smoothPattern = -1;
    public static int defaultPattern = 5;
    public static RandomMode randomMode = RandomMode.TAB;
    public static List<String> npcNames = new ArrayList<>();
    public static int armorChance = 50;

    public static int updateTicks = 2;

    public NpcManager(JavaPlugin pl) {
        plugin = pl;
        detectVersion();
        startUpdater();
        startListeners();
        nameManager = new NPCNameManager(plugin);
        loadSettings(plugin);
    }

    public static void loadSettings(Plugin plugin) {
        strafe = plugin.getConfig().getBoolean("npc.strafe", false);

        String rotMode = plugin.getConfig().getString("npc.rotation-mode", "default").toLowerCase();
        rotationMode = rotMode.equals("smart") ? RotationMode.SMART : RotationMode.DEFAULT;

        String aimModeStr = plugin.getConfig().getString("npc.aim-mode", "old").toLowerCase();
        switch (aimModeStr) {
            case "new":
                aimMode = AimMode.NEW;
                break;
            case "middle":
                aimMode = AimMode.MIDDLE;
                break;
            default:
                aimMode = AimMode.OLD;
                break;
        }

        lookPattern = plugin.getConfig().getInt("npc.pattern.look", 9);
        smoothPattern = plugin.getConfig().getInt("npc.pattern.smooth", -1);
        defaultPattern = plugin.getConfig().getInt("npc.pattern.default", 5);

        String randMode = plugin.getConfig().getString("npc.random.mode", "TAB").toUpperCase();
        randomMode = randMode.equals("CONFIG") ? RandomMode.CONFIG : RandomMode.TAB;

        String namesStr = plugin.getConfig().getString("npc.names", "");
        if (!namesStr.isEmpty()) {
            npcNames = Arrays.asList(namesStr.split(";"));
        } else {
            npcNames = Arrays.asList(
                    "akvi4", "IIuoner", "Error404", "M7trix", "bmw3000", "repeat3000",
                    "gercog228", "EllO1", "RErEreo", "OreO3020", "OrEo998", "MenTos3021",
                    "Cola3029", "Pepsi1023", "Elonmusk300", "Pr302er", "moderator1029",
                    "moder392", "shipuchka228", "Pon30928", "KIllka392", "Penit3000",
                    "hlebushek9", "vmer23900", "wwwxyz", "LoopRepeat", "Mann3029", "Eplka302r"
            );
        }

        long timeout = plugin.getConfig().getLong("npc.timeout",3000);
        try {
            TIMEOUT_MS = timeout;
        } catch (Exception e) {
            TIMEOUT_MS = 3000;
        }

        int rotUpd = plugin.getConfig().getInt("npc.rotation-update",2);
        try {
            updateTicks = rotUpd;
        } catch (Exception e) {
            updateTicks = 2;
        }

        armorChance = plugin.getConfig().getInt("npc.armor-chance",70);
        spawnMode = RotationMode.valueOf(plugin.getConfig().getString("npc.spawn-mode","SMART").toUpperCase());
    }

    public static boolean addRemote(Check check) {
        remoteCheks.add(check);
        return true;
    }

    private static void detectVersion() {
        String versionString = Bukkit.getVersion();
        plugin.getLogger().info("Обнаружена версия сервера: " + versionString);

        if (versionString.contains("1.16")) {
            version = MinecraftVersion.V1_16;
        } else if (versionString.contains("1.17")) {
            version = MinecraftVersion.V1_17;
        } else if (versionString.contains("1.18")) {
            version = MinecraftVersion.V1_18;
        } else if (versionString.contains("1.19")) {
            if (versionString.contains("1.19.3") || versionString.contains("1.19.4")) {
                version = MinecraftVersion.V1_19_3;
                isModernVersion = true;
            } else {
                version = MinecraftVersion.V1_19;
            }
        } else if (versionString.contains("1.20")) {
            version = MinecraftVersion.V1_20;
            isModernVersion = true;
            if (versionString.contains("1.20.2") || versionString.contains("1.20.3") || versionString.contains("1.20.4")) {
                isVeryModernVersion = true;
            }
        } else if (versionString.contains("1.21")) {
            version = MinecraftVersion.V1_21;
            isModernVersion = true;
            isVeryModernVersion = true;
            isNewestVersion = true;
        } else {
            version = MinecraftVersion.UNKNOWN;
            isModernVersion = true;
            isVeryModernVersion = true;
        }

        plugin.getLogger().info("Версия определена как: " + version +
                " (modern: " + isModernVersion +
                ", veryModern: " + isVeryModernVersion +
                ", newest: " + isNewestVersion + ")");
    }

    public static void spawnNpcFor(Player player, String name, WrappedGameProfile profile) {
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (npcMap.containsKey(playerId)) {
            npcMap.get(playerId).lastUsed = now;
            return;
        }

        int entityId = generateUniqueEntityId();
        Location loc = getBehindPlayer(player);
        float health = 1 + new Random().nextFloat() * 19;

        try {
            addPlayerToTabList(player, profile, name);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    spawnPlayerEntity(player, entityId, profile, loc);
                    rotateHead(player, entityId, loc);
                    if (Math.random()*100 < armorChance) sendRandomArmor(player, entityId);
                } catch (Exception e) {
                }
            }, 3L);

            TrackedNpc trackedNpc = new TrackedNpc(entityId, profile, now);
            trackedNpc.lastLocation = loc.clone();
            npcMap.put(playerId, trackedNpc);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendFakeArmor(Player player, int entityId) {
        try {
            PacketContainer equipmentPacket = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
            equipmentPacket.getIntegers().write(0, entityId);

            List<Pair<EnumWrappers.ItemSlot, ItemStack>> equipment = Arrays.asList(
                    new Pair<>(EnumWrappers.ItemSlot.HEAD, new ItemStack(Material.DIAMOND_HELMET)),
                    new Pair<>(EnumWrappers.ItemSlot.CHEST, new ItemStack(Material.DIAMOND_CHESTPLATE)),
                    new Pair<>(EnumWrappers.ItemSlot.LEGS, new ItemStack(Material.DIAMOND_LEGGINGS)),
                    new Pair<>(EnumWrappers.ItemSlot.FEET, new ItemStack(Material.DIAMOND_BOOTS))
            );

            equipmentPacket.getSlotStackPairLists().write(0, equipment);
            sendPacketSafely(player, equipmentPacket);
        } catch (Exception e) {
        }
    }

    private static void addPlayerToTabList(Player player, WrappedGameProfile profile, String name) {
        Player onlinePlayer = Bukkit.getPlayer(name);
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            return;
        }

        try {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
            if (isModernVersion) {
                try {
                    packet.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER));
                } catch (Exception e) {
                    packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
                }
            } else {
                packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            }
            List<PlayerInfoData> dataList = Arrays.asList(
                    new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL,
                            WrappedChatComponent.fromText(name))
            );
            packet.getPlayerInfoDataLists().write(0, dataList);
            sendPacketSafely(player, packet);
        } catch (Exception e) {
        }
    }

    private static void removePlayerFromTabList(Player player, WrappedGameProfile profile, String name) {
        Player onlinePlayer = Bukkit.getPlayer(name);
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            return;
        }

        try {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
            if (isModernVersion) {
                try {
                    packet.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER));
                } catch (Exception e) {
                    packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
                }
            } else {
                packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
            }
            List<PlayerInfoData> dataList = Arrays.asList(
                    new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL,
                            WrappedChatComponent.fromText(name))
            );
            packet.getPlayerInfoDataLists().write(0, dataList);
            sendPacketSafely(player, packet);
        } catch (Exception e) {
        }
    }

    private static void spawnPlayerEntity(Player player, int entityId, WrappedGameProfile profile, Location loc) {
        final double[] minDistance = {3.5};
        final double[] distance = {35.0};
//        if (rotationMode == RotationMode.SMART) {
//            loc = getSmartBehindPlayer(player,distance[0]);
//        } else {
//            loc = getBehindPlayer(player,distance[0]);
//        }
        if (spawnMode.equals(RotationMode.SMART)
                || spawnMode.equals(RotationMode.NEW)) {
            loc = getBehindPlayer(player,distance[0]);
        }
        PacketContainer spawn = new PacketContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
        spawn.getSpecificModifier(int.class).write(0, entityId);
        spawn.getUUIDs().write(0, profile.getUUID());
        spawn.getSpecificModifier(double.class).write(0, loc.getX());
        spawn.getSpecificModifier(double.class).write(1, loc.getY());
        spawn.getSpecificModifier(double.class).write(2, loc.getZ());
        spawn.getSpecificModifier(byte.class).write(0, (byte) (loc.getYaw() * 256.0F / 360.0F));
        spawn.getSpecificModifier(byte.class).write(1, (byte) (loc.getPitch() * 256.0F / 360.0F));
        if (spawnMode.equals(RotationMode.SMART) || spawnMode.equals(RotationMode.NEW)) {
            if (npcMap.containsKey(player.getUniqueId())) {
                TrackedNpc npc = npcMap.get(player.getUniqueId());
                distance[0] -= 0.56;
                sendPacketSafely(player, spawn);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        distance[0] -= 0.56;
                        teleportNpc(player,npc,distance[0]);
                        if (distance[0] <= minDistance[0]) {
                            this.cancel();
                            npc.locatedToPlayer = true;
                            return;
                        }
                    }
                }.runTaskTimer(plugin,2L,1L);
            }
        } else {
            sendPacketSafely(player, spawn);
            if (npcMap.containsKey(player.getUniqueId())) {
                TrackedNpc npc = npcMap.get(player.getUniqueId());
                npc.locatedToPlayer = true;
            }
        }
    }

    private static void rotateHead(Player player, int entityId, Location loc) {
        PacketContainer head = new PacketContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        head.getSpecificModifier(int.class).write(0, entityId);
        head.getSpecificModifier(byte.class).write(0, (byte) (loc.getYaw() * 256.0F / 360.0F));
        sendPacketSafely(player, head);
    }

    private static int generateUniqueEntityId() {
        int entityId;
        do {
            entityId = new Random().nextInt(900000) + 100000;
        } while (isEntityIdInUse(entityId));
        return entityId;
    }

    private static boolean isEntityIdInUse(int entityId) {
        return npcMap.values().stream().anyMatch(npc -> npc.entityId == entityId);
    }

    public void startListeners() {
        listenForPacketAttacks();
    }

    private void listenForPacketAttacks() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(plugin, PacketType.Play.Client.USE_ENTITY) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        PacketContainer packet = event.getPacket();
                        Player player = event.getPlayer();
                        try {
                            EnumWrappers.EntityUseAction action = packet.getEntityUseActions().read(0);
                            if (action == EnumWrappers.EntityUseAction.ATTACK) {
                                int attackedEntityId = packet.getSpecificModifier(int.class).read(0);
                                TrackedNpc npc = npcMap.get(player.getUniqueId());
                                if (npc != null && npc.entityId == attackedEntityId) {
                                    refreshNpc(player);
                                    event.setCancelled(true);
                                    for (Check check : remoteCheks) {
                                        check.remoteFlag(player);
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
        );
    }

    private static void startUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Iterator<Map.Entry<UUID, TrackedNpc>> it = npcMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, TrackedNpc> entry = it.next();
                    UUID playerId = entry.getKey();
                    TrackedNpc npc = entry.getValue();
                    Player player = Bukkit.getPlayer(playerId);
                    if (player == null || !player.isOnline()) {
                        it.remove();
                        continue;
                    }
                    if (now - npc.lastUsed > TIMEOUT_MS) {
                        destroyNpc(player, npc.entityId);
                        it.remove();
                        continue;
                    }
                    updateNpcPosition(player, npc);
                }
            }
        }.runTaskTimer(plugin, updateTicks, updateTicks);
    }

    public static void destroyNpc(Player player, int entityId) {
        try {
            TrackedNpc npc = npcMap.get(player.getUniqueId());
            removePlayerFromTabList(player, npc.profile, npc.profile.getName());
            PacketContainer destroy = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);

            if (isModernVersion || isVeryModernVersion || isNewestVersion) {
                try {
                    destroy.getIntegerArrays().write(0, new int[]{entityId});
                } catch (Exception e) {
                    try {
                        destroy.getIntLists().write(0, Arrays.asList(entityId));
                    } catch (Exception e2) {
                        try {
                            destroy.getSpecificModifier(int[].class).write(0, new int[]{entityId});
                        } catch (Exception e3) {
                            plugin.getLogger().info("error [Destroy-Npc] : "+e3.getMessage());
                        }
                    }
                }
            } else {
                try {
                    destroy.getSpecificModifier(int[].class).write(0, new int[]{entityId});
                } catch (Exception e4) {
                    plugin.getLogger().info("error [Destroy-Npc] : "+e4.getMessage());
                }
            }

            sendPacketSafely(player, destroy);
        } catch (Exception ignored) {}
    }

    private static void teleportNpc(Player player, TrackedNpc npc, double distance) {
        Location newLoc;
        if (rotationMode == RotationMode.SMART) {
            newLoc = getSmartBehindPlayer(player,35.0);
        } else {
            newLoc = getBehindPlayer(player,35.0);
        }
        Location oldLoc = npc.lastLocation;
        if (shouldUpdateLocation(oldLoc, newLoc)) {
            if (rotationMode == RotationMode.SMART && Math.random() < 0.1) {
                npc.isJumping = true;
                npc.jumpStartTime = System.currentTimeMillis();
            }

            if (npc.isJumping) {
                long jumpTime = System.currentTimeMillis() - npc.jumpStartTime;
                if (jumpTime < 500) {
                    double jumpHeight = Math.sin((jumpTime / 500.0) * Math.PI) * 1.2;
                    newLoc.add(0, jumpHeight, 0);
                } else {
                    npc.isJumping = false;
                }
            }

            PacketContainer tp = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
            tp.getSpecificModifier(int.class).write(0, npc.entityId);
            tp.getSpecificModifier(double.class).write(0, newLoc.getX());
            tp.getSpecificModifier(double.class).write(1, newLoc.getY());
            tp.getSpecificModifier(double.class).write(2, newLoc.getZ());
            tp.getSpecificModifier(byte.class).write(0, (byte) (newLoc.getYaw() * 256.0F / 360.0F));
            tp.getSpecificModifier(byte.class).write(1, (byte) (newLoc.getPitch() * 256.0F / 360.0F));
            if (isNewestVersion) {
                try {
                    tp.getBooleans().write(0, true);
                } catch (Exception ignored) {}
            }
            sendPacketSafely(player, tp);
            rotateHead(player, npc.entityId, newLoc);
            npc.lastLocation = newLoc.clone();
        }
    }

    private static void updateNpcPosition(Player player, TrackedNpc npc) {
        if (!npc.locatedToPlayer) {
            refreshNpc(player);
            return;
        }
        Location newLoc = null;
        if (rotationMode == RotationMode.SMART) {
            newLoc = getSmartBehindPlayer(player);
        } else {
            newLoc = getBehindPlayer(player);
        }

        Location oldLoc = npc.lastLocation;
        if (shouldUpdateLocation(oldLoc, newLoc)) {
            if (rotationMode == RotationMode.SMART && Math.random() < 0.1) {
                npc.isJumping = true;
                npc.jumpStartTime = System.currentTimeMillis();
            }

            if (npc.isJumping) {
                long jumpTime = System.currentTimeMillis() - npc.jumpStartTime;
                if (jumpTime < 500) {
                    double jumpHeight = Math.sin((jumpTime / 500.0) * Math.PI) * 1.2;
                    newLoc.add(0, jumpHeight, 0);
                } else {
                    npc.isJumping = false;
                }
            }

            PacketContainer tp = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
            tp.getSpecificModifier(int.class).write(0, npc.entityId);
            tp.getSpecificModifier(double.class).write(0, newLoc.getX());
            tp.getSpecificModifier(double.class).write(1, newLoc.getY());
            tp.getSpecificModifier(double.class).write(2, newLoc.getZ());
            tp.getSpecificModifier(byte.class).write(0, (byte) (newLoc.getYaw() * 256.0F / 360.0F));
            tp.getSpecificModifier(byte.class).write(1, (byte) (newLoc.getPitch() * 256.0F / 360.0F));
            if (isNewestVersion) {
                try {
                    tp.getBooleans().write(0, true);
                } catch (Exception ignored) {}
            }
            sendPacketSafely(player, tp);
            rotateHead(player, npc.entityId, newLoc);
            npc.lastLocation = newLoc.clone();
        }
    }

    private static boolean shouldUpdateLocation(Location oldLoc, Location newLoc) {
        if (oldLoc == null) return true;
        return Math.abs(newLoc.getX() - oldLoc.getX()) > 0.15 ||
                Math.abs(newLoc.getY() - oldLoc.getY()) > 0.15 ||
                Math.abs(newLoc.getZ() - oldLoc.getZ()) > 0.15 ||
                Math.abs(angleDifference(newLoc.getYaw(), oldLoc.getYaw())) > 10;
    }

    private static float angleDifference(float angle1, float angle2) {
        float diff = Math.abs(angle1 - angle2);
        return Math.min(diff, 360 - diff);
    }

    public static void sendPacketSafely(Player player, PacketContainer packet) {
        if (player == null || !player.isOnline()) {
            return;
        }
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } catch (Exception e) {
        }
    }

    private static Location getBehindPlayer(Player player) {
        Location loc = player.getLocation().clone();
        float yaw = loc.getYaw();
        double radianYaw = Math.toRadians(yaw + 180);
        double distance = 2.5;
        double x = -Math.sin(radianYaw) * distance;
        double z = Math.cos(radianYaw) * distance;
        if (strafe) {
            loc.add(
                    x + (Math.random() - 0.5) * 0.5,
                    (Math.random() - 0.5) * 0.75,
                    z + (Math.random() - 0.5) * 0.5
            );
        } else {
            loc.add(
                    x,
                    (Math.random() - 0.5) * 0.75,
                    z
            );
        }
        loc.setYaw(yaw);
        loc.setPitch(0);
        return loc;
    }

    private static Location getBehindPlayer(Player player, double distance) {
        Location loc = player.getLocation().clone();
        float yaw = loc.getYaw();
        double radianYaw = Math.toRadians(yaw + 180);
        double x = -Math.sin(radianYaw) * distance;
        double z = Math.cos(radianYaw) * distance;
        if (strafe) {
            loc.add(
                    x + (Math.random() - 0.5) * 0.5,
                    (Math.random() - 0.5) * 0.75,
                    z + (Math.random() - 0.5) * 0.5
            );
        } else {
            loc.add(
                    x,
                    (Math.random() - 0.5) * 0.75,
                    z
            );
        }
        loc.setYaw(yaw);
        loc.setPitch(0);
        return loc;
    }

    private static Location getSmartBehindPlayer(Player player) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();

        for (int attempts = 0; attempts < 8; attempts++) {
            Location testLoc = calculateBehindLocation(player, 2.5, attempts);

            if (isLocationSuitable(world, testLoc, playerLoc,50)) {
                return testLoc;
            }
        }

        return getBehindPlayer(player);
    }

    private static Location getSmartBehindPlayer(Player player, double distance) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();

        for (int attempts = 0; attempts < 8; attempts++) {
            Location testLoc = calculateBehindLocation(player, distance, attempts);

            if (isLocationSuitable(world, testLoc, playerLoc,50)) {
                return testLoc;
            }
        }

        return getBehindPlayer(player);
    }

    private static Location getSmartBehindPlayer(Player player, double distance, double offsetX, double offsetZ) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();

        for (int attempts = 0; attempts < 8; attempts++) {
            Location testLoc = calculateBehindLocationWithOffset(player, distance, offsetX, offsetZ, attempts);

            if (isLocationSuitable(world, testLoc, playerLoc,50)) {
                return testLoc;
            }
        }

        return getBehindPlayerWithOffset(player, offsetX, offsetZ);
    }

    private static Location calculateBehindLocationWithOffset(Player player, double distance, double offsetX, double offsetZ, int attempt) {
        Location playerLoc = player.getLocation();
        Vector direction = playerLoc.getDirection();

        Vector behind = direction.clone().multiply(-1).normalize().multiply(distance);

        Location baseLoc = playerLoc.clone().add(behind);

        baseLoc.add(offsetX, 0, offsetZ);

        if (attempt > 0) {
            double angle = (attempt - 1) * (Math.PI / 4);
            double variance = 1.0;

            double varX = Math.cos(angle) * variance;
            double varZ = Math.sin(angle) * variance;

            baseLoc.add(varX, 0, varZ);
        }

        return baseLoc;
    }

    private static Location getBehindPlayerWithOffset(Player player, double offsetX, double offsetZ) {
        Location playerLoc = player.getLocation();
        Vector direction = playerLoc.getDirection().normalize();
        Vector right = new Vector(-direction.getZ(), 0, direction.getX()).normalize();

        Location baseLoc = playerLoc.clone().add(direction.clone().multiply(2.0));

        baseLoc.add(right.clone().multiply(offsetX));
        baseLoc.add(direction.clone().multiply(offsetZ));

        return baseLoc;
    }

    private static Location calculateBehindLocation(Player player, double baseDistance, int attempt) {
        Location loc = player.getLocation().clone();
        float yaw = loc.getYaw();
        World world = player.getWorld();

        double angleOffset = (attempt * 45) % 360;
        double radianYaw = Math.toRadians(yaw + 180 + angleOffset);

        double distance = baseDistance + (Math.random() * 0.5);
        double x = -Math.sin(radianYaw) * distance;
        double z = Math.cos(radianYaw) * distance;

        loc.add(x, 0, z);

        for (int y = 3; y >= -3; y--) {
            Location testLoc = loc.clone().add(0, y, 0);
            if (world.getBlockAt(testLoc).getType().isSolid() &&
                    !world.getBlockAt(testLoc.add(0, 1, 0)).getType().isSolid() &&
                    !world.getBlockAt(testLoc.add(0, 2, 0)).getType().isSolid()) {
                loc.setY(testLoc.getY() + 1);
                break;
            }
        }

        if (strafe) {
            loc.add(
                    (Math.random() - 0.5) * 0.8,
                    (Math.random() - 0.5) * 0.3,
                    (Math.random() - 0.5) * 0.8
            );
        }

        loc.setYaw(yaw);
        loc.setPitch(0);
        return loc;
    }

    private static boolean isLocationSuitableRW(World world, Location loc, Location playerLoc) {
        double distance = loc.distance(playerLoc);
        if (distance > 3.5 || distance < 1.5) {
            return false;
        }

        Block feetBlock = world.getBlockAt(loc);
        Block headBlock = world.getBlockAt(loc.clone().add(0, 1, 0));
        Block aboveBlock = world.getBlockAt(loc.clone().add(0, 2, 0));

        if (headBlock.getType().isSolid() || aboveBlock.getType().isSolid() || feetBlock.getType().isSolid()) {
            return false;
        }

        boolean hasGround = false;
        for (int i = 0; i <= 2; i++) {
            Block checkBlock = world.getBlockAt(loc.clone().add(0, -i, 0));
            if (checkBlock.getType().isSolid()) {
                hasGround = true;
                break;
            }
        }

        return hasGround;
    }

    private static boolean isLocationSuitable(World world, Location testLoc, Location playerLoc, float minYawDifference) {
        Block feetBlock = world.getBlockAt(testLoc);
        Block headBlock = world.getBlockAt(testLoc.clone().add(0, 1, 0));

        if (feetBlock.getType().isSolid() || headBlock.getType().isSolid()) {
            return false;
        }

        if (testLoc.getY() - playerLoc.getY() > 2.5) {
            return false;
        }

        Block groundBlock = world.getBlockAt(testLoc.clone().add(0, -1, 0));
        if (!groundBlock.getType().isSolid()) {
            return false;
        }

        Vector direction = testLoc.clone().subtract(playerLoc).toVector();
        RayTraceResult result = world.rayTraceBlocks(playerLoc, direction, direction.length());
        if (result != null && result.getHitBlock() != null) {
            return false;
        }

        double distance = testLoc.distance(playerLoc);
        if (distance > 3.5 || distance < 1.5) {
            return false;
        }

        Block aboveBlock = world.getBlockAt(testLoc.clone().add(0, 2, 0));
        if (aboveBlock.getType().isSolid()) {
            return false;
        }

        double npcToPlayerYaw = Math.toDegrees(Math.atan2(playerLoc.getZ() - testLoc.getZ(), playerLoc.getX() - testLoc.getX()));
        double playerYaw = playerLoc.getYaw();
        double yawDifference = Math.abs(playerYaw - npcToPlayerYaw);

        if (yawDifference > 180) {
            yawDifference = 360 - yawDifference;
        }

        return yawDifference >= minYawDifference;
    }

    public static WrappedGameProfile createProfile(String name, String value, String signature) {
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
        WrappedGameProfile profile = new WrappedGameProfile(uuid, name);
        if (value != null && signature != null &&
                !value.trim().isEmpty() && !signature.trim().isEmpty()) {
            WrappedSignedProperty property = new WrappedSignedProperty("textures", value, signature);
            profile.getProperties().put("textures", property);
        }
        return profile;
    }

    public static String getRandomNpcName() {
        if (randomMode == RandomMode.TAB) {
            List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
            if (!onlinePlayers.isEmpty()) {
                return onlinePlayers.get(new Random().nextInt(onlinePlayers.size())).getName();
            }
        }

        if (!npcNames.isEmpty()) {
            return npcNames.get(new Random().nextInt(npcNames.size()));
        }

        return "NPC_" + new Random().nextInt(1000);
    }

    public static void removeNpcFor(Player player) {
        UUID playerId = player.getUniqueId();
        TrackedNpc npc = npcMap.remove(playerId);
        if (npc != null) {
            destroyNpc(player, npc.entityId);
        }
    }

    public static boolean hasActiveNpc(Player player) {
        return npcMap.containsKey(player.getUniqueId());
    }

    public static void refreshNpc(Player player) {
        if (hasActiveNpc(player)) {
            npcMap.get(player.getUniqueId()).lastUsed = System.currentTimeMillis();
        }
    }

    public static void clearAllNpcs() {
        for (Map.Entry<UUID, TrackedNpc> entry : npcMap.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                destroyNpc(player, entry.getValue().entityId);
            }
        }
        npcMap.clear();
    }

    public static boolean isNpc(Player player, int entityId) {
        TrackedNpc npc = npcMap.get(player.getUniqueId());
        return npc != null && npc.entityId == entityId;
    }

    public static MinecraftVersion getDetectedVersion() {
        return version;
    }

    public static boolean isModernVersion() {
        return isModernVersion;
    }

    private enum MinecraftVersion {
        V1_16, V1_17, V1_18, V1_19, V1_19_3, V1_20, V1_21, UNKNOWN
    }

    public enum RotationMode {
        DEFAULT, SMART, NEW
    }

    public static class TrackedNpc {
        public final int entityId;
        public final WrappedGameProfile profile;
        public volatile long lastUsed;
        public Location lastLocation;
        public boolean isJumping = false;
        public long jumpStartTime = 0;

        public boolean locatedToPlayer = false;

        public TrackedNpc(int entityId, WrappedGameProfile profile, long lastUsed) {
            this.entityId = entityId;
            this.profile = profile;
            this.lastUsed = lastUsed;
            this.lastLocation = null;
        }
    }
}