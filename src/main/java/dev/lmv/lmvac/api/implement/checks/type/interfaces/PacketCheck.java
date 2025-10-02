package dev.lmv.lmvac.api.implement.checks.type.interfaces;

import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import dev.lmv.lmvac.LmvAC;
import org.bukkit.plugin.Plugin;

public interface PacketCheck extends PacketListener {

    default void onPacketReceiving(PacketEvent event) {
    }

    default void onPacketSending(PacketEvent event) {
    }

    default ListeningWhitelist getSendingWhitelist() {
        return ListeningWhitelist.newBuilder().build();
    }

    default ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().build();
    }

    default Plugin getPlugin() {
        return LmvAC.getInstance();
    }
}
