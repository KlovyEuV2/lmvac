package dev.lmv.lmvac.api.modules.checks.aim;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Client;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import dev.lmv.lmvac.api.modules.checks.aim.utils.AimUtil;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

// простой Aim-паттернLook(детектит только если противник на месте, в ост - оч плохо!)
@SettingCheck(
   value = "AimA",
   cooldown = Cooldown.COOLDOWN
)
public class AimA extends Check implements PacketCheck {
   public AimA(Plugin plugin) {
      super(plugin);
   }

   public void onPacketReceiving(PacketEvent packetEvent) {
      Player player = packetEvent.getPlayer();
      LmvPlayer targetPlayer = LmvPlayer.get(player);
      if (targetPlayer != null) {
         List<LmvPlayer.LookInformation> looks = targetPlayer.looks;
         if (looks.size() >= 2) {
             int lockedCount = AimUtil.getLockCount(looks);
             int smoothCount = AimUtil.getSmoothAim(looks, 1.5F, 15.0F, 1.5F, 15.0F);
             int moveCount = AimUtil.getConsistentMovementAim(looks, packetEvent, 1.5F, 15.0F, 1.5F, 15.0F);
             boolean smoothLook = smoothCount > 10;
             boolean moveAim = moveCount > 8;
             boolean lockedLook = lockedCount > 13;
             if (lockedLook && moveAim && smoothLook) {
                 flag(player);
             }
         }
      }
   }

   public ListeningWhitelist getReceivingWhitelist() {
      return ListeningWhitelist.newBuilder().types(new PacketType[]{Client.LOOK, Client.POSITION_LOOK}).build();
   }
}
