package dev.lmv.lmvac.api.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.api.packetListeners.InventoryListener;
import dev.lmv.lmvac.api.implement.api.settings.LocaleManager;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.DescType;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.BukkitCheck;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.Configurable;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import dev.lmv.lmvac.api.implement.utils.TimeUtil;
import dev.lmv.lmvac.api.implement.utils.inventory.InventoryUtil;
import dev.lmv.lmvac.api.implement.utils.simulation.MovementUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.StringUtil;

// Движения в инвентаре
@SettingCheck(value = "InventoryF", cooldown = Cooldown.COOLDOWN, descType = DescType.ALPHA, description = "InventoryMove' Detection")
public class InventoryF extends Check implements BukkitCheck, PacketCheck, Configurable {
    public int IMoves = 5;
    public boolean stop = true;
    public InventoryF(Plugin plugin) {
        super(plugin);
        reloadConfiguration(plugin);
    }

    public void reloadConfiguration(Plugin plugin) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("checks.inventory.f");
        try {
            IMoves = section.getInt("cancel-moves",5);
            stop = section.getBoolean("stop-sprint",true);
        } catch (NullPointerException ex) {
            IMoves = 5;
            stop = true;
        }
    }

    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PacketType packetType = event.getPacketType();
        int id = event.getPlayer().getEntityId();
        LmvPlayer targetPlayer = (LmvPlayer)LmvPlayer.players.get(id);
        if (targetPlayer != null) {
            long now = System.currentTimeMillis();
            if (!packetType.equals(PacketType.Play.Client.POSITION) && !packetType.equals(PacketType.Play.Client.POSITION_LOOK) && !packetType.equals(PacketType.Play.Client.LOOK)) {
                if (packetType.equals(PacketType.Play.Client.WINDOW_CLICK) && targetPlayer.inventoryMoves >= IMoves && MovementUtil.checkMove(player)) {
                    event.setCancelled(true);
                    InventoryUtil.updateSlot(targetPlayer,event);
                }
            } else {
                if (player.isInsideVehicle() || player.isGliding() || player.isInWater()) {
                    return;
                }

                double x, y, z;

                try {
                    if (packetType.equals(PacketType.Play.Client.POSITION) || packetType.equals(PacketType.Play.Client.POSITION_LOOK)) {
                        x = event.getPacket().getDoubles().read(0);
                        y = event.getPacket().getDoubles().read(1);
                        z = event.getPacket().getDoubles().read(2);
                    } else {
                        return;
                    }
                } catch (Exception e) {
                    return;
                }

                double deltaX = Math.abs(x - player.getLocation().getX());
                double deltaY = Math.abs(y - player.getLocation().getY());
                double deltaZ = Math.abs(z - player.getLocation().getZ());

                if (deltaX < 0.001 && deltaY < 0.001 && deltaZ < 0.001) {
                    return;
                }

                String unknown = LocaleManager.current_.getString("unknown","unknown");
                String lastClose = targetPlayer.lastWindowClose != 0L ? String.valueOf(TimeUtil.formatDuration(now - targetPlayer.lastWindowClose)) : unknown;
                String lastClick = targetPlayer.lastWindowClick != 0L ? String.valueOf(TimeUtil.formatDuration(now - targetPlayer.lastWindowClick)) : unknown;

                if (targetPlayer.isInventoryOpened && MovementUtil.checkMove(player)) {
                    String reason = locales.getOrDefault("1","Suspend moving maybe in inventory. Close[%0] : Click[%1] with IMoves[%2].");
                    String pReason = reason
                            .replaceAll("%0",lastClose)
                            .replaceAll("%1",lastClick)
                            .replaceAll("%2",String.valueOf(targetPlayer.inventoryMoves)
                                    .replaceAll("%m",(targetPlayer.inventoryMoves>=IMoves)?"moving ":""));
                    this.aFlag(player, targetPlayer, event, pReason);
                }
            }

        }
    }

    public void aFlag(Player player, LmvPlayer client, PacketEvent event, String reason) {
        if (client.inventoryMoves >= IMoves) {
            event.setCancelled(true);
            flag(player,reason);
        } else if (client.inventoryMoves == Math.min(0,client.inventoryMoves-1)) {
            player.setSprinting(false);
        }
        client.inventoryMoves++;
    }

    public void aFlag(Player player, LmvPlayer client, PacketEvent event) {
        if (client.inventoryMoves >= IMoves) {
            event.setCancelled(true);
            flag(player);
        } else if (client.inventoryMoves == Math.min(0,client.inventoryMoves-1)) {
            player.setSprinting(false);
        }
        client.inventoryMoves++;
    }

    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder()
                .types(new PacketType[]{
                        PacketType.Play.Client.POSITION,
                        PacketType.Play.Client.POSITION_LOOK,
                        PacketType.Play.Client.LOOK,
                        PacketType.Play.Client.WINDOW_CLICK
                })
                .build();
    }
}