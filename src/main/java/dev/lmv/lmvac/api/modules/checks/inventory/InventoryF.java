package dev.lmv.lmvac.api.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.api.settings.LocaleManager;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.BukkitCheck;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import dev.lmv.lmvac.api.implement.utils.TimeUtil;
import dev.lmv.lmvac.api.implement.utils.simulation.MovementUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

// Движения в инвентаре
@SettingCheck(value = "InventoryF", cooldown = Cooldown.COOLDOWN)
public class InventoryF extends Check implements BukkitCheck, PacketCheck {
    public static int IMoves = 5;
    public InventoryF(Plugin plugin) {
        super(plugin);
    }

    public static void reloadCfg(Plugin plugin) {
        IMoves = plugin.getConfig().getInt("checks.inventory.f.cancel-moves",5);
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
                                .replaceAll("%2",String.valueOf(targetPlayer.inventoryMoves));
                        this.aFlag(player, targetPlayer, event, pReason);
                    }
                }

        }
    }

    public void aFlag(Player player, LmvPlayer client, PacketEvent event, String reason) {
        if (client.inventoryMoves >= IMoves) {
            event.setCancelled(true);
            flag(player,reason);
        }
        client.inventoryMoves++;
    }

    public void aFlag(Player player, LmvPlayer client, PacketEvent event) {
        if (client.inventoryMoves >= IMoves) {
            event.setCancelled(true);
            flag(player);
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