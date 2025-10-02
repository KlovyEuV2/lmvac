package dev.lmv.lmvac.api.implement.checks.type;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.temporary.TemporaryPlayer;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.api.settings.LocaleManager;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import dev.lmv.lmvac.api.implement.modutils.AlertsManager;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public abstract class Check {
    protected final Plugin plugin;
    public String name;
    private boolean enabled;
    private boolean registered = false;
    private PacketListener packetListener;
    private final ProtocolManager protocolManager;
    private Cooldown cooldown;
    public boolean remoteEventEnabled = false;

    public ConcurrentHashMap<String,String> locales = new ConcurrentHashMap<>();

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
        SettingCheck setting = (SettingCheck)clazz.getAnnotation(SettingCheck.class);

        if (setting == null) {
            throw new IllegalStateException("Check class " + clazz.getSimpleName() + " must have @SettingCheck annotation!");
        } else {
            this.name = setting.value();
            this.cooldown = setting.cooldown();
            this.enabled = this.isEnabledInConfig();

            if (this instanceof PacketCheck) {
                this.packetListener = this.createPacketListener();
            }
            loadLocale();
        }
    }

    private boolean isEnabledInConfig() {
        return !this.plugin.getConfig().getStringList("checks.disabled").contains(this.name);
    }

    private PacketListener createPacketListener() {
        if (!(this instanceof PacketCheck)) {
            return null;
        } else {
            final PacketCheck packetCheck = (PacketCheck)this;
            return new PacketListener() {
                public void onPacketReceiving(PacketEvent event) {
                    if (Check.this.enabled && Check.this.registered) {
                        if (event.getPlayer() == null || !event.getPlayer().isOnline() || (event.getPlayer() instanceof TemporaryPlayer)) return;
                        int id = event.getPlayer().getEntityId();
                        LmvPlayer targetPlayer = (LmvPlayer)LmvPlayer.players.get(id);
                        if (targetPlayer != null) {
                            if (!targetPlayer.hasBypass(Check.this.getName())) {
                                try {
                                    packetCheck.onPacketReceiving(event);
                                } catch (Exception var5) {
                                    var5.printStackTrace();
                                }

                            }
                        }
                    }
                }

                public void onPacketSending(PacketEvent event) {
                    if (Check.this.enabled && Check.this.registered) {
                        if (event.getPlayer() == null || !event.getPlayer().isOnline() || (event.getPlayer() instanceof TemporaryPlayer)) return;
                        int id = event.getPlayer().getEntityId();
                        LmvPlayer targetPlayer = (LmvPlayer)LmvPlayer.players.get(id);
                        if (targetPlayer != null) {
                            if (!targetPlayer.hasBypass(Check.this.getName())) {
                                try {
                                    packetCheck.onPacketSending(event);
                                } catch (Exception var5) {
                                    var5.printStackTrace();
                                }

                            }
                        }
                    }
                }

                public ListeningWhitelist getSendingWhitelist() {
                    return packetCheck.getSendingWhitelist();
                }

                public ListeningWhitelist getReceivingWhitelist() {
                    return packetCheck.getReceivingWhitelist();
                }

                public Plugin getPlugin() {
                    return Check.this.plugin;
                }
            };
        }
    }

    public void register() {
        if (!this.enabled) {
            this.unregister();
        } else if (!this.registered) {
            if (this instanceof Listener) {
                Bukkit.getPluginManager().registerEvents((Listener)this, this.plugin);
            }

            if (this instanceof PacketCheck && this.packetListener != null) {
                try {
                    this.protocolManager.addPacketListener(this.packetListener);
                } catch (Exception var2) {
                    var2.printStackTrace();
                }
            }

            this.registered = true;
        }
    }

    public void unregister() {
        if (this.registered) {
            if (this instanceof Listener) {
                HandlerList.unregisterAll((Listener)this);
            }

            if (this instanceof PacketCheck && this.packetListener != null) {
                try {
                    this.protocolManager.removePacketListener(this.packetListener);
                } catch (Exception var2) {
                }
            }

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
        }
        loadLocale();
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

    public void flag(Player player) {
        if (player != null) {
            AlertsManager.sendFlagAlerts((List)null, "lmvac.alerts", true, player, this.name, 0.09, this.cooldown == Cooldown.NO_COOLDOWN ? 0L : 100L);
        }
    }

    public void flag(Player player, String reason) {
        if (player != null) {
            AlertsManager.sendFlagAlerts((List)null, "lmvac.alerts", true, player, this.name, 0.09, this.cooldown == Cooldown.NO_COOLDOWN ? 0L : 100L, reason);
        }
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