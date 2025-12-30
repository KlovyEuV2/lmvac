package dev.lmv.lmvac.api.modules.checks.aim;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.other.PlayerRotationData;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.DescType;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

@SettingCheck(
        value = "AimA",
        cooldown = Cooldown.COOLDOWN,
        descType = DescType.BETA
)
public class AimA extends Check implements PacketCheck {

    private final List<Double> deltaYaws = new ArrayList<>();
    private final List<Double> deltaPitches = new ArrayList<>();
    private final int maxHistorySize = 100;

    private double distinctThreshold = 50.0;
    private double maxBuffer = 5.0;
    private double bufferDecrease = 0.25;

    private double buffer1 = 0.0;
    private double buffer2 = 0.0;

    public AimA(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        Player player = packetEvent.getPlayer();
        LmvPlayer client = LmvPlayer.get(player);

        if (client == null) return;
        if (player.isInsideVehicle()) return;
        if (!client.isInCombat()) return;
        if (client.rotationData.cinematicRotation) return;

        PlayerRotationData data = client.rotationData;

        double deltaYaw = Math.abs(data.deltaYaw);
        double deltaPitch = Math.abs(data.deltaPitch);

        if (deltaYaw > 0.35 && deltaYaw < 20.0) {
            this.deltaYaws.add(deltaYaw);
        }

        if (deltaPitch > 0.35 && deltaPitch < 20.0) {
            this.deltaPitches.add(deltaPitch);
        }

        if (this.deltaYaws.size() > maxHistorySize) {
            double distinct = client.calculateDistinct(this.deltaYaws);

            if (distinct < distinctThreshold) {
                buffer1++;
                if (buffer1 > maxBuffer) {
                    flag(player, String.format("Low yaw diversity: %.2f%% (buffer: %.2f)", distinct, buffer1));
                    buffer1 = 0.0;
                }
            } else if (buffer1 > 0.0) {
                buffer1 -= bufferDecrease;
            }

            this.deltaYaws.remove(0);
        }

        if (this.deltaPitches.size() > maxHistorySize) {
            double distinct = client.calculateDistinct(this.deltaPitches);

            if (distinct < distinctThreshold) {
                buffer2++;
                if (buffer2 > maxBuffer) {
                    flag(player, String.format("Low pitch diversity: %.2f%% (buffer: %.2f)", distinct, buffer2));
                    buffer2 = 0.0;
                }
            } else if (buffer2 > 0.0) {
                buffer2 -= bufferDecrease;
            }

            this.deltaPitches.remove(0);
        }
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder()
                .types(PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK)
                .build();
    }
}