package dev.lmv.lmvac.api.modules.checks.flight;

import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.DescType;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.BukkitCheck;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

// fly-speed / DragonFlight / FlightStrafe
@SettingCheck(
        value = "FlightC",
        cooldown = Cooldown.COOLDOWN,
        descType = DescType.BETA
)
public class FlightC extends Check implements BukkitCheck {
    public static ConcurrentHashMap<UUID,Long> lastFlight = new ConcurrentHashMap<>();

    public FlightC(Plugin plugin) {
        super(plugin);
    }

    @EventHandler
    private void check(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        LmvPlayer client = LmvPlayer.get(player);
        if (client == null || client.hasBypass(getName()) || !player.isFlying() || player.isInsideVehicle()) return;

        if (client.isFlying && client.wasFlying) {
            this.handleEssentials(event, client);
        }
    }

    private void handleEssentials(PlayerMoveEvent event, LmvPlayer client) {
        Player player = event.getPlayer();

        Location from = event.getFrom();
        Location to = event.getTo();

        double dx = from.getX() - to.getX();
        double dy = from.getY() - to.getY();
        double dz = from.getZ() - to.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance > this.getMaxSpeed(event)) {
            String reason = locales.getOrDefault("1", "Suspicious movement while flying. SPerEvent[%0].");
            String pReason = reason.replace("%0", String.format("%.2f", distance));
            event.setCancelled(true);
            this.flag(player, pReason);
        }
    }

    private double getMaxSpeed(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        AttributeInstance flyingAttr = player.getAttribute(Attribute.GENERIC_FLYING_SPEED);
        double attributeSpeed = flyingAttr != null ? flyingAttr.getValue() : 0.0;
        double flySpeed = (double)player.getFlySpeed();
        double base = 1.191;
        if (flyingAttr != null || player.getGameMode().equals(GameMode.SPECTATOR)) {
            return Integer.MAX_VALUE;
        }
        return flySpeed >= 0.1 ? base * flySpeed * 10.0 : base;
    }
}
