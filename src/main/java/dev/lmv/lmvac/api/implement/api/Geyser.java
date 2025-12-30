package dev.lmv.lmvac.api.implement.api;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.lmv.lmvac.LmvAC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Geyser implements Listener {
    private static Method geyserApiMethod = null;
    private static Method isBedrockPlayerMethod = null;
    private static boolean reflectionInitialized = false;
    private static final CopyOnWriteArrayList<UUID> bedrockPlayers = new CopyOnWriteArrayList<>();

    public final LmvAC plugin;
    public Geyser(LmvAC plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (isBedrockPlayerViaReflection(playerId)) {
            bedrockPlayers.add(playerId);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        bedrockPlayers.remove(playerId);
    }

    private boolean isBedrockPlayerViaReflection(UUID uuid) {
        if (Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null && Bukkit.getPluginManager().getPlugin("Geyser-Spigot").isEnabled()) {
            if (!reflectionInitialized) {
                initializeReflection();
            }

            if (geyserApiMethod != null && isBedrockPlayerMethod != null) {
                try {
                    Object geyserApiInstance = geyserApiMethod.invoke((Object)null);
                    return (Boolean)isBedrockPlayerMethod.invoke(geyserApiInstance, uuid);
                } catch (Exception var2) {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean isBedrockPlayer(UUID uuid) {
        return bedrockPlayers.contains(uuid);
    }

    private static void initializeReflection() {
        try {
            Class<?> geyserApiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
            geyserApiMethod = geyserApiClass.getMethod("api");
            isBedrockPlayerMethod = geyserApiClass.getMethod("isBedrockPlayer", UUID.class);
        } catch (Exception ignored) {
        } finally {
            reflectionInitialized = true;
        }
    }

    public static boolean isBedrockPlayer(Player player) {
        return isBedrockPlayer(player.getUniqueId());
    }
}