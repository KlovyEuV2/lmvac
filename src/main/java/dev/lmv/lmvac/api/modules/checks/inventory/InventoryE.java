package dev.lmv.lmvac.api.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.BukkitCheck;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

// Движение головы и других пакетов в инвентаре
@SettingCheck(value = "InventoryE", cooldown = Cooldown.COOLDOWN)
public class InventoryE extends Check implements BukkitCheck, PacketCheck {

    public static int IMoves = 5;

    public InventoryE(Plugin plugin) {
        super(plugin);
        reloadCfg(plugin);
    }

    public static void reloadCfg(Plugin plugin) {
        IMoves = plugin.getConfig().getInt("checks.inventory.e.cancel-moves", 5);
    }

    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PacketType packetType = event.getPacketType();
        int id = event.getPlayer().getEntityId();
        LmvPlayer client = (LmvPlayer) LmvPlayer.players.get(id);

        if (client == null) {
            return;
        }

        String reason = locales.getOrDefault("1","Suspiciously sending packet (%0) while the inventory might be open.");
        String pReason = reason.replaceAll("%0",packetType.name().toString().toUpperCase());

        if (packetType.equals(PacketType.Play.Client.POSITION_LOOK) ||
                packetType.equals(PacketType.Play.Client.POSITION)) {

            handleMovementPacket(event, player, client, packetType, pReason);

        } else if (packetType.equals(PacketType.Play.Client.WINDOW_CLICK)) {

            handleWindowClickPacket(event, client, pReason);

        } else {

            if (client.isInventoryOpened) {
                aFlag(player, client, event, pReason);
            }
        }
    }

    private void handleMovementPacket(PacketEvent event, Player player, LmvPlayer client,
                                      PacketType packetType, String reason) {
        double x, y, z;

        try {
            x = event.getPacket().getDoubles().read(0);
            y = event.getPacket().getDoubles().read(1);
            z = event.getPacket().getDoubles().read(2);
        } catch (Exception e) {
            return;
        }

        double deltaX = Math.abs(x - player.getLocation().getX());
        double deltaY = Math.abs(y - player.getLocation().getY());
        double deltaZ = Math.abs(z - player.getLocation().getZ());

        if (deltaX < 0.001 && deltaY < 0.001 && deltaZ < 0.001) {
            return;
        }

        if (client.isInventoryOpened) {
            aFlag(player, client, event, reason);
        }
    }

    private void handleWindowClickPacket(PacketEvent event, LmvPlayer client, String reason) {
        Integer currentCount = client.inventoryMovesP.get(event.getPacketType());
        int from = (currentCount != null) ? currentCount : 0;

        if (from >= IMoves) {
            event.setCancelled(true);
        }
    }

    public void aFlag(Player player, LmvPlayer client, PacketEvent event, String reason) {
        Integer currentCount = client.inventoryMovesP.get(event.getPacketType());
        int from = (currentCount != null) ? currentCount : 0;

        if (from >= IMoves) {
            event.setCancelled(true);
            flag(player, reason);
        }

        from++;
        client.inventoryMovesP.put(event.getPacketType(), from);
    }

    public void aFlag(Player player, LmvPlayer client, PacketEvent event) {
        Integer currentCount = client.inventoryMovesP.get(event.getPacketType());
        int from = (currentCount != null) ? currentCount : 0;

        if (from >= IMoves) {
            event.setCancelled(true);
            flag(player);
        }

        from++;
        client.inventoryMovesP.put(event.getPacketType(), from);
    }

    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder()
                .types(new PacketType[]{
                        PacketType.Play.Client.POSITION_LOOK,
                        PacketType.Play.Client.LOOK,
                        PacketType.Play.Client.ENTITY_ACTION,
                        PacketType.Play.Client.USE_ENTITY,
                        PacketType.Play.Client.BLOCK_DIG,
                        PacketType.Play.Client.BLOCK_PLACE,
                        PacketType.Play.Client.HELD_ITEM_SLOT,
                        PacketType.Play.Client.USE_ITEM,
                        PacketType.Play.Client.WINDOW_CLICK
                })
                .build();
    }
}