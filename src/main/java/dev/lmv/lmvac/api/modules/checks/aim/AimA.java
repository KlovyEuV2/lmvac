package dev.lmv.lmvac.api.modules.checks.aim;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@SettingCheck(
        value = "AimA",
        cooldown = Cooldown.COOLDOWN
)
public class AimA extends Check implements PacketCheck {

    private static final int SAMPLE_SIZE = 40;
    private static final double MIN_DELTA_TO_CHECK = 0.5;

    private static final double SMALL_DELTA_MAX = 2.5;
    private static final double MEDIUM_DELTA_MAX = 7.0;

    private static final double MAX_SMALL_DELTA_RATIO = 0.75;
    private static final double MIN_LARGE_DELTA_RATIO = 0.05;

    private final Logger logger;

    public AimA(Plugin plugin) {
        super(plugin);
        this.logger = plugin.getLogger();
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        Player player = packetEvent.getPlayer();
        LmvPlayer lmvPlayer = LmvPlayer.get(player);

        if (lmvPlayer == null || lmvPlayer.looks.size() < SAMPLE_SIZE + 1) {
            return;
        }

        List<LmvPlayer.LookInformation> looks = lmvPlayer.looks;
        int startIndex = looks.size() - (SAMPLE_SIZE + 1);

        int smallDeltas = 0;
        int mediumDeltas = 0;
        int largeDeltas = 0;
        int totalValidDeltas = 0;

        for (int i = startIndex; i < looks.size() - 1; i++) {
            LmvPlayer.LookInformation currentLook = looks.get(i);
            LmvPlayer.LookInformation nextLook = looks.get(i + 1);

            float deltaYaw = Math.abs(nextLook.location.getYaw() - currentLook.location.getYaw());
            float deltaPitch = Math.abs(nextLook.location.getPitch() - currentLook.location.getPitch());
            double totalDelta = deltaYaw + deltaPitch;

            if (totalDelta < MIN_DELTA_TO_CHECK) {
                continue;
            }

            totalValidDeltas++;

            if (totalDelta <= SMALL_DELTA_MAX) {
                smallDeltas++;
            } else if (totalDelta <= MEDIUM_DELTA_MAX) {
                mediumDeltas++;
            } else {
                largeDeltas++;
            }
        }

        if (totalValidDeltas < SAMPLE_SIZE * 0.5) {
            return;
        }

        double smallDeltaRatio = (double) smallDeltas / totalValidDeltas;
        double largeDeltaRatio = (double) largeDeltas / totalValidDeltas;

        logger.info(String.format("[AimA Analysis] Player: %s | Small: %.2f%%, Medium: %.2f%%, Large: %.2f%%",
                player.getName(),
                smallDeltaRatio * 100,
                (double) mediumDeltas / totalValidDeltas * 100,
                largeDeltaRatio * 100
        ));

        boolean flagged = false;
        String reason = "";

        if (smallDeltaRatio > MAX_SMALL_DELTA_RATIO) {
            flagged = true;
            reason += " too many small deltas";
        }

        if (largeDeltaRatio < MIN_LARGE_DELTA_RATIO) {
            flagged = true;
            reason += " not enough large deltas";
        }

        if (flagged) {
            flag(player, reason.trim());
        }
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().types(PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK).build();
    }
}