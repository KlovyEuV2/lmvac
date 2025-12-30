package dev.lmv.lmvac.api.implement.checks.type;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.temporary.TemporaryPlayer;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.api.settings.LocaleManager;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.Configurable;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import dev.lmv.lmvac.api.implement.modutils.AlertsManager;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public abstract class Check {
    protected final Plugin plugin;
    public String name;
    private boolean enabled;
    private boolean registered = false;
    private PacketListener packetListener;
    private final ProtocolManager protocolManager;
    public Cooldown cooldown;
    public boolean remoteEventEnabled = false;

    public DescType descType;
    public String description;

    public ConcurrentHashMap<String,String> locales = new ConcurrentHashMap<>();

    private ConcurrentHashMap<UUID, CopyOnWriteArrayList<Long>> data = new ConcurrentHashMap<>();

    private BukkitRunnable runnable;

    private final long periodRD = 10;

    public ListeningWhitelist SENDING_LIST = null;
    public ListeningWhitelist RECEIVE_LIST = null;

    public void loadLocale() {
        String checkName = getName();

        List<String> messages = LocaleManager.current_.getStringList("checks." + checkName.toLowerCase());

        int i = 1;
        for (String msg : messages) {
            locales.put(String.valueOf(i), msg);
            i++;
        }
    }

    public Check(Plugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        Class<? extends Check> clazz = this.getClass();
        SettingCheck setting = clazz.getAnnotation(SettingCheck.class);

        if (setting == null) {
            throw new IllegalStateException("Check class " + clazz.getSimpleName() + " must have @SettingCheck annotation!");
        } else {
            this.descType = setting.descType();

            this.name = setting.value();
            this.description = setting.description();

            this.cooldown = setting.cooldown();
            this.enabled = this.isEnabledInConfig();

            loadLocale();

            runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    removeDPlayers();
                }
            };
            runnable.runTaskTimer(plugin,periodRD,periodRD);
        }
    }

    private boolean isEnabledInConfig() {
        return !this.plugin.getConfig().getStringList("checks.disabled").contains(this.name);
    }

    public void removeDPlayers() {
        long now = System.currentTimeMillis();
        long reset = 60000L;

        for (CopyOnWriteArrayList<Long> datas : data.values()) {
            if (datas.isEmpty()) continue;

            long last = datas.get(datas.size() - 1);
            long diff = now - last;

            if (diff >= reset) {
                datas.clear();
            }
        }
    }


    public void removeData(Player player) {
        data.remove(player.getUniqueId());
    }

    public void register() {
        if (!this.enabled) {
            this.unregister();
        } else if (!this.registered) {
            this.registered = true;
        }
    }

    public void unregister() {
        if (this.registered) {
            this.registered = false;
        }
    }

    public void reload() {
        boolean nowEnabled = this.isEnabledInConfig();

        if (nowEnabled && !this.registered) {
            this.enabled = true;
            this.register();
        } else if (!nowEnabled && this.registered) {
            this.enabled = false;
            this.unregister();
        } else {
            this.enabled = nowEnabled;
            loadLocale();
            if (this instanceof Configurable) {
                ((Configurable) this).reloadConfiguration(plugin);
            }
        }
    }

    public void remoteFlag(Player player) {
        if (remoteEventEnabled) {
            flag(player);
        }
    }

    public void remoteFlag(Player player, String reason) {
        if (remoteEventEnabled) {
            flag(player, reason);
        }
    }

    public boolean flag(Player player) {
        if (player != null) {
            AlertsManager.sendFlagAlerts(null, "lmvac.alerts", true, player, this.name, 0.09, this.cooldown == Cooldown.NO_COOLDOWN ? 0L : 100L);
            return true;
        }
        return false;
    }

    public boolean flag(Player player, String reason) {
        if (player != null) {
            AlertsManager.sendFlagAlerts(null, "lmvac.alerts", true, player, this.name, 0.09, this.cooldown == Cooldown.NO_COOLDOWN ? 0L : 100L, reason);
            return true;
        }
        return false;
    }

    public boolean reward(Player player) {
        CopyOnWriteArrayList<Long> datas = data.computeIfAbsent(player.getUniqueId(),k -> new CopyOnWriteArrayList<>());
        if (!datas.isEmpty()) {
            datas.remove(0);
        }
        return false;
    }

    public boolean setBack(LmvPlayer client) {
        if (client != null) {
            client.setBack();
            return true;
        }
        return false;
    }

    public boolean isMovePacket(PacketType type) {
        return type.equals(PacketType.Play.Client.POSITION) || type.equals(PacketType.Play.Client.POSITION_LOOK);
    }

    public boolean isRotationPacket(PacketType type) {
        return type.equals(PacketType.Play.Client.POSITION_LOOK) || type.equals(PacketType.Play.Client.LOOK);
    }
    
    public int getSuspends(Player player) {
        return data.computeIfAbsent(player.getUniqueId(),k -> new CopyOnWriteArrayList<>()).size();
    }

    public boolean suspend(Player player) {
        CopyOnWriteArrayList<Long> datas = data.computeIfAbsent(player.getUniqueId(),k -> new CopyOnWriteArrayList<>());
        long now = System.currentTimeMillis();
        datas.add(now);
        return false;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public String getName() {
        return this.name != null ? this.name : "Unknown";
    }

    public boolean isRegistered() {
        return this.registered;
    }

    protected ProtocolManager getProtocolManager() {
        return this.protocolManager;
    }
}