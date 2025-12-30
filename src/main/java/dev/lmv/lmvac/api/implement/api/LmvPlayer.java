package dev.lmv.lmvac.api.implement.api;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.PacketType.Play.Client;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.injector.temporary.TemporaryPlayer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.lmv.lmvac.LmvAC;
import dev.lmv.lmvac.api.ConfigManager;
import dev.lmv.lmvac.api.commands.Main_Command;
import dev.lmv.lmvac.api.implement.api.packetListeners.InventoryListener;
import dev.lmv.lmvac.api.implement.checks.custom.CustomCheckManager;
import dev.lmv.lmvac.api.implement.checks.other.CheckManager;
import dev.lmv.lmvac.api.implement.checks.other.PlayerRotationData;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.utils.ProtocolUtil;
import dev.lmv.lmvac.api.implement.utils.listeners.MovementListener;
import dev.lmv.lmvac.api.implement.utils.setback.SetBackUtil;
import dev.lmv.lmvac.api.implement.utils.simulation.SimulationUtil;
import dev.lmv.lmvac.api.implement.utils.text.ColorUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import static dev.lmv.lmvac.api.implement.utils.math.MathUtil.averageDelta;
import static dev.lmv.lmvac.api.implement.utils.math.MathUtil.distanceXZ;

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
    public long lastSneakStop, lastSneakStart, lastStartSprint, lastStopSprint, lastStopSprintNano, lastStartSprintNano, lastGliding, lastFlying, nanoGliding, nanoFlying, lastSneak, lastSprint, lastRidingJump, nanoAttack, nanoArmAnimation, lastAlive, lastInventoryOpen, nanoEntityAction, lastWindowClick, lastWindowClose, lastMoveTime, lastPositionLook, lastLook, lastBlockDig, lastBlockPlace, lastArmAnimation, lastAbilities, lastEntityAction, lastHeldItem, lastPickItem, lastUseItem, lastTabComplete, lastVehicleMove, lastTeleportAccept, lastUseEntity, lastEnchantItem, lastBoatMove, lastSetCreativeSlot, lastGround, lastAttackPacket = 0L;
    public boolean hasSneak, hasSprint, isSneaking, wasSneaking, isSprinting, wasSprinting, isOnGround, wasOnGround, wasGliding, isGliding, isFlying, wasFlying, isAttacking, isArmAnimation, isWindowClosing, isWindowOpen, isMoving, isSWindowClosing, isRespawning, swithSlot, cancelAttack, rotating, isDigging, isPlacing, creativeSlot, tabCompleting, teleporting, usingEntity, usingItem, vehicleMoving, windowClicking, enchanting, boatMoving, wasClimbing, isClimbing, wasFluid, isFluid;
    public final List<Location> locations = new CopyOnWriteArrayList<>();
    public final List<Location> groundLocations = new CopyOnWriteArrayList<>();
    public PacketContainer lastClickPacket = null;
    public Location currentLocation;
    public Location groundLocation;
    public List<LookInformation> looks = new CopyOnWriteArrayList<>();
    public final Set<String> bypass = ConcurrentHashMap.newKeySet();
    private BukkitTask bypassUpdateTask;
    public boolean collision = false;

    public boolean simulationWalk, simulationJump, simulationSprint, simulationShield, simulationSneak,
            lastSimulationWalk, lastSimulationSprint, lastSimulationJump, lastSimulationShield, lastSimulationSneak,
            isSlowMovement, isSlowMovementT1, isSlowMovementT2, wasSlowMovement, wasSlowMovementT1, wasSlowMovementT2,
            lastSimulationStrafe, simulationStrafe, simulationSwim, lastSimulationSwim, simulationSwimJump, lastSimulationSwimJump,
            simulationClimb, lastSimulationClimb, wasLastSimulationWalk, wasLastSimulationSneak, wasLastSimulationShield,
            wasLastSimulationClimb, wasLastSimulationSwim;
    public double x, y, z, lastX, lastY, lastZ, lastDeltaZ, lastDeltaY, lastDeltaX, lastDeltaXZ;
    public float yaw, pitch, lastYaw, lastPitch;

    public long uAttackTime = 0L;
    public long u2AttackTime = 0L;
    public int invTick = 0;

    public PlayerRotationData rotationData = new PlayerRotationData();

    public boolean isInventoryOpened = false;
    public boolean isServerInventoryOpened = false;

    public double damageMultiple = 1.0;
    public PacketContainer lastPacket = null;

    public List<PositionInfo> positions = new CopyOnWriteArrayList<>();
    public ConcurrentHashMap<PacketType,List<Long>> packets = new ConcurrentHashMap<>();

    public static long orderA_maxPing = 929;

    public double deltaX() {
        return x - lastX;
    }

    public double deltaZ() {
        return z - lastZ;
    }

    public double deltaY() {
        return y - lastY;
    }

    public double deltaXZ() {
        return distanceXZ(lastX,x,lastZ,z);
    }

    public int level(PotionEffectType type) {
        PotionEffect effect = player.getPotionEffect(type);
        int level = effect != null ? effect.getAmplifier() +1 : 0;
        return level;
    }

    public void setBack() {
        SetBackUtil.sendSetBack(this,this.lastX,this.lastY,this.lastZ);
    }

    public static class PositionInfo {
        public PacketEvent event;
        public long time;
        public PositionInfo(PacketEvent moveEvent) {
            this.event = moveEvent;
            time = System.currentTimeMillis();
        }
    }

    public void setDamageMultiple(double multiple, long millis) {
        long now = System.currentTimeMillis();
        long newUnlockTime = now + millis;

        if (this.uAttackTime > newUnlockTime) {
            return;
        }

        this.damageMultiple = multiple;
        this.uAttackTime = newUnlockTime;
    }

    public void setCancelAttack(long millis) {
        long now = System.currentTimeMillis();
        long newUnlockTime = now + millis;

        if (this.u2AttackTime > newUnlockTime) {
            return;
        }

        this.cancelAttack = true;
        this.u2AttackTime = newUnlockTime;
    }


    public int inventoryMoves = -1;
    public ConcurrentHashMap<PacketType,Integer> inventoryMovesP = new ConcurrentHashMap<>();

    public boolean alerts = true;
    public long lastUpdateTarget = -1L;

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
    public LmvPlayer(Player player) {
        this.player = player;
        players.put(player.getEntityId(), this);
        this.bypassUpdateTask = Bukkit.getScheduler().runTaskTimer(LmvAC.getInstance(), this::updateBypassList, 0L, 600L);
    }

    public LmvPlayer(Plugin plugin) {
        LmvPlayer.plugin = plugin;
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
        return this.bypass.contains(checkName);
    }

    /**
     * Проверяет, телепортировался ли игрок недавно.
     * @return true, если с момента последней телепортации прошло меньше TELEPORT_COOLDOWN_MS.
     */
    public boolean isRecentlyTeleported() {
        return this.lastPacketIsTeleport();
    }

    public boolean lastPacketIsTeleport() {
        return this.lastPacket != null && this.lastPacket.getType().equals(Client.TELEPORT_ACCEPT);
    }

    public void updateRotation(float newYaw, float newPitch) {
        float lastYawTemp = rotationData.lastYaw;
        float lastPitchTemp = rotationData.lastPitch;

        rotationData.lastYaw = newYaw;
        rotationData.lastPitch = newPitch;

        float newDeltaYaw = newYaw - lastYawTemp;
        float newDeltaPitch = newPitch - lastPitchTemp;

        rotationData.deltaYaw = newDeltaYaw;
        rotationData.deltaPitch = newDeltaPitch;

        rotationData.deltaYaws.add((double) newDeltaYaw);
        rotationData.deltaPitches.add((double) newDeltaPitch);

        if (rotationData.deltaYaws.size() > 100) {
            rotationData.deltaYaws.remove(0);
            rotationData.deltaPitches.remove(0);
        }

        processCinematic();
    }

    private void processCinematic() {
        long now = System.currentTimeMillis();
        double differenceYaw = Math.abs((double)rotationData.deltaYaw - rotationData.lastDeltaXRot);
        double differencePitch = Math.abs((double)rotationData.deltaPitch - rotationData.lastDeltaYRot);
        double joltYaw = Math.abs(differenceYaw - (double)rotationData.deltaYaw);
        double joltPitch = Math.abs(differencePitch - (double)rotationData.deltaPitch);
        boolean cinematic = now - rotationData.lastHighRate > 250L || now - rotationData.lastSmooth < 9000L;

        if (joltYaw > 1.0 && joltPitch > 1.0) {
            rotationData.lastHighRate = now;
        }

        rotationData.yawSamples.add((double)rotationData.deltaYaw);
        rotationData.pitchSamples.add((double)rotationData.deltaPitch);

        if (rotationData.yawSamples.size() >= 20 && rotationData.pitchSamples.size() >= 20) {
            Set<Double> shannonYaw = new HashSet<>();
            Set<Double> shannonPitch = new HashSet<>();
            List<Double> stackYaw = new ArrayList<>();
            List<Double> stackPitch = new ArrayList<>();

            for (Double yawSample : rotationData.yawSamples) {
                stackYaw.add(yawSample);
                if (stackYaw.size() >= 10) {
                    shannonYaw.add(calculateShannonEntropy(stackYaw));
                    stackYaw.clear();
                }
            }

            for (Double pitchSample : rotationData.pitchSamples) {
                stackPitch.add(pitchSample);
                if (stackPitch.size() >= 10) {
                    shannonPitch.add(calculateShannonEntropy(stackPitch));
                    stackPitch.clear();
                }
            }

            if (shannonYaw.size() != 1 || shannonPitch.size() != 1 || !shannonYaw.toArray()[0].equals(shannonPitch.toArray()[0])) {
                rotationData.isTotallyNotCinematic = 20;
            }

            GraphResult resultsYaw = getGraph(rotationData.yawSamples);
            GraphResult resultsPitch = getGraph(rotationData.pitchSamples);

            int negativesYaw = resultsYaw.negatives;
            int negativesPitch = resultsPitch.negatives;
            int positivesYaw = resultsYaw.positives;
            int positivesPitch = resultsPitch.positives;

            if (positivesYaw > negativesYaw || positivesPitch > negativesPitch) {
                rotationData.lastSmooth = now;
            }

            rotationData.yawSamples.clear();
            rotationData.pitchSamples.clear();
        }

        if (rotationData.isTotallyNotCinematic > 0) {
            --rotationData.isTotallyNotCinematic;
            rotationData.cinematicRotation = false;
        } else {
            rotationData.cinematicRotation = cinematic;
        }

        rotationData.lastDeltaXRot = (double)rotationData.deltaYaw;
        rotationData.lastDeltaYRot = (double)rotationData.deltaPitch;
    }

    private double calculateShannonEntropy(List<Double> data) {
        if (data.isEmpty()) return 0.0;
        Map<Double, Integer> frequencyMap = new HashMap<>();
        for (Double value : data) {
            frequencyMap.put(value, frequencyMap.getOrDefault(value, 0) + 1);
        }
        double entropy = 0.0;
        int total = data.size();
        for (Integer count : frequencyMap.values()) {
            double probability = (double) count / total;
            entropy -= probability * (Math.log(probability) / Math.log(2));
        }
        return entropy;
    }

    private GraphResult getGraph(List<Double> data) {
        int positives = 0;
        int negatives = 0;
        for (int i = 1; i < data.size(); i++) {
            double diff = data.get(i) - data.get(i - 1);
            if (diff > 0) positives++;
            else if (diff < 0) negatives++;
        }
        return new GraphResult(positives, negatives);
    }

    private static class GraphResult {
        int positives;
        int negatives;
        GraphResult(int positives, int negatives) {
            this.positives = positives;
            this.negatives = negatives;
        }
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
            String checkName = check.getName();

            if (this.player.hasPermission("lmvac.bypass." + (checkName.toLowerCase()))) {
                this.bypass.add(checkName);
            }
        }

        for (Check check : CustomCheckManager.loadedChecks.values()) {
            String checkName = check.getName().toLowerCase();

            if (this.player.hasPermission("lmvac.bypass." + (checkName.toLowerCase()))) {
                this.bypass.add(checkName);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int id = player.getEntityId();
        plID.put(id, player);
        LmvPlayer client = players.get(id);
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
            player.sendMessage(ColorUtil.setColorCodes("&b█░░ █▀▄▀█ █░█ ▄▀█ █▀▀"));
            player.sendMessage(ColorUtil.setColorCodes("&b█▄▄ █░▀░█ ▀▄▀ █▀█ █▄▄"));
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

    public boolean isInCombat() {
        long now = System.currentTimeMillis();
        return (now - this.lastAttackPacket) < 5000;
    }

    public double calculateDistinct(List<Double> values) {
        if (values.isEmpty()) return 100.0;

        Set<Double> uniqueValues = new HashSet<>();
        for (Double value : values) {
            uniqueValues.add(Math.round(value * 100.0) / 100.0);
        }

        return (uniqueValues.size() / (double) values.size()) * 100.0;
    }

    public boolean isCinematicRotation() {
        PlayerRotationData data = this.rotationData;
        if (data == null) return false;

        if (data.deltaYaws.size() >= 10 && data.deltaPitches.size() >= 10) {
            double avgYaw = data.deltaYaws.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double avgPitch = data.deltaPitches.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

            return avgYaw < 0.5 && avgPitch < 0.5;
        }

        return false;
    }

    public boolean isSimulating() {
        long now = System.currentTimeMillis();
        return now-this.lastEntityAction < 1000 || now-this.lastUseEntity < 1000
                || now-this.lastLook < 1000 || now-this.lastHeldItem < 1000 || now-this.lastUseItem < 1000;
    }

    public boolean isMovePacketing(int locs, long time) {
        long now = System.currentTimeMillis();
        List<LmvPlayer.PositionInfo> positions = this.positions;
        positions.removeIf(k -> now - k.time >= time);
        return positions.size()>=locs;
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

    public boolean isSpeedSimulation() {
        List<Location> locs = MovementListener.lastLocations.getOrDefault(player.getUniqueId(), new ArrayList<>());

        if (locs.size() < 2) {
            return false;
        }

        Location from = locs.get(locs.size() - 2);
        Location to = player.getLocation();

        double distX = Math.abs(from.getX() - to.getX());
        double distZ = Math.abs(from.getZ() - to.getZ());
        double distXZ = Math.sqrt(distX * distX + distZ * distZ);

        AttributeInstance speedAttribute = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        double baseSpeed = (!this.isOnGround && !this.wasOnGround) ? ((speedAttribute != null) ? speedAttribute.getBaseValue() : 0.1) : ((speedAttribute != null) ? speedAttribute.getValue() : 0.1);

        double thresholdMultiplier = 4.1;
        if (this.hasSprint && (this.isOnGround || this.wasOnGround)) baseSpeed /= 1.3;

        double calculatedThreshold = baseSpeed * thresholdMultiplier;
        double finalThreshold = calculatedThreshold + 0.05;

        return distXZ > finalThreshold;
    }

    public boolean isSpeedSimulation(double s) {
        List<Location> locs = MovementListener.lastLocations.getOrDefault(player.getUniqueId(), new ArrayList<>());

        if (locs.size() < 2) {
            return false;
        }

        Location from = locs.get(locs.size() - 2);
        Location to = player.getLocation();

        double distX = Math.abs(from.getX() - to.getX());
        double distZ = Math.abs(from.getZ() - to.getZ());
        double distXZ = Math.sqrt(distX * distX + distZ * distZ);

        AttributeInstance speedAttribute = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        double baseSpeed = (!this.isOnGround && !this.wasOnGround) ? ((speedAttribute != null) ? speedAttribute.getBaseValue() : 0.1) : ((speedAttribute != null) ? speedAttribute.getValue() : 0.1);

        double thresholdMultiplier = s;
        if (this.hasSprint && (this.isOnGround || this.wasOnGround)) baseSpeed /= 1.3;

        double calculatedThreshold = baseSpeed * thresholdMultiplier;
        double finalThreshold = calculatedThreshold + 0.05;

        return distXZ > finalThreshold;
    }

    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        if (player instanceof TemporaryPlayer) return;
        LmvPlayer client = get(player);
        PacketContainer packet = event.getPacket();
        if (client != null) {
            PacketType type = event.getPacketType();
            long now = System.currentTimeMillis();
            long nano = System.nanoTime();

            if (isRotation(type)) {
                float yaw = packet.getFloat().read(0);
                float pitch = packet.getFloat().read(1);
                client.lastYaw = client.yaw;
                client.lastPitch = client.pitch;
                client.yaw = yaw;
                client.pitch = pitch;
            }
            if (type.equals(Client.POSITION) || type.equals(Client.POSITION_LOOK)) {
                double x = packet.getDoubles().read(0);
                double y = packet.getDoubles().read(1);
                double z = packet.getDoubles().read(2);
                client.lastDeltaX = client.deltaX();
                client.lastDeltaY = client.deltaY();
                client.lastDeltaZ = client.deltaZ();
                client.lastDeltaXZ = client.deltaXZ();
                client.lastX = client.x;
                client.lastY = client.y;
                client.lastZ = client.z;
                client.x = x;
                client.y = y;
                client.z = z;

                AttributeInstance sattr = client.player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                int sLvl = client.level(PotionEffectType.SPEED);
                int jLvl = client.level(PotionEffectType.JUMP);
                double sat = (sattr != null ? sattr.getValue() : 0.1F);
                double deltaXZ = client.deltaXZ();
                double lastDeltaXZ = client.lastDeltaXZ;
                double deltaY = client.deltaY();
                double lastDeltaY = client.lastDeltaY;
                boolean strafe = SimulationUtil.isStrafe(sat, deltaXZ, 0.001);
                boolean walking = SimulationUtil.isWalk(sat, deltaXZ, 0.001);
                boolean jumping = SimulationUtil.isJump(jLvl, deltaY, 0.001);
                boolean sneaking = SimulationUtil.isSneak(sat, deltaXZ, 0.001);
                boolean shield = SimulationUtil.isShield(sat, deltaXZ, 0.001);
                boolean swim = SimulationUtil.isSwim(deltaXZ, 0.001);
                boolean swimhop = SimulationUtil.isSwimJump(deltaY, 0.001);
                boolean climb = SimulationUtil.isClimbUp(deltaY,0.001);

                boolean isSlowMovement = SimulationUtil.isSlowMovement(sat, lastDeltaXZ, deltaXZ, 0.001);
                boolean isSlowMovementT1 = SimulationUtil.isSlowMovementT1(sat, lastDeltaXZ, deltaXZ, 0.001);
                boolean isSlowMovementT2 = SimulationUtil.isSlowMovementT2(sat, lastDeltaXZ, deltaXZ, 0.001);

                client.wasLastSimulationSwim = client.lastSimulationSwim;
                client.wasLastSimulationWalk = client.lastSimulationWalk;
                client.wasLastSimulationClimb = client.lastSimulationClimb;
                client.wasLastSimulationShield = client.lastSimulationShield;
                client.wasLastSimulationSneak = client.lastSimulationSneak;

                client.lastSimulationStrafe = client.simulationStrafe;
                client.lastSimulationJump = client.simulationJump;
                client.lastSimulationWalk = client.simulationWalk;
                client.lastSimulationSprint = client.simulationSprint;
                client.lastSimulationSneak = client.simulationSneak;
                client.lastSimulationShield = client.simulationShield;
                client.lastSimulationSwim = client.simulationSwim;
                client.lastSimulationSwimJump = client.simulationSwimJump;
                client.lastSimulationClimb = client.simulationClimb;

                client.simulationStrafe = strafe;
                client.simulationJump = jumping;
                client.simulationWalk = walking;
                client.simulationSprint = false;
                client.simulationSneak = sneaking;
                client.simulationShield = shield;
                client.simulationSwim = swim;
                client.simulationSwimJump = swimhop;
                client.simulationClimb = climb;

                client.wasSlowMovement = client.isSlowMovement;
                client.wasSlowMovementT1 = client.isSlowMovementT1;
                client.wasSlowMovementT2 = client.isSlowMovementT2;
                client.isSlowMovement = isSlowMovement;
                client.isSlowMovementT1 = isSlowMovementT1;
                client.isSlowMovementT2 = isSlowMovementT2;

                if (client.isRecentlyTeleported()) {
                    client.lastPacket = packet;
                }
            }

            if (type.equals(Client.ARM_ANIMATION)) {
                client.lastArmAnimation = now;
                client.isArmAnimation = true;
                client.nanoArmAnimation = nano;
            } else if (type.equals(Client.ABILITIES)) {
                client.lastAbilities = now;
            } else if (type.equals(Client.BLOCK_DIG)) {
                client.lastBlockDig = now;
                client.isDigging = true;
            } else if (type.equals(Client.BLOCK_PLACE)) {
                client.lastBlockPlace = now;
                client.isPlacing = true;
            } else if (type.equals(Client.CLOSE_WINDOW)) {
                client.lastWindowClose = now;
                client.isWindowClosing = true;
            } else if (type.equals(Client.ENTITY_ACTION)) {
                client.lastEntityAction = now;
                client.nanoEntityAction = nano;
                EnumWrappers.PlayerAction action = (EnumWrappers.PlayerAction)event.getPacket().getPlayerActions().read(0);
                switch (action) {
                    case START_SNEAKING:
                        client.hasSneak = true;
                        client.lastSneakStart = now;
                        client.lastSneak = now;
                        break;
                    case STOP_SNEAKING:
                        client.hasSneak = false;
                        client.lastSneakStop = now;
                        client.lastSneak = now;
                        break;
                    case START_SPRINTING:
                        client.hasSprint = true;
                        client.lastStartSprint = now;
                        client.lastStartSprintNano = nano;
                        client.lastSprint = now;
                        break;
                    case STOP_SPRINTING:
                        client.hasSprint = false;
                        client.lastStopSprint = now;
                        client.lastStopSprintNano = nano;
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
                client.swithSlot = true;
            } else if (type.equals(Client.LOOK)) {
                client.lastLook = now;
                client.rotating = true;
                float yaw = packet.getFloat().read(0);
                float pitch = packet.getFloat().read(1);
                client.updateRotation(yaw, pitch);
            } else if (type.equals(Client.PICK_ITEM)) {
                client.lastPickItem = now;
            } else if (type.equals(Client.POSITION_LOOK)) {
                client.wasSneaking = client.isSneaking;
                client.wasSprinting = client.isSprinting;
                client.wasClimbing = client.isClimbing;
                client.wasFluid = client.isFluid;

                client.isSprinting = player.isSprinting();
                client.isSneaking = player.isSneaking();
                client.isClimbing = hasClimbableNearby(player.getLocation());
                client.isFluid = player.isInWater() || player.isInLava();

                Location location = player.getLocation();
                client.lastPositionLook = now;
                client.rotating = true;
                client.isMoving = true;
                float yaw = packet.getFloat().read(0);
                float pitch = packet.getFloat().read(1);
                client.updateRotation(yaw, pitch);
                if (!player.isInsideVehicle() && player.isTicking() && !player.isDead() && player.isOnline()) {
                    client.positions.add(new PositionInfo(event));
                } else {
                    isLastLagging = true;
                }

                if (savePT_Look > 0) {
                    if (now-lastUpdateTarget>=savePT_Look) {
                        try {
                            client.looks.add(new LookInformation(event, player.getLocation()));
                        } catch (Exception e1) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                if (!player.isOnline()) return;
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
                            if (!player.isOnline()) return;
                            client.looks.add(new LookInformation(event, player.getLocation()));
                        });
                    }
                    lastUpdateTarget = now;
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
                        client.wasOnGround = client.isOnGround;
                        if (player.isOnGround()) {
                            client.isOnGround = true;
                            client.groundLocation = location.clone();
                            client.groundLocations.add(client.groundLocation);
                            client.lastGround = now;
                        } else {
                            client.isOnGround = false;
                        }
                    }

                    if (player.isGliding()) {
                        client.lastGliding = now;
                        client.nanoGliding = now;
                        if (!client.isGliding) {
                            client.isGliding = true;
                        } else {
                            client.wasGliding = true;
                        }
                    } else {
                        if (client.isGliding) {
                            client.isGliding = false;
                        } else {
                            client.wasGliding = false;
                        }
                    }

                    if (player.isFlying()) {
                        client.lastFlying = now;
                        client.nanoFlying = now;
                        if (!client.isFlying) {
                            client.isFlying = true;
                        } else {
                            client.wasFlying = true;
                        }
                    } else {
                        if (client.isFlying) {
                            client.isFlying = false;
                        } else {
                            client.wasFlying = false;
                        }
                    }
                }
            } else if (type.equals(Client.POSITION)) {
                client.wasSneaking = client.isSneaking;
                client.wasSprinting = client.isSprinting;
                client.wasClimbing = client.isClimbing;
                client.wasFluid = client.isFluid;

                client.isSprinting = player.isSprinting();
                client.isSneaking = player.isSneaking();
                client.isClimbing = hasClimbableNearby(player.getLocation());
                client.isFluid = player.isInWater() || player.isInLava();

                client.isMoving = true;
                Location location = player.getLocation();
                if (!player.isInsideVehicle() && player.isTicking() && !player.isDead() && player.isOnline()) {
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

                    if (player.isFlying()) {
                        client.lastFlying = now;
                        client.nanoFlying = now;
                        if (!client.isFlying) {
                            client.isFlying = true;
                        } else {
                            client.wasFlying = true;
                        }
                    } else {
                        if (client.isFlying) {
                            client.isFlying = false;
                        } else {
                            client.wasFlying = false;
                        }
                    }

                    if (player.isGliding()) {
                        client.lastGliding = now;
                        client.nanoGliding = now;
                        if (!client.isGliding) {
                            client.isGliding = true;
                        } else {
                            client.wasGliding = true;
                        }
                    } else {
                        if (client.isGliding) {
                            client.isGliding = false;
                        } else {
                            client.wasGliding = false;
                        }
                    }

                    if (distanceXZ > 0.0) {
                        client.currentLocation = location.clone();
                        client.locations.add(client.currentLocation);
                        client.lastMoveTime = now;
                        client.wasOnGround = client.isOnGround;
                        if (player.isOnGround()) {
                            client.groundLocation = location.clone();
                            client.groundLocations.add(client.groundLocation);
                            client.lastGround = now;
                        } else {
                            client.isOnGround = false;
                        }
                    }
                }
            } else if (type.equals(Client.SET_CREATIVE_SLOT)) {
                client.lastSetCreativeSlot = now;
                client.creativeSlot = true;
            } else if (type.equals(Client.TAB_COMPLETE)) {
                client.lastTabComplete = now;
                client.tabCompleting = true;
            } else if (type.equals(Client.TELEPORT_ACCEPT)) {
                client.lastTeleportAccept = now;
                client.teleporting = true;
            } else if (type.equals(Client.USE_ENTITY)) {
                client.lastUseEntity = now;
                client.usingEntity = true;
                try {
                    EnumWrappers.EntityUseAction action = packet.getEntityUseActions().read(0);
                    if (action == EnumWrappers.EntityUseAction.ATTACK) {
                        client.lastAttackPacket = now;
                        client.isAttacking = true;
                        client.nanoAttack = nano;
                    }
                } catch (Exception ignored) {}
            } else if (type.equals(Client.USE_ITEM)) {
                client.lastUseItem = now;
                client.usingItem = true;
            } else if (type.equals(Client.VEHICLE_MOVE)) {
                client.lastVehicleMove = now;
                client.vehicleMoving = true;
            } else if (type.equals(Client.WINDOW_CLICK)) {
                client.lastWindowClick = now;
                client.lastClickPacket = packet;
                client.windowClicking = true;
            } else if (type.equals(Client.ENCHANT_ITEM)) {
                client.lastEnchantItem = now;
                client.enchanting = true;
            } else if (type.equals(Client.BOAT_MOVE)) {
                client.lastBoatMove = now;
                client.boatMoving = true;
            } else if (type.equals(Client.KEEP_ALIVE)) {
                client.resetActions();
                client.lastAlive = now;
            }

            List<Long> packS = packets.computeIfAbsent(type,k -> new CopyOnWriteArrayList<>());

            packS.add(now);
            if (packS.size() > 50) packS.remove(0);

            if (client.looks.size() > 20) client.looks.remove(0);
            if (client.locations.size() > 20) client.locations.remove(0);
            client.lastPacket = packet;

            InventoryListener.onPacketReceiving(event);
            if (isMovement(type)) {
                if (client.isInventoryOpened) client.invTick++;
                else client.invTick = 0;
            }
            CheckManager.onPacketReceive(event);
        }
    }

    public boolean isMovement(PacketType type) {
        return type.equals(Client.POSITION) || type.equals(Client.POSITION_LOOK);
    }

    public boolean isRotation(PacketType type) {
        return type.equals(Client.POSITION_LOOK) || type.equals(Client.LOOK);
    }

    public void resetActions() {
        isAttacking = false;
        isArmAnimation = false;
        isWindowClosing = false;
        isWindowOpen = false;
        isSWindowClosing = false;
        isRespawning = false;
        swithSlot = false;
        rotating = false;
        isDigging = false;
        isPlacing = false;
        creativeSlot = false;
        tabCompleting = false;
        teleporting = false;
        usingEntity = false;
        usingItem = false;
        vehicleMoving = false;
        windowClicking = false;
        enchanting = false;
        boatMoving = false;
        isMoving = false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity entity = event.getEntity();

        if (!(damager instanceof Player)) return;

        Player plr = (Player) damager;
        LmvPlayer client = get(plr);
        if (client == null) return;

        double fromDamage = event.getDamage();
        double toDamage = fromDamage * client.damageMultiple;

        long now = System.currentTimeMillis();

        if (client.cancelAttack) {
            event.setCancelled(true);

            if (client.u2AttackTime <= now) {
                client.cancelAttack = false;
                client.u2AttackTime = 0L;
            }
            return;
        }

        if (client.damageMultiple != 1.0) {
            event.setDamage(toDamage);

            if (client.uAttackTime <= now) {
                client.damageMultiple = 1.0;
                client.uAttackTime = 0L;
            }
            return;
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
        if (lastLocation == null) return def;
        return lastLocation.distance(def) <= max ? lastLocation : def;
    }

    public Location getLastGroundLocation() {
        return this.groundLocation;
    }

    public Location getLastLocation() {
        int size = this.locations.size();

        if (size == 0) {
            return null;
        }

        if (size == 1) {
            return this.locations.get(0);
        }

        return this.locations.get(size - 2);
    }

    public void onPacketSending(PacketEvent event) {
        InventoryListener.onPacketSending(event);
        CheckManager.onPacketSend(event);
    }

    public ListeningWhitelist SENDING_WHITELIST =
            ListeningWhitelist.newBuilder().types(PacketType.Play.Server.getInstance().values().stream().filter(PacketType::isSupported).toArray(PacketType[]::new)).build();
    public ListeningWhitelist getSendingWhitelist() {
        return SENDING_WHITELIST;
    }

    public ListeningWhitelist RECEIVE_WHITELIST =
            ListeningWhitelist.newBuilder().types(Client.getInstance().values().stream().filter(PacketType::isSupported).toArray(PacketType[]::new)).build();
    public ListeningWhitelist getReceivingWhitelist() {
        return RECEIVE_WHITELIST;
    }

    public Plugin getPlugin() {
        return LmvAC.getInstance();
    }

    static {
        CONSOLE_COOLDOWN = TimeUnit.SECONDS.toMillis(15L);
        ADMIN_COOLDOWN = TimeUnit.SECONDS.toMillis(5L);
        PLAYER_JOIN_COOLDOWN = TimeUnit.MINUTES.toMillis(3L);
        lastConsoleMsgTime = 0L;
        lastAdminMsgTime = new ConcurrentHashMap<>();
        lastPlayerJoinTime = new ConcurrentHashMap<>();
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