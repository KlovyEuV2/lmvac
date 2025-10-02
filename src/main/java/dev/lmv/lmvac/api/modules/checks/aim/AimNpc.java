package dev.lmv.lmvac.api.modules.checks.aim;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.api.npcs.AimMode;
import dev.lmv.lmvac.api.implement.api.npcs.NPCNameManager;
import dev.lmv.lmvac.api.implement.api.npcs.NpcManager;
import dev.lmv.lmvac.api.implement.api.npcs.RandomMode;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import dev.lmv.lmvac.api.modules.checks.aim.utils.AimUtil;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Random;

// Aim-чек для вызова NPC
@SettingCheck(value = "AimNpc", cooldown = Cooldown.COOLDOWN)
public class AimNpc extends Check implements PacketCheck {
    private static final Random random = new Random();
    public static RandomMode mode = null;
    private static AimMode aimMode = null;
    private static AimSettings aimSettings = null;
    public AimNpc(Plugin plugin) {
        super(plugin);
        this.remoteEventEnabled = true;
        NpcManager.addRemote(this);
        resetMode(plugin);
    }
    public static class AimSettings {
        public int look = -1;
        public int smooth = -1;
        public int def = -1;
        public AimSettings(Integer look, Integer smooth, Integer def) {
            this.look = look;
            this.smooth = smooth;
            this.def = def;
        }
    }
    public static RandomMode loadRandomMode(Plugin plugin) {
        String modeString = plugin.getConfig().getString("npc.random.mode", "TAB").toUpperCase();

        try {
            return RandomMode.valueOf(modeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid random mode in config: " + modeString + ". Using default: TAB");
            return RandomMode.TAB;
        }
    }
    public static void resetMode(Plugin plugin) {
        mode = loadRandomMode(plugin);
        AimMode aimMode = AimMode.NEW;
        try {
            String modeString = plugin.getConfig().getString("npc.aim-mode", "new").toUpperCase();
            aimMode = AimMode.valueOf(modeString);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid aim mode in config. Defaulting to 'NEW'.");
        }
        AimNpc.aimMode = aimMode;

        ConfigurationSection pattern = plugin.getConfig().getConfigurationSection("npc.pattern");
        if (pattern == null) {
            aimSettings = new AimSettings(7,3,5);
            return;
        }
        int look = pattern.getInt("look",7);int smooth = pattern.getInt("smooth",3);int def = pattern.getInt("default",5);
        aimSettings = new AimSettings(look,smooth,def);
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        Player player = packetEvent.getPlayer();
        LmvPlayer targetPlayer = LmvPlayer.get(player);
        if (targetPlayer == null) return;

        List<LmvPlayer.LookInformation> looks = targetPlayer.looks;
        if (looks.size() >= 2) {
            if (aimMode.equals(AimMode.MIDDLE)) {
                handleMiddleAim(looks,packetEvent,player);
            } else if (aimMode.equals(AimMode.OLD)) {
                handleOldAim(looks,packetEvent,player);
            } else if (aimMode.equals(AimMode.NEW)) {
                handleNewAim(looks,packetEvent,player);
            } else {
                handleNewAim(looks,packetEvent,player);
            }
        }
    }

    private void handleMiddleAim(List<LmvPlayer.LookInformation> looks, PacketEvent packetEvent, Player player) {
        int lockedCount = AimUtil.getLockCount(looks);
        int smoothCount = AimUtil.getSmoothAim(looks, 1.5F, 15.0F, 1.5F, 15.0F);
        int moveAim = AimUtil.getConsistentMovementAim(looks, packetEvent, 1.5F, 15.0F, 1.5F, 15.0F);
        boolean smoothLook = smoothCount > aimSettings.smooth && moveAim > aimSettings.def;
        boolean lockedLook = lockedCount > aimSettings.look;
        if (smoothLook && lockedLook) {
            String name = NpcManager.nameManager.getRandomName(player, mode);
            NpcManager.spawnNpcFor(player, name, NpcManager.createProfile(name, "", ""));
        }
    }

    private void handleNewAim(List<LmvPlayer.LookInformation> looks, PacketEvent packetEvent, Player player) {
        int lockedCount = AimUtil.getLockCount(looks);
        int smoothCount = AimUtil.getSmoothAim(looks, 1.5F, 15.0F, 1.5F, 15.0F);
        boolean smoothLook = smoothCount > aimSettings.smooth;
        boolean lockedLook = lockedCount > aimSettings.look;
        if (smoothLook && lockedLook) {
            String name = NpcManager.nameManager.getRandomName(player, mode);
            NpcManager.spawnNpcFor(player, name, NpcManager.createProfile(name, "", ""));
        }
    }

    private void handleOldAim(List<LmvPlayer.LookInformation> looks, PacketEvent packetEvent, Player player) {
        int lockedCount = AimUtil.getLockCount(looks);
        if (lockedCount >= aimSettings.look) {
            String name = NpcManager.nameManager.getRandomName(player, mode);
            NpcManager.spawnNpcFor(player, name, NpcManager.createProfile(name, "", ""));
        }
    }

    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().types(new PacketType[]{PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK}).build();
    }
}