package dev.lmv.lmvac.api.modules.checks.meta;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import dev.lmv.lmvac.api.implement.api.npcs.NpcManager;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static dev.lmv.lmvac.api.implement.api.npcs.NpcManager.sendPacketSafely;

@SettingCheck(value = "ModerSpoof", cooldown = Cooldown.COOLDOWN)
public class ModerSpoof extends Check implements PacketCheck, Listener {

    public ModerSpoof(Plugin plugin) {
        super(plugin);
        if (this.isEnabled()) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();
        WrappedGameProfile profile = NpcManager.createProfile(joined.getName(), "", "");

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(joined) || !viewer.canSee(joined)) continue;
            removePlayerFromTabList(viewer, profile);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (viewer.isOnline() && joined.isOnline() && viewer.canSee(joined)) {
                        addPlayerToTabList(viewer, profile, joined.getName(), EnumWrappers.NativeGameMode.CREATIVE);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    private static void addPlayerToTabList(Player viewer, WrappedGameProfile profile, String name, EnumWrappers.NativeGameMode mode) {
        if (viewer == null || !viewer.isOnline()) return;

        try {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);

            if (NpcManager.isModernVersion()) {
                packet.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER));
            } else {
                packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            }

            List<PlayerInfoData> dataList = Collections.singletonList(
                    new PlayerInfoData(profile, 0, mode, WrappedChatComponent.fromText(name))
            );

            packet.getPlayerInfoDataLists().write(0, dataList);
            sendPacketSafely(viewer, packet);
        } catch (Exception ignored) {
        }
    }

    private static void removePlayerFromTabList(Player viewer, WrappedGameProfile profile) {
        if (viewer == null || !viewer.isOnline()) return;

        try {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);

            if (NpcManager.isModernVersion()) {
                packet.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER));
            } else {
                packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
            }

            List<PlayerInfoData> dataList = Collections.singletonList(new PlayerInfoData(profile, 0, null, null));

            packet.getPlayerInfoDataLists().write(0, dataList);
            sendPacketSafely(viewer, packet);
        } catch (Exception ignored) {
        }
    }
}