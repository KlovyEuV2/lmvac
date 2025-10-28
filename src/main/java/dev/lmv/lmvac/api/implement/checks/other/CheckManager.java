package dev.lmv.lmvac.api.implement.checks.other;

import dev.lmv.lmvac.LmvAC;
import dev.lmv.lmvac.api.ConfigManager;
import dev.lmv.lmvac.api.implement.ai.listener.AimPacketListener;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.modules.checks.aim.AimA;
import dev.lmv.lmvac.api.modules.checks.aim.snaps.Snap360;
import dev.lmv.lmvac.api.modules.checks.aim.snaps.SnapElytra;
import dev.lmv.lmvac.api.modules.checks.aim.AimNpc;
import dev.lmv.lmvac.api.modules.checks.autoclicker.inventory.ClickSpamA;
import dev.lmv.lmvac.api.modules.checks.autoclicker.packet.PacketSpamA;
import dev.lmv.lmvac.api.modules.checks.badpackets.inventory.*;
import dev.lmv.lmvac.api.modules.checks.badpackets.other.BadPacketsB;
import dev.lmv.lmvac.api.modules.checks.flight.FlightC;
import dev.lmv.lmvac.api.modules.checks.inventory.*;
import dev.lmv.lmvac.api.modules.checks.meta.AttributeCancel;
import dev.lmv.lmvac.api.modules.checks.meta.MetaCancel;
import dev.lmv.lmvac.api.modules.checks.multiactions.MultiActionsA;
import dev.lmv.lmvac.api.modules.checks.other.AutoFishA;
import dev.lmv.lmvac.api.modules.checks.sprint.SprintB;
import dev.lmv.lmvac.api.modules.checks.timer.NegativeTimer;
import dev.lmv.lmvac.api.modules.checks.timer.PacketTimer;

import java.util.*;

import org.bukkit.plugin.Plugin;

public class CheckManager {
    public static List<Check> checks = new ArrayList<>();

    public static NegativeTimer negativeTimer;
    public static PacketTimer timer;

    public CheckManager(Plugin plugin) {
        ConfigManager.setCheckManager(this);

        checks = new ArrayList<>(Arrays.asList(
                new AimNpc(plugin), new AimPacketListener(LmvAC.instance),
                new AttributeCancel(plugin), new AutoFishA(plugin),
                new BadPacketsA(plugin), new BadPacketsD(plugin),
                new ClickSpamA(plugin),
                new FlightC(plugin),
                new InventoryB(plugin), new InventoryC(plugin), new InventoryE(plugin), new InventoryF(plugin), new InventoryG(plugin), new InventoryH(plugin),
                new MetaCancel(plugin),
                new MultiActionsA(plugin),
                new PacketSpamA(plugin),
                new Snap360(plugin), new SnapElytra(plugin),
                new SprintB(plugin)
        ));

        for (Check check : checks) {
            check.register();
        }

        NegativeTimer nt = new NegativeTimer(plugin);
        PacketTimer pt = new PacketTimer(plugin);

        negativeTimer = nt;
        timer = pt;

        checks.add(nt);
        checks.add(pt);
    }

    public void reloadChecks() {
        for (Check check : checks) {
            check.reload();
        }
    }

    public static List<Check> getChecks() {
        return new ArrayList<>(checks);
    }
}