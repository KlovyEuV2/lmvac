package dev.lmv.lmvac.api.modules.checks.aim.snaps;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Client;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

// Обычный чек на снапы на элитре
@SettingCheck(
        value = "SnapElytra",
        cooldown = Cooldown.COOLDOWN
)
public class SnapElytra extends Check implements PacketCheck, Listener {
    private static final ConcurrentHashMap<Player, List<ElytraAimInformation>> playerElytraAimData = new ConcurrentHashMap<>();
    private static final double MIN_ELYTRA_SPEED = 0.5;
    private static final float SNAP_THRESHOLD = 45.0F;
    private static final float CORRECTION_THRESHOLD = 15.0F;

    public SnapElytra(Plugin plugin) {
        super(plugin);
    }

    public void onPacketReceiving(PacketEvent packetEvent) {
        Player player = packetEvent.getPlayer();
        PacketType packetType = packetEvent.getPacketType();
        LmvPlayer targetPlayer = LmvPlayer.get(player);

        if (targetPlayer == null) return;

        if (!player.isGliding()) return;

        if (packetType == Client.LOOK || packetType == Client.POSITION_LOOK) {
            List<LmvPlayer.LookInformation> looks = targetPlayer.looks;
            if (looks.size() >= 2) {
                LmvPlayer.LookInformation lastLook = looks.get(looks.size() - 2);
                LmvPlayer.LookInformation currentLook = looks.get(looks.size() - 1);

                if (lastLook == null || currentLook == null) return;
                if (lastLook.location == null || currentLook.location == null) return;

                Location lastLocation = lastLook.location.clone();
                Location currentLocation = currentLook.location.clone();

                double distance = lastLocation.distance(currentLocation);
                if (distance < MIN_ELYTRA_SPEED) return;

                float yawDiff = Math.abs(lastLocation.getYaw() - currentLocation.getYaw());
                if (yawDiff > 180.0F) {
                    yawDiff = 360.0F - yawDiff;
                }

                float pitchDiff = Math.abs(lastLocation.getPitch() - currentLocation.getPitch());

                boolean targetChanged = false;
                Entity elytraTarget = null;

                if (lastLook.target != null && currentLook.target != null) {
                    targetChanged = !lastLook.target.equals(currentLook.target);
                    elytraTarget = currentLook.target;
                } else if (lastLook.target != currentLook.target) {
                    targetChanged = true;
                    elytraTarget = currentLook.target;
                }

                boolean isElytraSnap = (targetChanged && yawDiff > SNAP_THRESHOLD) ||
                        (elytraTarget != null && (yawDiff > SNAP_THRESHOLD || pitchDiff > SNAP_THRESHOLD));

                ElytraAimInformation thisAim = new ElytraAimInformation(
                        packetEvent, lastLook, currentLook, isElytraSnap, yawDiff, pitchDiff, distance, elytraTarget
                );

                List<ElytraAimInformation> lastAims = playerElytraAimData.computeIfAbsent(player, k -> new ArrayList<>());

                if (lastAims.size() > 1) {
                    ElytraAimInformation lastAim = lastAims.get(lastAims.size() - 1);

                    if (lastAim.isElytraSnap && !thisAim.isElytraSnap &&
                            yawDiff < CORRECTION_THRESHOLD && thisAim.elytraTarget != null) {
                        this.flag(player);
                    }

                    if (lastAim.isElytraSnap && thisAim.isElytraSnap &&
                            Math.abs(lastAim.yawDiff - thisAim.yawDiff) < 10.0F &&
                            lastAim.elytraTarget != null && thisAim.elytraTarget != null) {
                        this.flag(player);
                    }

                    if (lastAims.size() > 2) {
                        ElytraAimInformation thirdLastAim = lastAims.get(lastAims.size() - 2);

                        if (thirdLastAim.isElytraSnap && !lastAim.isElytraSnap && thisAim.isElytraSnap &&
                                Math.abs(thirdLastAim.yawDiff - thisAim.yawDiff) < 15.0F &&
                                thisAim.elytraTarget != null) {
                            this.flag(player);
                        }
                    }

                    if (thisAim.distance > 2.0 && yawDiff > 45.0F && pitchDiff > 30.0F &&
                            thisAim.elytraTarget != null) {
                        long snapCount = lastAims.stream()
                                .skip(Math.max(0, lastAims.size() - 5))
                                .mapToLong(aim -> (aim.isElytraSnap && aim.distance > 1.5) ? 1 : 0)
                                .sum();

                        if (snapCount >= 3) {
                            this.flag(player);
                        }
                    }
                }

                lastAims.add(thisAim);

                if (lastAims.size() > 15) {
                    lastAims.remove(0);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerElytraAimData.remove(event.getPlayer());
    }

    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder()
                .types(new PacketType[]{Client.LOOK, Client.POSITION_LOOK})
                .build();
    }

    public static class ElytraAimInformation {
        public PacketEvent packetEvent;
        public LmvPlayer.LookInformation lastLook;
        public LmvPlayer.LookInformation thisLook;
        public boolean isElytraSnap;
        public float yawDiff;
        public float pitchDiff;
        public double distance;
        public Entity elytraTarget;
        public long time;

        public ElytraAimInformation(PacketEvent packetEvent,
                                    LmvPlayer.LookInformation lastLook,
                                    LmvPlayer.LookInformation thisLook,
                                    boolean isElytraSnap,
                                    float yawDiff,
                                    float pitchDiff,
                                    double distance,
                                    Entity elytraTarget) {
            this.packetEvent = packetEvent;
            this.lastLook = lastLook;
            this.thisLook = thisLook;
            this.isElytraSnap = isElytraSnap;
            this.yawDiff = yawDiff;
            this.pitchDiff = pitchDiff;
            this.distance = distance;
            this.elytraTarget = elytraTarget;
            this.time = System.currentTimeMillis();
        }
    }
}