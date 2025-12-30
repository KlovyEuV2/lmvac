package dev.lmv.lmvac.api.modules.checks.wallhit;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.other.DoubleBuffer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.DescType;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.Configurable;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SettingCheck(
        value = "WallHitA",
        cooldown = Cooldown.COOLDOWN,
        descType = DescType.NEW
)
public class WallHitA extends Check implements PacketCheck, Configurable {

    private double hitboxExpandX = 0.25;
    private double hitboxExpandY = 0.35;
    private double hitboxExpandZ = 0.25;

    public DoubleBuffer buffer;

    public void reloadConfiguration(Plugin plugin) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection bufferSection = config.getConfigurationSection("buffer.wallhita");

        boolean enabled;
        double threshold;
        double increment;
        double decrement;
        try {
            enabled = bufferSection.getBoolean("enabled", true);
            threshold = bufferSection.getDouble("threshold", 3.0);
            increment = bufferSection.getDouble("increment", 1.0);
            decrement = bufferSection.getDouble("decrement", 0.25);
        } catch (NullPointerException ex) {
            enabled = true;
            threshold = 3.0;
            increment = 1.0;
            decrement = 0.25;
        }

        buffer = new DoubleBuffer(enabled, threshold, increment, decrement);
    }

    public WallHitA(Plugin plugin) {
        super(plugin);
        reloadConfiguration(plugin);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        LmvPlayer client = LmvPlayer.get(player);

        if (client == null) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.USE_ENTITY) {
            try {
                if (event.getPacket().getEntityUseActions().read(0) == EnumWrappers.EntityUseAction.ATTACK) {
                    int targetEntityId = event.getPacket().getIntegers().read(0);

                    Entity targetEntity = event.getPacket().getEntityModifier(event.getPlayer().getWorld()).read(0);

                    if (!(targetEntity instanceof Player) || targetEntity.equals(player)) {
                        return;
                    }
                    Player target = (Player) targetEntity;

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        UUID playerId = player.getUniqueId();

                        if (wallHit(player, target)) {
                            if (buffer.enabled) {
                                buffer.addViolation(player);
                                if (buffer.getBuffer(player) >= buffer.threshold) {
                                    flag(player, "Through-wall hit, buffer: " + String.format("%.2f", buffer.getBuffer(player)));
                                }
                            } else {
                                flag(player, "Through-wall hit");
                            }
                        } else if (buffer.enabled) {
                            buffer.reduceBuffer(player);
                        }
                    });
                }
            } catch (Exception ignored) {
            }
        }
    }

    private boolean wallHit(Player from, Player target) {
        Location origin = from.getEyeLocation();

        double widthX = target.getWidth() / 2.0 + this.hitboxExpandX;
        double widthZ = target.getWidth() / 2.0 + this.hitboxExpandZ;
        double minX = target.getLocation().getX() - widthX;
        double maxX = target.getLocation().getX() + widthX;
        double minY = target.getLocation().getY();
        double maxY = target.getLocation().getY() + target.getHeight() + this.hitboxExpandY;
        double minZ = target.getLocation().getZ() - widthZ;
        double maxZ = target.getLocation().getZ() + widthZ;

        List<Vector> points = new ArrayList<>();
        for (int i = 0; i <= 2; i++) {
            double x = minX + (double) i * (maxX - minX) / 2.0;
            for (int j = 0; j <= 2; j++) {
                double y = minY + (double) j * (maxY - minY) / 2.0;
                for (int k = 0; k <= 2; k++) {
                    double z = minZ + (double) k * (maxZ - minZ) / 2.0;
                    points.add(new Vector(x, y, z));
                }
            }
        }

        for (Vector point : points) {
            Vector direction = point.clone().subtract(origin.toVector());
            RayTraceResult result = from.getWorld().rayTraceBlocks(
                    origin,
                    direction.normalize(),
                    direction.length(),
                    FluidCollisionMode.NEVER,
                    true
            );

            if (result == null) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder()
                .types(PacketType.Play.Client.USE_ENTITY)
                .build();
    }
}