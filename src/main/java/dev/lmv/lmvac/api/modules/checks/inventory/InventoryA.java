package dev.lmv.lmvac.api.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.DescType;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SettingCheck(
        value = "InventoryA",
        cooldown = Cooldown.NO_COOLDOWN,
        descType = DescType.BETA,
        description = "Pattern fast' clicking detection"
)
public class InventoryA extends Check implements PacketCheck {
    public static long maxDiff = 1;
    public static int maxMatches = 5;
    public static ConcurrentHashMap<UUID, List<Long>> detects = new ConcurrentHashMap<>();

    public InventoryA(Plugin plugin) {
        super(plugin);
    }

    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        PacketType packetType = event.getPacketType();
        LmvPlayer client = LmvPlayer.get(player);
        if (client == null || player.getPing() <= maxDiff) return;

        long now = System.currentTimeMillis();

        if (packetType == PacketType.Play.Client.WINDOW_CLICK) {
            if (now - client.lastWindowClick <= maxDiff) {
                List<Long> detectives = detects.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());

                if (!detectives.isEmpty() && now - detectives.get(detectives.size() - 1) > maxDiff) {
                    detectives.clear();
                }

                detectives.add(now);

                if (detectives.size() >= maxMatches) {
                    event.setCancelled(true);
                    flag(player);
                }

                if (detectives.size() > maxMatches*10) {
                    detectives.remove(0);
                }
            }
        }
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder()
                .types(PacketType.Play.Client.WINDOW_CLICK)
                .build();
    }
}