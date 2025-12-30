package dev.lmv.lmvac.api.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

@SettingCheck(
        value = "InventoryD"
)
public class InventoryD extends Check implements PacketCheck {
    public InventoryD(Plugin plugin) {
        super(plugin);
    }
    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        PacketType packetType = packetEvent.getPacketType();
        Player player = packetEvent.getPlayer();
        LmvPlayer lmvPlayer = LmvPlayer.get(player);
        if (lmvPlayer == null) return;
        if (isMovePacket(packetType)) {
            StringBuilder violations = getReason(lmvPlayer);
//            player.sendMessage("xz: " + lmvPlayer.deltaXZ() + ", y: " + lmvPlayer.deltaY());
            if (lmvPlayer.isInventoryOpened
                    && lmvPlayer.invTick >= 3) {
                // (Inventory-Tick) от ложных
                // (Клиент для остановки может отправить последние 1-2 пакета/тика).
                // invTick - текущий тик-движения
                // 1 Tick - клиент может отправить как пакет до открытия (Timing)
                // 2 Tick - Остановка, тоже может быть отправлен как прошлый
                // Поэтому мы их игнорируем от ложных, таков Mojang и кубики.
                if (violations.length() > 0) {
                    flag(player, violations.toString()
//                            + " tick=" + lmvPlayer.invTick
                    );
                }
            }
        }
    }

    public static @NotNull StringBuilder getReason(LmvPlayer lmvPlayer) {
        StringBuilder violations = new StringBuilder();
        if (isJumping(lmvPlayer)) violations.append("jumping ");
        if (isClimbing(lmvPlayer)) violations.append("climbing ");
        if (isMoving(lmvPlayer)) violations.append("moving ");
        if (isSwimming(lmvPlayer)) violations.append("swimming ");
        if (isSneaking(lmvPlayer)) violations.append("sneaking ");
        if (isShieldMoving(lmvPlayer)) violations.append("shield ");
        if (violations.length() > 0) violations.setLength(violations.length() - 1);
        return violations;
    }

    public static boolean isJumping(LmvPlayer client) {
        return client.simulationJump;
    }

    public static boolean isMoving(LmvPlayer client) {
        return client.simulationWalk && client.lastSimulationWalk && client.wasLastSimulationWalk;
    }

    public static boolean isSwimming(LmvPlayer client) {
        return client.simulationSwim && client.lastSimulationSwim && client.wasLastSimulationSwim;
    }

    public static boolean isShieldMoving(LmvPlayer client) {
        return client.simulationShield && client.lastSimulationShield && client.wasLastSimulationShield;
    }

    public static boolean isClimbing(LmvPlayer client) {
        return client.simulationClimb && client.lastSimulationClimb && client.wasLastSimulationClimb;
    }

    public static boolean isSneaking(LmvPlayer client) {
        return client.simulationSneak && client.lastSimulationSneak && client.wasLastSimulationSneak;
    }
}
