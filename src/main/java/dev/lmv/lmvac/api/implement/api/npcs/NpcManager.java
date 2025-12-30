package dev.lmv.lmvac.api.implement.api.npcs;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.*;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.utils.simulation.MovementUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static dev.lmv.lmvac.api.implement.api.npcs.RandomArmorGenerator.sendRandomArmor;

public class NpcManager {

    public static NPCNameManager nameManager;

    private static final List<Check> remoteCheks = new CopyOnWriteArrayList<>();

    public static final ConcurrentHashMap<UUID, TrackedNpc> npcMap = new ConcurrentHashMap<>();
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
    public static List<String> npcNames = new CopyOnWriteArrayList<>();
    public static int armorChance = 50;

    public static int updateTicks = 2;

    public static boolean smoothAim_Enabled = true;
    public static int smoothAim_Duration = 4;

    public static boolean handShake_Enabled = true;
    public static long handShake_diff = 580;
    public static int handShake_add = 50;

    public static boolean armorVirtualization_Enabled = true;

    private static BukkitTask updater;

    private static String nmsVersion;
    private static boolean useReflection = true;

    private static double MIN_DISTANCE = 1.5;
    private static double MAX_DISTANCE = 3.5;
    private static double DEFAULT_DISTANCE = 2.5;
    private static final double FLYING_MAX_DISTANCE = 35.0;

    private static double MIN_ANGLE_DEGREES = -45;
    private static double MAX_ANGLE_DEGREES = 45;

    private static final long MAX_TIMEOUT_MS = 60000;

    public NpcManager(JavaPlugin pl) {
        plugin = pl;
        detectVersion();
        initializeNMSVersion();
        loadSettings(plugin);

        startUpdater();
        startListeners();

        nameManager = new NPCNameManager(plugin);
    }

    public static void loadSettings(Plugin plugin) {
        ConfigurationSection npc = plugin.getConfig().getConfigurationSection("npc");
        ConfigurationSection handShake = npc.getConfigurationSection("hand-shake");
        try {
            handShake_Enabled = handShake.getBoolean("enabled", true);
            handShake_diff = handShake.getLong("min-diff", 580);
            handShake_add = handShake.getInt("random-add", 50);
        } catch (Exception e) {
            handShake_Enabled = true;
            handShake_diff = 580;
            handShake_add = 50;
        }

        ConfigurationSection smoothAim = npc.getConfigurationSection("smooth-aim");
        try {
            smoothAim_Enabled = smoothAim.getBoolean("enabled", true);
            smoothAim_Duration = smoothAim.getInt("per-ticks", 4);
        } catch (Exception e) {
            smoothAim_Enabled = false;
            smoothAim_Duration = 2;
        }

        try {
            armorVirtualization_Enabled = npc.getBoolean("armor-virtualization", true);
        } catch (Exception e) {
            armorVirtualization_Enabled = true;
        }

        strafe = npc.getBoolean("strafe", false);

        String rotMode = npc.getString("rotation-mode", "default").toLowerCase();
        rotationMode = rotMode.equals("smart") ? RotationMode.SMART : RotationMode.DEFAULT;

        String aimModeStr = npc.getString("aim-mode", "old").toLowerCase();
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

        lookPattern = npc.getInt("pattern.look", 9);
        smoothPattern = npc.getInt("pattern.smooth", -1);
        defaultPattern = npc.getInt("pattern.default", 5);

        String randMode = npc.getString("random.mode", "TAB").toUpperCase();
        randomMode = RandomMode.valueOf(randMode.toUpperCase()) != null ? RandomMode.valueOf(randMode.toUpperCase()) : RandomMode.TAB;

        String namesStr = npc.getString("names", "");
        if (!namesStr.isEmpty()) {
            npcNames = new CopyOnWriteArrayList<>(Arrays.asList(namesStr.split(";")));
        } else {
            npcNames = new CopyOnWriteArrayList<>(Arrays.asList(
                    "akvi4", "IIuoner", "Error404", "M7trix", "bmw3000", "repeat3000",
                    "gercog228", "EllO1", "RErEreo", "OreO3020", "OrEo998", "MenTos3021",
                    "Cola3029", "Pepsi1023", "Elonmusk300", "Pr302er", "moderator1029",
                    "moder392", "shipuchka228", "Pon30928", "KIllka392", "Penit3000",
                    "hlebushek9", "vmer23900", "wwwxyz", "LoopRepeat", "Mann3029", "Eplka302r"
            ));
        }

        long timeout = npc.getLong("timeout", 3000);
        try {
            TIMEOUT_MS = timeout;
        } catch (Exception e) {
            TIMEOUT_MS = 3000;
        }

        int rotUpd = npc.getInt("rotation-update", 2);
        try {
            updateTicks = rotUpd;
        } catch (Exception e) {
            updateTicks = 2;
        }

        armorChance = npc.getInt("armor-chance", 70);
        spawnMode = RotationMode.valueOf(npc.getString("spawn-mode", "SMART").toUpperCase());

        ConfigurationSection angle = npc.getConfigurationSection("angle");
        try {
            MIN_ANGLE_DEGREES = angle.getDouble("min", -45);
            MAX_ANGLE_DEGREES = angle.getDouble("max", 45);
        } catch (Exception e) {
            MIN_ANGLE_DEGREES = -45;
            MAX_ANGLE_DEGREES = 45;
        }

        ConfigurationSection distance = npc.getConfigurationSection("distance");
        try {
            MAX_DISTANCE = distance.getDouble("max",3.5);
            DEFAULT_DISTANCE = distance.getDouble("default",2.5);
            MIN_DISTANCE = distance.getDouble("min",1.5);
        } catch (Exception e) {
            MAX_DISTANCE = 3.5;
            DEFAULT_DISTANCE = 2.5;
            MIN_DISTANCE = 1.5;
        }
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

    private static void initializeNMSVersion() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            nmsVersion = packageName.substring(packageName.lastIndexOf('.') + 1);
            plugin.getLogger().info("NMS Version: " + nmsVersion);
        } catch (Exception e) {
            nmsVersion = "";
            useReflection = false;
            plugin.getLogger().warning("Не удалось определить NMS версию, виртуализация будет ограничена");
        }
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
            addPlayerToTabListWithPing(player, profile, name);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    if (!player.isOnline()) return;
                    spawnPlayerEntity(player, entityId, profile, loc);
                    rotateHead(player, entityId, loc);
                    sendNpcHealth(player, entityId);
                    if (Math.random() * 100 < armorChance) sendVirtualizedArmor(player, entityId);
                } catch (Exception e) {
                }
            }, 3L);

            TrackedNpc trackedNpc = new TrackedNpc(entityId, profile, now, TIMEOUT_MS);
            trackedNpc.lastLocation = loc.clone();
            npcMap.put(playerId, trackedNpc);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void spawnNpcFor(Player player, String name, WrappedGameProfile profile, long livetime) {
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
            addPlayerToTabListWithPing(player, profile, name);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    if (!player.isOnline()) return;
                    spawnPlayerEntity(player, entityId, profile, loc);
                    rotateHead(player, entityId, loc);
                    sendNpcHealth(player, entityId);
                    if (Math.random() * 100 < armorChance) sendVirtualizedArmor(player, entityId);
                } catch (Exception e) {
                }
            }, 3L);

            TrackedNpc trackedNpc = new TrackedNpc(entityId, profile, now, livetime);
            trackedNpc.lastLocation = loc.clone();
            npcMap.put(playerId, trackedNpc);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendVirtualizedArmor(Player player, int entityId) {
        try {
            PacketContainer equipmentPacket = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
            equipmentPacket.getIntegers().write(0, entityId);

            Random rand = new Random();
            List<Pair<EnumWrappers.ItemSlot, ItemStack>> equipment = new ArrayList<>();

            Material[] helmetTypes = {Material.DIAMOND_HELMET, Material.NETHERITE_HELMET, Material.IRON_HELMET};
            Material[] chestTypes = {Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE, Material.IRON_CHESTPLATE};
            Material[] legsTypes = {Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS, Material.IRON_LEGGINGS};
            Material[] bootsTypes = {Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS, Material.IRON_BOOTS};

            equipment.add(new Pair<>(EnumWrappers.ItemSlot.HEAD,
                    createVirtualizedArmor(helmetTypes[rand.nextInt(helmetTypes.length)])));
            equipment.add(new Pair<>(EnumWrappers.ItemSlot.CHEST,
                    createVirtualizedArmor(chestTypes[rand.nextInt(chestTypes.length)])));
            equipment.add(new Pair<>(EnumWrappers.ItemSlot.LEGS,
                    createVirtualizedArmor(legsTypes[rand.nextInt(legsTypes.length)])));
            equipment.add(new Pair<>(EnumWrappers.ItemSlot.FEET,
                    createVirtualizedArmor(bootsTypes[rand.nextInt(bootsTypes.length)])));

            if (rand.nextInt(100) < 60) {
                Material[] weaponTypes = {Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.IRON_SWORD};
                equipment.add(new Pair<>(EnumWrappers.ItemSlot.MAINHAND,
                        createVirtualizedWeapon(weaponTypes[rand.nextInt(weaponTypes.length)])));
            }

            equipmentPacket.getSlotStackPairLists().write(0, equipment);
            sendPacketSafely(player, equipmentPacket);
        } catch (Exception e) {
            sendRandomArmor(player, entityId);
        }
    }

    private static ItemStack createVirtualizedArmor(Material material) {
        ItemStack item = new ItemStack(material);
        Random rand = new Random();

        try {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (meta instanceof org.bukkit.inventory.meta.Damageable) {
                    org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) meta;
                    int maxDurability = material.getMaxDurability();
                    if (maxDurability > 0) {
                        int damage = rand.nextInt(maxDurability / 5);
                        damageable.setDamage(damage);
                    }
                }

                Map<org.bukkit.enchantments.Enchantment, Integer> enchants = getRandomArmorEnchantments(material, rand);
                for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : enchants.entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }

                if (rand.nextInt(100) < 30) {
                    try {
                        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                    } catch (Exception ignored) {}
                }

                if (rand.nextInt(100) < 15) {
                    try {
                        meta.setUnbreakable(true);
                        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE);
                    } catch (Exception ignored) {}
                }

                if (rand.nextInt(100) < 20) {
                    try {
                        meta.setCustomModelData(rand.nextInt(1000) + 1);
                    } catch (Exception ignored) {}
                }

                if (rand.nextInt(100) < 40) {
                    try {
                        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
                    } catch (Exception ignored) {}
                }

                item.setItemMeta(meta);
            }

            if (useReflection && !nmsVersion.isEmpty()) {
                item = virtualizeItemNBT(item, rand, false);
            }

        } catch (Exception ignored) {
        }

        return item;
    }

    private static void sendNpcHealth(Player player, int entityId) {
        try {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
            packet.getIntegers().write(0, entityId);

            List<WrappedWatchableObject> metadata = new ArrayList<>();

            int healthIndex = 8;

            float randomHealth = (float) (1 + new Random().nextInt(20));

            WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(Float.class);

            WrappedWatchableObject healthObject = new WrappedWatchableObject(
                    new WrappedDataWatcher.WrappedDataWatcherObject(healthIndex, serializer),
                    randomHealth
            );

            metadata.add(healthObject);

            packet.getWatchableCollectionModifier().write(0, metadata);
            sendPacketSafely(player, packet);
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось отправить здоровье для NPC: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static ItemStack createVirtualizedWeapon(Material material) {
        ItemStack item = new ItemStack(material);
        Random rand = new Random();

        try {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (meta instanceof org.bukkit.inventory.meta.Damageable) {
                    org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) meta;
                    int maxDurability = material.getMaxDurability();
                    if (maxDurability > 0) {
                        int damage = rand.nextInt(maxDurability / 4);
                        damageable.setDamage(damage);
                    }
                }

                Map<org.bukkit.enchantments.Enchantment, Integer> enchants = getRandomWeaponEnchantments(rand);
                for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : enchants.entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }

                if (rand.nextInt(100) < 25) {
                    try {
                        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                    } catch (Exception ignored) {}
                }

                if (rand.nextInt(100) < 10) {
                    try {
                        meta.setUnbreakable(true);
                        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE);
                    } catch (Exception ignored) {}
                }

                if (rand.nextInt(100) < 35) {
                    try {
                        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
                    } catch (Exception ignored) {}
                }

                item.setItemMeta(meta);
            }

            if (useReflection && !nmsVersion.isEmpty()) {
                item = virtualizeItemNBT(item, rand, true);
            }

        } catch (Exception ignored) {
        }

        return item;
    }

    private static ConcurrentHashMap<org.bukkit.enchantments.Enchantment, Integer> getRandomArmorEnchantments(Material material, Random rand) {
        ConcurrentHashMap<org.bukkit.enchantments.Enchantment, Integer> enchants = new ConcurrentHashMap<>();

        boolean isHelmet = material.name().contains("HELMET");
        boolean isChestplate = material.name().contains("CHESTPLATE");
        boolean isLeggings = material.name().contains("LEGGINGS");
        boolean isBoots = material.name().contains("BOOTS");

        if (rand.nextInt(100) < 75) {
            enchants.put(org.bukkit.enchantments.Enchantment.PROTECTION_ENVIRONMENTAL, rand.nextInt(4) + 1);
        }

        if (rand.nextInt(100) < 55) {
            enchants.put(org.bukkit.enchantments.Enchantment.DURABILITY, rand.nextInt(3) + 1);
        }

        if (rand.nextInt(100) < 45) {
            enchants.put(org.bukkit.enchantments.Enchantment.MENDING, 1);
        }

        if (rand.nextInt(100) < 30) {
            int protType = rand.nextInt(4);
            switch (protType) {
                case 0:
                    enchants.put(org.bukkit.enchantments.Enchantment.PROTECTION_FIRE, rand.nextInt(4) + 1);
                    break;
                case 1:
                    enchants.put(org.bukkit.enchantments.Enchantment.PROTECTION_EXPLOSIONS, rand.nextInt(4) + 1);
                    break;
                case 2:
                    enchants.put(org.bukkit.enchantments.Enchantment.PROTECTION_PROJECTILE, rand.nextInt(4) + 1);
                    break;
            }
        }

        if (isHelmet) {
            if (rand.nextInt(100) < 35) {
                enchants.put(org.bukkit.enchantments.Enchantment.OXYGEN, rand.nextInt(3) + 1);
            }
            if (rand.nextInt(100) < 30) {
                enchants.put(org.bukkit.enchantments.Enchantment.WATER_WORKER, 1);
            }
            if (rand.nextInt(100) < 25) {
                try {
                    enchants.put(org.bukkit.enchantments.Enchantment.getByName("thorns"), rand.nextInt(3) + 1);
                } catch (Exception ignored) {}
            }
        }

        if (isChestplate) {
            if (rand.nextInt(100) < 20) {
                try {
                    enchants.put(org.bukkit.enchantments.Enchantment.getByName("thorns"), rand.nextInt(3) + 1);
                } catch (Exception ignored) {}
            }
        }

        if (isLeggings) {
            if (rand.nextInt(100) < 25) {
                try {
                    enchants.put(org.bukkit.enchantments.Enchantment.getByName("thorns"), rand.nextInt(3) + 1);
                } catch (Exception ignored) {}
            }
        }

        if (isBoots) {
            if (rand.nextInt(100) < 45) {
                enchants.put(org.bukkit.enchantments.Enchantment.PROTECTION_FALL, rand.nextInt(4) + 1);
            }
            if (rand.nextInt(100) < 40) {
                enchants.put(org.bukkit.enchantments.Enchantment.DEPTH_STRIDER, rand.nextInt(3) + 1);
            }
            if (rand.nextInt(100) < 20) {
                try {
                    enchants.put(org.bukkit.enchantments.Enchantment.getByName("soul_speed"), rand.nextInt(3) + 1);
                } catch (Exception ignored) {}
            }
            if (rand.nextInt(100) < 15) {
                try {
                    enchants.put(org.bukkit.enchantments.Enchantment.getByName("frost_walker"), rand.nextInt(2) + 1);
                } catch (Exception ignored) {}
            }
        }

        return enchants;
    }

    private static ConcurrentHashMap<org.bukkit.enchantments.Enchantment, Integer> getRandomWeaponEnchantments(Random rand) {
        ConcurrentHashMap<org.bukkit.enchantments.Enchantment, Integer> enchants = new ConcurrentHashMap<>();

        if (rand.nextInt(100) < 70) {
            enchants.put(org.bukkit.enchantments.Enchantment.DAMAGE_ALL, rand.nextInt(5) + 1);
        }

        if (rand.nextInt(100) < 50) {
            enchants.put(org.bukkit.enchantments.Enchantment.DURABILITY, rand.nextInt(3) + 1);
        }

        if (rand.nextInt(100) < 40) {
            enchants.put(org.bukkit.enchantments.Enchantment.MENDING, 1);
        }

        if (rand.nextInt(100) < 35) {
            int dmgType = rand.nextInt(3);
            switch (dmgType) {
                case 0:
                    enchants.put(org.bukkit.enchantments.Enchantment.DAMAGE_ARTHROPODS, rand.nextInt(5) + 1);
                    break;
                case 1:
                    enchants.put(org.bukkit.enchantments.Enchantment.DAMAGE_UNDEAD, rand.nextInt(5) + 1);
                    break;
            }
        }

        if (rand.nextInt(100) < 45) {
            enchants.put(org.bukkit.enchantments.Enchantment.LOOT_BONUS_MOBS, rand.nextInt(3) + 1);
        }

        if (rand.nextInt(100) < 30) {
            enchants.put(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, rand.nextInt(2) + 1);
        }

        if (rand.nextInt(100) < 25) {
            enchants.put(org.bukkit.enchantments.Enchantment.KNOCKBACK, rand.nextInt(2) + 1);
        }

        if (rand.nextInt(100) < 20) {
            try {
                enchants.put(org.bukkit.enchantments.Enchantment.getByName("sweeping"), rand.nextInt(3) + 1);
            } catch (Exception ignored) {}
        }

        return enchants;
    }

    private static ItemStack virtualizeItemNBT(ItemStack item, Random rand, boolean isWeapon) {
        try {
            Class<?> craftItemStackClass = getCraftBukkitClass("inventory.CraftItemStack");
            Method asNMSCopyMethod = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
            Method asBukkitCopyMethod = craftItemStackClass.getMethod("asBukkitCopy", getNMSClass("ItemStack"));

            Object nmsItem = asNMSCopyMethod.invoke(null, item);

            if (nmsItem != null) {
                Class<?> nmsItemClass = nmsItem.getClass();
                Method getOrCreateTagMethod = null;
                Method getTagMethod = null;
                Method setTagMethod = null;

                try {
                    getOrCreateTagMethod = nmsItemClass.getMethod("getOrCreateTag");
                } catch (NoSuchMethodException e) {
                    try {
                        getTagMethod = nmsItemClass.getMethod("getTag");
                        setTagMethod = nmsItemClass.getMethod("setTag", getNMSClass("NBTTagCompound"));
                    } catch (Exception ignored) {}
                }

                Object nbtTag = null;
                if (getOrCreateTagMethod != null) {
                    nbtTag = getOrCreateTagMethod.invoke(nmsItem);
                } else if (getTagMethod != null) {
                    nbtTag = getTagMethod.invoke(nmsItem);
                    if (nbtTag == null) {
                        Class<?> nbtTagCompoundClass = getNMSClass("NBTTagCompound");
                        nbtTag = nbtTagCompoundClass.newInstance();
                    }
                }

                if (nbtTag != null) {
                    Class<?> nbtTagCompoundClass = nbtTag.getClass();

                    Method setIntMethod = nbtTagCompoundClass.getMethod("setInt", String.class, int.class);
                    Method setStringMethod = nbtTagCompoundClass.getMethod("setString", String.class, String.class);
                    Method setBooleanMethod = null;
                    try {
                        setBooleanMethod = nbtTagCompoundClass.getMethod("setBoolean", String.class, boolean.class);
                    } catch (Exception ignored) {}

                    setIntMethod.invoke(nbtTag, "RepairCost", rand.nextInt(10) + 1);

                    if (rand.nextInt(100) < 20) {
                        setIntMethod.invoke(nbtTag, "HideFlags", rand.nextInt(127) + 1);
                    }

                    if (rand.nextInt(100) < 15) {
                        setIntMethod.invoke(nbtTag, "CustomModelData", rand.nextInt(500) + 1);
                    }

                    if (rand.nextInt(100) < 25 && setBooleanMethod != null) {
                        setBooleanMethod.invoke(nbtTag, "Unbreakable", true);
                    }

                    addAttributeModifiers(nbtTag, item.getType(), rand, isWeapon);

                    if (setTagMethod != null) {
                        setTagMethod.invoke(nmsItem, nbtTag);
                    }
                }

                item = (ItemStack) asBukkitCopyMethod.invoke(null, nmsItem);
            }

        } catch (Exception ignored) {
        }

        return item;
    }

    private static void addAttributeModifiers(Object nbtTag, Material material, Random rand, boolean isWeapon) {
        try {
            Class<?> nbtTagCompoundClass = nbtTag.getClass();
            Class<?> nbtTagListClass = getNMSClass("NBTTagList");

            Method setMethod = nbtTagCompoundClass.getMethod("set", String.class, getNMSClass("NBTBase"));
            Object attributeList = nbtTagListClass.newInstance();
            Method addMethod = nbtTagListClass.getMethod("add", int.class, getNMSClass("NBTBase"));

            if (isWeapon) {
                Object damageModifier = createAttributeModifier("generic.attackDamage",
                        rand.nextDouble() * 3 + 5, rand);
                addMethod.invoke(attributeList, 0, damageModifier);

                Object speedModifier = createAttributeModifier("generic.attackSpeed",
                        rand.nextDouble() * 0.5 + 1.5, rand);
                addMethod.invoke(attributeList, 1, speedModifier);
            } else {
                String armorType = material.name().toLowerCase();
                double armorValue = 0;
                double toughnessValue = 0;

                if (armorType.contains("diamond")) {
                    armorValue = rand.nextDouble() * 1 + 2;
                    toughnessValue = rand.nextDouble() * 0.5 + 1.5;
                } else if (armorType.contains("netherite")) {
                    armorValue = rand.nextDouble() * 1 + 3;
                    toughnessValue = rand.nextDouble() * 1 + 2.5;
                } else if (armorType.contains("iron")) {
                    armorValue = rand.nextDouble() * 0.5 + 1.5;
                    toughnessValue = rand.nextDouble() * 0.3;
                }

                if (armorValue > 0) {
                    Object armorModifier = createAttributeModifier("generic.armor", armorValue, rand);
                    addMethod.invoke(attributeList, 0, armorModifier);
                }

                if (toughnessValue > 0) {
                    Object toughnessModifier = createAttributeModifier("generic.armorToughness", toughnessValue, rand);
                    addMethod.invoke(attributeList, 1, toughnessModifier);
                }

                if (armorType.contains("boots") && rand.nextInt(100) < 30) {
                    Object movementModifier = createAttributeModifier("generic.movementSpeed",
                            rand.nextDouble() * 0.01 + 0.01, rand);
                    addMethod.invoke(attributeList, 2, movementModifier);
                }
            }

            if (attributeList != null) {
                setMethod.invoke(nbtTag, "AttributeModifiers", attributeList);
            }

        } catch (Exception ignored) {
        }
    }

    private static Object createAttributeModifier(String attributeName, double amount, Random rand) {
        try {
            Class<?> nbtTagCompoundClass = getNMSClass("NBTTagCompound");
            Object modifier = nbtTagCompoundClass.newInstance();

            Method setStringMethod = nbtTagCompoundClass.getMethod("setString", String.class, String.class);
            Method setDoubleMethod = nbtTagCompoundClass.getMethod("setDouble", String.class, double.class);
            Method setIntMethod = nbtTagCompoundClass.getMethod("setInt", String.class, int.class);

            Method setIntArrayMethod = nbtTagCompoundClass.getMethod("setIntArray", String.class, int[].class);

            UUID uuid = UUID.randomUUID();
            long mostSigBits = uuid.getMostSignificantBits();
            long leastSigBits = uuid.getLeastSignificantBits();
            int[] uuidArray = new int[] {
                    (int)(mostSigBits >> 32),
                    (int)mostSigBits,
                    (int)(leastSigBits >> 32),
                    (int)leastSigBits
            };

            setStringMethod.invoke(modifier, "AttributeName", attributeName);
            setStringMethod.invoke(modifier, "Name", attributeName);
            setDoubleMethod.invoke(modifier, "Amount", amount);
            setIntMethod.invoke(modifier, "Operation", 0);
            setIntArrayMethod.invoke(modifier, "UUID", uuidArray);

            String[] slots = {"mainhand", "offhand", "feet", "legs", "chest", "head"};
            setStringMethod.invoke(modifier, "Slot", slots[rand.nextInt(slots.length)]);

            return modifier;

        } catch (Exception e) {
            return null;
        }
    }

    private static Class<?> getNMSClass(String className) throws ClassNotFoundException {
        if (nmsVersion.isEmpty()) {
            return Class.forName("net.minecraft." + className);
        }
        try {
            return Class.forName("net.minecraft.server." + nmsVersion + "." + className);
        } catch (ClassNotFoundException e) {
            return Class.forName("net.minecraft." + className);
        }
    }

    private static Class<?> getCraftBukkitClass(String className) throws ClassNotFoundException {
        return Class.forName("org.bukkit.craftbukkit." + nmsVersion + "." + className);
    }

    private static void addPlayerToTabListWithPing(Player player, WrappedGameProfile profile, String name) {
        Player onlinePlayer = Bukkit.getPlayer(name);
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            return;
        }

        try {
            int randomPing = new Random().nextInt(91) + 10;

            if (isModernVersion) {
                PacketContainer addPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);

                List<PlayerInfoData> addDataList = Arrays.asList(
                        new PlayerInfoData(
                                profile,
                                randomPing,
                                EnumWrappers.NativeGameMode.SURVIVAL,
                                WrappedChatComponent.fromText(name)
                        )
                );

                addPacket.getPlayerInfoDataLists().write(0, addDataList);
                sendPacketSafely(player, addPacket);

            } else {
                PacketContainer addPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
                addPacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
                List<PlayerInfoData> addDataList = Arrays.asList(
                        new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(name))
                );
                addPacket.getPlayerInfoDataLists().write(0, addDataList);
                sendPacketSafely(player, addPacket);

                PacketContainer latencyPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
                latencyPacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.UPDATE_LATENCY);
                List<PlayerInfoData> latencyDataList = Arrays.asList(
                        new PlayerInfoData(profile, randomPing, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(name))
                );
                latencyPacket.getPlayerInfoDataLists().write(0, latencyDataList);
                sendPacketSafely(player, latencyPacket);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось добавить NPC в таб с пингом: " + e.getMessage());
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
        } catch (Exception ignored) {
        }
    }

    private static void spawnPlayerEntity(Player player, int entityId, WrappedGameProfile profile, Location loc) {
        final double[] minDistance = {MIN_DISTANCE};
        final double[] distance = {FLYING_MAX_DISTANCE};
        if (spawnMode.equals(RotationMode.SMART)
                || spawnMode.equals(RotationMode.NEW)) {
            loc = getBehindPlayer(player, distance[0]);
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
                distance[0] -= 2.27;
                sendPacketSafely(player, spawn);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        distance[0] -= 2.27;
                        teleportNpc(player, npc, distance[0], true);
                        if (distance[0] <= minDistance[0]) {
                            this.cancel();
                            npc.locatedToPlayer = true;
                            return;
                        }
                    }
                }.runTaskTimer(plugin, 2L, 1L);
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
                        } catch (Exception ignored) {
                        }
                    }
                }
        );
    }

    public static void startUpdater() {
        if (updater != null) updater.cancel();
        updater = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
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
                if (now - npc.lastUsed > npc.liveTime) {
                    destroyNpc(player, npc.entityId);
                    it.remove();
                    continue;
                }
                updateNpcPosition(player, npc);
            }
        }, updateTicks, updateTicks);
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
                            plugin.getLogger().info("error [Destroy-Npc] : " + e3.getMessage());
                        }
                    }
                }
            } else {
                try {
                    destroy.getSpecificModifier(int[].class).write(0, new int[]{entityId});
                } catch (Exception e4) {
                    plugin.getLogger().info("error [Destroy-Npc] : " + e4.getMessage());
                }
            }

            sendPacketSafely(player, destroy);
        } catch (Exception ignored) {
        }
    }

    private static void handShake(Player player, int entityId, int animationType) {
        try {
            PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ANIMATION);
            packet.getIntegers().write(0, entityId);
            packet.getIntegers().write(1, animationType);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } catch (Exception ignored) {
        }
    }

    private static void teleportNpc(Player player, TrackedNpc npc, double distance, boolean isFlying) {
        Location newLoc;
        if (rotationMode == RotationMode.SMART) {
            newLoc = getSmartBehindPlayer(player, distance, npc, isFlying);
        } else {
            newLoc = getBehindPlayer(player, distance);
        }

        Location playerLoc = player.getLocation();
        Vector playerDirection = playerLoc.getDirection();
        Vector playerToNpcVector = newLoc.toVector().subtract(playerLoc.toVector());

        if (playerDirection.dot(playerToNpcVector) >= 0) {
            Vector parallelComponent = playerDirection.clone().multiply(playerDirection.dot(playerToNpcVector));
            Vector correctedVector = playerToNpcVector.subtract(parallelComponent.multiply(2));
            newLoc = playerLoc.clone().add(correctedVector);

            double currentMinDistance = isFlying ? MIN_DISTANCE * 0.5 : MIN_DISTANCE;
            if (newLoc.distance(playerLoc) < currentMinDistance) {
                newLoc = playerLoc.clone().add(correctedVector.normalize().multiply(currentMinDistance));
            }
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

            sendTeleportPacket(player, npc, newLoc);
            npc.lastLocation = newLoc.clone();
        }
    }

    private static void startSmoothMovement(Player player, TrackedNpc npc, Location targetLoc) {
        npc.isSmooth = true;
        npc.targetLocation = targetLoc.clone();
        npc.smoothStartLocation = npc.lastLocation.clone();
        npc.smoothCurrentTick = 0;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !npcMap.containsKey(player.getUniqueId())) {
                    npc.isSmooth = false;
                    this.cancel();
                    return;
                }

                npc.smoothCurrentTick++;

                if (npc.smoothCurrentTick >= smoothAim_Duration) {
                    sendTeleportPacket(player, npc, npc.targetLocation);
                    npc.lastLocation = npc.targetLocation.clone();
                    npc.isSmooth = false;
                    this.cancel();
                    return;
                }

                double progress = (double) npc.smoothCurrentTick / smoothAim_Duration;
                Location currentLoc = interpolateLocation(npc.smoothStartLocation, npc.targetLocation, progress);

                sendTeleportPacket(player, npc, currentLoc);
                npc.lastLocation = currentLoc.clone();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static Location interpolateLocation(Location start, Location end, double progress) {
        progress = Math.max(0.0, Math.min(1.0, progress));

        if (!start.getWorld().equals(end.getWorld())) {
            return start.clone();
        }

        Location result = start.clone();

        result.setX(start.getX() + (end.getX() - start.getX()) * progress);
        result.setY(start.getY() + (end.getY() - start.getY()) * progress);
        result.setZ(start.getZ() + (end.getZ() - start.getZ()) * progress);

        result.setYaw(lerpYaw(start.getYaw(), end.getYaw(), progress));
        result.setPitch((float) (start.getPitch() + (end.getPitch() - start.getPitch()) * progress));

        return result;
    }

    private static float lerpYaw(float start, float end, double progress) {
        float delta = ((end - start + 540) % 360) - 180;
        return start + delta * (float) progress;
    }

    private static void sendTeleportPacket(Player player, TrackedNpc npc, Location loc) {
        PacketContainer tp = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
        tp.getSpecificModifier(int.class).write(0, npc.entityId);
        tp.getSpecificModifier(double.class).write(0, loc.getX());
        tp.getSpecificModifier(double.class).write(1, loc.getY());
        tp.getSpecificModifier(double.class).write(2, loc.getZ());
        tp.getSpecificModifier(byte.class).write(0, (byte) (loc.getYaw() * 256.0F / 360.0F));
        tp.getSpecificModifier(byte.class).write(1, (byte) (loc.getPitch() * 256.0F / 360.0F));
        if (isNewestVersion) {
            try {
                tp.getBooleans().write(0, true);
            } catch (Exception ignored) {
            }
        }
        sendPacketSafely(player, tp);
        rotateHead(player, npc.entityId, loc);
    }

    private static void updateNpcPosition(Player player, TrackedNpc npc) {
        if (!npc.locatedToPlayer) {
            refreshNpc(player);
            return;
        }
        Location newLoc;
        if (rotationMode == RotationMode.SMART) {
            newLoc = getSmartBehindPlayer(player, DEFAULT_DISTANCE, npc, !npc.locatedToPlayer);
        } else {
            newLoc = getBehindPlayer(player);
        }

        Location playerLoc = player.getLocation();
        Vector playerDirection = playerLoc.getDirection();
        Vector playerToNpcVector = newLoc.toVector().subtract(playerLoc.toVector());

        if (playerDirection.dot(playerToNpcVector) >= 0) {
            Vector parallelComponent = playerDirection.clone().multiply(playerDirection.dot(playerToNpcVector));
            Vector correctedVector = playerToNpcVector.subtract(parallelComponent.multiply(2));
            newLoc = playerLoc.clone().add(correctedVector);

            if (MovementUtil.distance(newLoc,playerLoc) < MIN_DISTANCE) {
                newLoc = playerLoc.clone().add(correctedVector.normalize().multiply(MIN_DISTANCE));
            }
        }

        Location oldLoc = npc.lastLocation;
        long now = System.currentTimeMillis();
        if (shouldUpdateLocation(oldLoc, newLoc)) {
            if (smoothAim_Enabled && !npc.isSmooth) {
                startSmoothMovement(player, npc, newLoc);
                return;
            }

            if (rotationMode == RotationMode.SMART && Math.random() < 0.1) {
                if (handShake_Enabled) {
                    long random = new Random().nextInt(handShake_add);
                    if (now - npc.handShake > (handShake_diff + random)) {
                        handShake(player, npc.entityId, 0);
                        npc.handShake = now;
                    }
                }
                npc.isJumping = true;
                npc.jumpStartTime = now;
            }

            if (npc.isJumping) {
                long jumpTime = now - npc.jumpStartTime;
                if (jumpTime < 500) {
                    double jumpHeight = Math.sin((jumpTime / 500.0) * Math.PI) * 1.2;
                    newLoc.add(0, jumpHeight, 0);
                } else {
                    npc.isJumping = false;
                }
            }

            double distance = MovementUtil.distance(newLoc,player.getLocation());
            npc.isPhantom = distance < 1.5;

            sendTeleportPacket(player, npc, newLoc);
            npc.lastLocation = newLoc.clone();

            if (npc.isPhantom) {
                makeNpcPhantom(player, npc.entityId);
            }
        }
    }

    private static void makeNpcPhantom(Player player, int entityId) {
        try {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
            packet.getIntegers().write(0, entityId);

            List<WrappedWatchableObject> metadata = new ArrayList<>();

            metadata.add(new WrappedWatchableObject(0, (byte) 0x20));

            packet.getWatchableCollectionModifier().write(0, metadata);
            sendPacketSafely(player, packet);
        } catch (Exception ignored) {
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
        return getBehindPlayer(player, DEFAULT_DISTANCE);
    }

    private static Location getBehindPlayer(Player player, double distance) {
        Location loc = player.getLocation().clone();
        float yaw = loc.getYaw();

        double angle = MIN_ANGLE_DEGREES + (MAX_ANGLE_DEGREES - MIN_ANGLE_DEGREES) / 2;
        double radianYaw = Math.toRadians(yaw + 180 + angle);

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

    private static Location getSmartBehindPlayer(Player player, TrackedNpc npc) {
        return getSmartBehindPlayer(player, DEFAULT_DISTANCE, npc, false);
    }

    private static Location getSmartBehindPlayer(Player player, double distance, TrackedNpc npc, boolean isFlying) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();

        if (!npc.locatedToPlayer) {
            return getBehindPlayer(player, distance);
        }

        double currentMaxDistance = isFlying ? FLYING_MAX_DISTANCE : MAX_DISTANCE;

        int maxAttempts = (int)((MAX_ANGLE_DEGREES - MIN_ANGLE_DEGREES) / 45) + 1;

        for (int attempts = 0; attempts < maxAttempts; attempts++) {
            Location testLoc = calculateBehindLocation(player, distance, attempts);

            if (isLocationSuitable(world, testLoc, playerLoc, 50, npc, currentMaxDistance)) {
                return testLoc;
            }
        }

        return getBehindPlayer(player, distance);
    }

    private static Location getSmartBehindPlayer(Player player, double distance, double offsetX, double offsetZ, TrackedNpc npc) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();

        for (int attempts = 0; attempts < 8; attempts++) {
            Location testLoc = calculateBehindLocationWithOffset(player, distance, offsetX, offsetZ, attempts);

            if (isLocationSuitable(world, testLoc, playerLoc, 50, npc, MAX_DISTANCE)) {
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

        Location baseLoc = playerLoc.clone().add(direction.clone().multiply(DEFAULT_DISTANCE));

        baseLoc.add(right.clone().multiply(offsetX));
        baseLoc.add(direction.clone().multiply(offsetZ));

        return baseLoc;
    }

    private static Location calculateBehindLocation(Player player, double baseDistance, int attempt) {
        Location loc = player.getLocation().clone();
        float yaw = loc.getYaw();
        World world = player.getWorld();

        double angleRange = MAX_ANGLE_DEGREES - MIN_ANGLE_DEGREES;
        double angleOffset = MIN_ANGLE_DEGREES + (attempt * (angleRange / 8)) % angleRange;
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

    private static boolean isLocationSuitableRW(World world, Location loc, Location playerLoc, TrackedNpc npc) {
        double distance = MovementUtil.distance(loc, playerLoc);

        if (!npc.locatedToPlayer) {
            return true;
        }
        if ((distance > MAX_DISTANCE && npc.locatedToPlayer) || distance < MIN_DISTANCE) {
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

    private static boolean isLocationSuitable(World world, Location testLoc, Location playerLoc, float minYawDifference, TrackedNpc npc, double maxDistance) {
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

        double distance = MovementUtil.distance(testLoc, playerLoc);
        if (!npc.locatedToPlayer) {
            return true;
        }

        if (distance > maxDistance || distance < MIN_DISTANCE) {
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
        try {
            if (plugin == null || !plugin.isEnabled()) {
                return null;
            }

            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
            WrappedGameProfile profile = new WrappedGameProfile(uuid, name);
            if (value != null && signature != null &&
                    !value.trim().isEmpty() && !signature.trim().isEmpty()) {
                WrappedSignedProperty property = new WrappedSignedProperty("textures", value, signature);
                profile.getProperties().put("textures", property);
            }
            return profile;
        } catch (Exception e) {
            return null;
        }
    }

    public static String getRandomNpcName() {
        if (randomMode == RandomMode.TAB) {
            List<Player> onlinePlayers = new CopyOnWriteArrayList<>(Bukkit.getOnlinePlayers());
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

    public enum AimMode {
        OLD, NEW, MIDDLE
    }

    public static class TrackedNpc {
        public final int entityId;
        public final WrappedGameProfile profile;
        public volatile long lastUsed;
        public Location lastLocation;
        public boolean isJumping = false;
        public long jumpStartTime = 0;

        public boolean locatedToPlayer = false;
        public long handShake = 0;
        public boolean isPhantom = false;

        public boolean isSmooth = false;
        public Location targetLocation;
        public Location smoothStartLocation;
        public int smoothCurrentTick = 0;

        public final long liveTime;

        public TrackedNpc(int entityId, WrappedGameProfile profile, long lastUsed, long liveTime) {
            this.entityId = entityId;
            this.profile = profile;
            this.lastUsed = lastUsed;
            this.lastLocation = null;

            boolean validLiveTimeDown = liveTime > 0;
            boolean validLiveTimeUp = liveTime <= MAX_TIMEOUT_MS;
            if (!validLiveTimeDown) {
                this.liveTime = TIMEOUT_MS;
            } else if (!validLiveTimeUp) {
                this.liveTime = MAX_TIMEOUT_MS;
            } else {
                this.liveTime = liveTime;
            }
        }
    }
}