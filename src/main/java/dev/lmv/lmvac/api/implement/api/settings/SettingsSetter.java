package dev.lmv.lmvac.api.implement.api.settings;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SettingsSetter {

    private final Map<UUID, LmvPlayer.ClientSettings> settingsMap = new ConcurrentHashMap<>();

    public SettingsSetter(Plugin plugin) {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
                ListenerPriority.NORMAL, PacketType.Play.Client.SETTINGS) {

            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                PacketContainer packet = event.getPacket();

                int id = player.getEntityId();
                LmvPlayer client = (LmvPlayer)LmvPlayer.players.get(id);
                if (client == null) return;

                String locale = packet.getStrings().readSafely(0);
                Integer viewDistance = packet.getIntegers().readSafely(0);
                EnumWrappers.ChatVisibility chatVisibility = packet.getChatVisibilities().readSafely(0);
                Boolean chatColors = packet.getBooleans().readSafely(0);
                Byte skinPartsByte = packet.getBytes().readSafely(0);
                EnumWrappers.Hand mainHand = packet.getHands().readSafely(0);
                Boolean textFiltering = packet.getBooleans().readSafely(1);
                Boolean allowServerListings = packet.getBooleans().readSafely(2);

                String safeLocale = (locale != null) ? locale : "en_us";
                int safeViewDistance = (viewDistance != null) ? viewDistance : 8;
                EnumWrappers.ChatVisibility safeChatVisibility = (chatVisibility != null) ? chatVisibility : EnumWrappers.ChatVisibility.FULL;
                boolean safeChatColors = (chatColors != null) ? chatColors : true;
                byte safeSkinParts = (skinPartsByte != null) ? skinPartsByte : 0;
                EnumWrappers.Hand safeMainHand = (mainHand != null) ? mainHand : EnumWrappers.Hand.OFF_HAND;
                boolean safeTextFiltering = (textFiltering != null) ? textFiltering : true;
                boolean safeAllowServerListings = (allowServerListings != null) ? allowServerListings : true;

                LmvPlayer.ClientSettings settings = new LmvPlayer.ClientSettings(
                        safeLocale,
                        safeViewDistance,
                        safeChatVisibility,
                        safeChatColors,
                        safeSkinParts,
                        safeMainHand,
                        safeTextFiltering,
                        safeAllowServerListings
                );

                client.clientSettings = settings;
            }
        });
    }
}
