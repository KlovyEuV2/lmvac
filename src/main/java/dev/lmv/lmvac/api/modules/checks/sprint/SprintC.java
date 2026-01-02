package dev.lmv.lmvac.api.modules.checks.sprint;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.lmv.lmvac.LmvAC;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.other.DoubleBuffer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.DescType;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.Configurable;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import dev.lmv.lmvac.api.implement.utils.FluidUtil;
import dev.lmv.lmvac.api.implement.utils.IceUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// Вообще это был чек с Premium' версии, и да. Я его сливаю. (Но старый)

// Детект сброса спринта
@SettingCheck(
        value = "SprintC",
        cooldown = Cooldown.COOLDOWN,
        descType = DescType.NEW
)
public class SprintC extends Check implements PacketCheck, Configurable {

    public SprintMode mode;
    public DoubleBuffer buffer;

    public ConcurrentHashMap<UUID, Long> lastcheck = new ConcurrentHashMap<>();

    public enum SprintMode {
        TICK
    }

    public CopyOnWriteArrayList<UUID> checks = new CopyOnWriteArrayList<>();


    public void reloadConfiguration(Plugin plugin) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection bufferSection = config.getConfigurationSection("buffer.sprintc");

        boolean enabled;
        double threshold;
        double increment;
        double decrement;
        try {
            enabled = bufferSection.getBoolean("enabled", false);
            threshold = bufferSection.getDouble("threshold", 1.0);
            increment = bufferSection.getDouble("increment", 1.0);
            decrement = bufferSection.getDouble("decrement", 0.25);
        } catch (NullPointerException ex) {
            enabled = false;
            threshold = 1.0;
            increment = 1.0;
            decrement = 0.25;
        }

        buffer = new DoubleBuffer(enabled, threshold, increment, decrement);
        try {
            mode = SprintMode.valueOf(config.getString("checks.sprintc.mode","TICK").toUpperCase());
        } catch (Exception ex) {
            mode = SprintMode.TICK;
        }
    }

    public SprintC(Plugin plugin) {
        super(plugin);
        reloadConfiguration(plugin);
    }

    public void onPacketReceiving(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        Player player = event.getPlayer();
        PacketType packetType = event.getPacketType();

        int id = event.getPlayer().getEntityId();
        LmvPlayer client = LmvPlayer.players.get(id);
        UUID uuid = player.getUniqueId();

        if (client == null) return;
        if (packetType == PacketType.Play.Client.USE_ENTITY) {
            try {
                EnumWrappers.EntityUseAction action = packet.getEntityUseActions().read(0);
                if (action == EnumWrappers.EntityUseAction.ATTACK) {
                    if (mode.equals(SprintMode.TICK)) {
                        if (isCritReady(player)) { // важно, ото будет ложные без критов
                            this.handleHardSimulation(event);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().types(new PacketType[]{PacketType.Play.Client.USE_ENTITY}).build();
    }

    public boolean isMovement(PacketType packetType) {
        return packetType.equals(PacketType.Play.Client.POSITION) || packetType.equals(PacketType.Play.Client.POSITION_LOOK);
    }

    private void handleHardSimulation(PacketEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        LmvPlayer client = LmvPlayer.get(player);
        if (client == null) return;
        if (IceUtil.isOnIce(player) || FluidUtil.isInFluid(player) || client.player.isSprinting() || player.isInsideVehicle()
                || player.isFlying() || player.isGliding()) {
            if (buffer.enabled) buffer.reduceBuffer(player);
            return;
        }

        Long now = System.nanoTime();
        lastcheck.put(uuid,now);
        Bukkit.getScheduler().runTaskLater(LmvAC.getInstance(), () -> {
            // тут можно сказать проверка когда чел на стадий сброса спринта (задержка в 1 тик, тк игрок делает 10 ивентов в сек
            // 1 сек = 20 тиков, получ 1 тик обработка, и мы в этот тик считайте чекаем сброс
            // если чел подозрительно привышает скорость обычной ходьбы - детект.
            // кароче 1 словом - детект SrintSpoof'еров
            if (lastcheck.get(uuid) == null || !Objects.equals(lastcheck.get(uuid), now)) return;
            if (!player.isOnline() || event.isCancelled() || player.isJumping()) return;
            if (client.player.isSprinting() && client.isSpeedSimulation()) { // слишком не легитное начало спринта
                if (buffer.enabled) {
                    buffer.addViolation(player);
                    double cBuffer = buffer.getBuffer(player);
                    if (cBuffer >= buffer.threshold) {
                        String reason = locales.getOrDefault("1","suspicious simulation delay from packets during combat (sprinting-reset). buffer %0");
                        String pReason = reason
                                .replace("%0",String.format("%.2f", cBuffer));
                        flag(player, pReason);
                    }
                } else {
                    String reason = locales.getOrDefault("1","suspicious simulation delay from packets during combat (sprinting-reset). buffer %0");
                    String pReason = reason
                            .replace("%0",String.format("%.2f", 0.0));
                    flag(player, pReason);
                }
            } else if (buffer.enabled) {
                buffer.reduceBuffer(player);
            }
        }, 1L);
    }

    private static boolean isCritReady(Player player) {
        return player.getAttackCooldown() >= 0.9F;
    }
}