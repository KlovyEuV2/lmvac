package dev.lmv.lmvac.api.modules.checks.badpackets.other;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.BukkitCheck;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import dev.lmv.lmvac.api.implement.utils.FluidUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

@SettingCheck(
        value = "BadPacketsB",
        cooldown = Cooldown.COOLDOWN
)
public class BadPacketsB extends Check implements PacketCheck, BukkitCheck {
    public BadPacketsB(Plugin plugin) {
        super(plugin);
    }

    public static List<ShulkerProcess> processingShulkers = new ArrayList<>();
    public static long lastTimeUpdate = -1;

    public static class ShulkerProcess {
        public Player player;
        public Block block;
        public Location position;
        public Long time;

        public ShulkerProcess(Player player, Block block, Location position) {
            this.player = player;
            this.block = block;
            this.position = position;
            this.time = System.currentTimeMillis();
        }
    }

    public static void cleanUpOlder(long maxOld) {
        long now = System.currentTimeMillis();
        processingShulkers.removeIf(k -> now - k.time > maxOld);
    }

    public static boolean isShulkerNearby(Location location, double distance) {
        processingShulkers.removeIf(sp -> !isShulker(sp.block.getType()));

        for (ShulkerProcess sp : processingShulkers) {
            double diffX = Math.abs(sp.position.getX() - location.getX());
            double diffY = Math.abs(sp.position.getY() - location.getY());
            double diffZ = Math.abs(sp.position.getZ() - location.getZ());

            if (diffX <= distance && diffY <= distance && diffZ <= distance) {
                return true;
            }
        }
        return false;
    }

    public void onPacketSending(PacketEvent packetEvent) {
        long now = System.currentTimeMillis();
        if (lastTimeUpdate <= 0 || now - lastTimeUpdate > 50) {
            cleanUpOlder(2000);
            lastTimeUpdate = now;
        }
    }

    public void onPacketReceiving(PacketEvent packetEvent) {
        Player player = packetEvent.getPlayer();
        PacketType packetType = packetEvent.getPacketType();
        PacketContainer packet = packetEvent.getPacket();

        int id = player.getEntityId();
        LmvPlayer client = LmvPlayer.players.get(id);
        if (client == null) return;

        if (FluidUtil.isInFluid(player) || player.isInsideVehicle() || player.isFlying() || player.isGliding()
                || !player.isTicking() || player.isDead()) return;

        if (packetType.equals(PacketType.Play.Client.POSITION)) {
            Location playerLoc = player.getLocation();

            if (client.groundLocation == null) client.groundLocation = playerLoc.clone();

            boolean nearShulker = isShulkerNearby(client.groundLocation, 1.5);

            double diffY = playerLoc.getY() - client.groundLocation.getY();

            double maxY = 1.7;

            if (player.hasPotionEffect(PotionEffectType.JUMP)) {
                PotionEffect effect = player.getPotionEffect(PotionEffectType.JUMP);
                int amplifier = effect.getAmplifier() + 1;
                maxY += amplifier * 0.5;
            }

            if (diffY > maxY && nearShulker) {
                packetEvent.setCancelled(true);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.teleport(client.groundLocation);
                });
                flag(player, "SHULKER. UP-EXPLOIT. Y: " + String.format("%.2f", diffY) + " Max: " + String.format("%.2f", maxY));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onShulkerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !isShulker(block.getType())) return;

        Location loc = block.getLocation();
        processingShulkers.add(new ShulkerProcess(player,block,loc));
    }

    public static boolean isShulker(Material material) {
        return material == Material.SHULKER_BOX || material.name().endsWith("SHULKER_BOX");
    }

    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder()
                .types(PacketType.Play.Client.POSITION)
                .build();
    }

    public ListeningWhitelist getSendingWhitelist() {
        return ListeningWhitelist.newBuilder()
                .types(PacketType.Play.Server.UPDATE_TIME)
                .build();
    }
}
