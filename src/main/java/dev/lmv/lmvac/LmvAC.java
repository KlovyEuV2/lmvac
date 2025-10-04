package dev.lmv.lmvac;

import dev.lmv.lmvac.api.ConfigManager;
import dev.lmv.lmvac.api.commands.CustomChecksCommand;
import dev.lmv.lmvac.api.commands.Main_Command;
import dev.lmv.lmvac.api.implement.animations.api.Blocker;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.api.lmvEvents.api.listeners.ClientListener;
import dev.lmv.lmvac.api.implement.api.npcs.NpcManager;
import dev.lmv.lmvac.api.implement.api.packetListeners.InventoryListener;
import dev.lmv.lmvac.api.implement.api.settings.LocaleLoader;
import dev.lmv.lmvac.api.implement.api.settings.LocaleManager;
import dev.lmv.lmvac.api.implement.api.settings.SettingsSetter;
import dev.lmv.lmvac.api.implement.checks.other.CheckManager;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.modutils.AlertsManager;
import dev.lmv.lmvac.api.implement.themes.ThemeManager;
import dev.lmv.lmvac.api.implement.utils.listeners.MovementListener;
import dev.lmv.lmvac.api.implement.utils.punishments.Punishments;
import dev.lmv.lmvac.api.implement.utils.text.ColorUtil;
import java.util.Iterator;
import java.util.UUID;

import dev.lmv.lmvac.api.modules.checks.timer.NegativeTimer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LmvAC extends JavaPlugin {
    private static ConfigManager configManager;
    private static AlertsManager alertsManager;
    private static Main_Command mainCommand;
    private static CheckManager checkHandler;
    private static long loadTime = 0L;

    private dev.lmv.lmvac.api.implement.checks.custom.CustomCheckManager customCheckManager;

    public static String version = "v1.0.74F";
    public static String supportLink = "https://dsc.gg/lmvdev";

    public static final JavaPlugin getInstance() {
        return getPlugin(LmvAC.class);
    }

    public void onEnable() {
        this.saveDefaultConfig();
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&b██╗░░░░░███╗░░░███╗██╗░░░██╗░█████╗░░█████╗░"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&b██║░░░░░████╗░████║██║░░░██║██╔══██╗██╔══██╗"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&b██║░░░░░██╔████╔██║╚██╗░██╔╝███████║██║░░╚═╝"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&b██║░░░░░██║╚██╔╝██║░╚████╔╝░██╔══██║██║░░██╗"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&b███████╗██║░╚═╝░██║░░╚██╔╝░░██║░░██║╚█████╔╝"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&b╚══════╝╚═╝░░░░░╚═╝░░░╚═╝░░░╚═╝░░╚═╝░╚════╝░"));
        Bukkit.getConsoleSender().sendMessage("");

        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
            Bukkit.getLogger().warning("ProtocolLib not found. Disabling LmvAC.");
            Bukkit.getPluginManager().disablePlugin(this);
        } else {
            long now = System.currentTimeMillis();
            loadTime = now;
            Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&bLoading Api..."));
            new LocaleLoader(this);
            new LocaleManager(this);
            new Punishments(this);
            configManager = new ConfigManager(this);
            alertsManager = new AlertsManager(this);
            mainCommand = new Main_Command(this);
            new ThemeManager(this);
            now = System.currentTimeMillis();
            Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&bLoaded. &7(" + (now - loadTime) + "ms)"));
            Bukkit.getConsoleSender().sendMessage("");
            loadTime = System.currentTimeMillis();
            Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&bLoading Listeners..."));
            new MovementListener(this);
            now = System.currentTimeMillis();
            Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&bLoaded. &7(" + (now - loadTime) + "ms)"));
            Bukkit.getConsoleSender().sendMessage("");
            loadTime = System.currentTimeMillis();
            Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&bLoading Checks..."));
            checkHandler = new CheckManager(this);
            now = System.currentTimeMillis();
            Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&bLoaded. &7(" + (now - loadTime) + "ms)"));
            Bukkit.getConsoleSender().sendMessage("");
            loadTime = System.currentTimeMillis();
            Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&bLoading Player-Api..."));
            new Blocker(this);
            new LmvPlayer(this);
            new InventoryListener(this);
            new NpcManager(this);
            new ClientListener(this);
            now = System.currentTimeMillis();
            Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&bLoaded. &7(" + (now - loadTime) + "ms)"));
            Bukkit.getConsoleSender().sendMessage("");
            Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&aLoaded. Thanks for Downloading."));
            Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&bSupport: &9dsc.gg/lmvdev"));
            Bukkit.getConsoleSender().sendMessage("");
            new SettingsSetter(this);
            initCustomChecks();
        }
    }

    private void initCustomChecks() {
        try {
            customCheckManager = new dev.lmv.lmvac.api.implement.checks.custom.CustomCheckManager(this);

            customCheckManager.loadAllChecks();

            PluginCommand ccCommand = Bukkit.getPluginCommand("lmvlua");
            if (ccCommand != null) {
                CustomChecksCommand executor = new CustomChecksCommand(customCheckManager);
                ccCommand.setExecutor(executor);
                ccCommand.setTabCompleter(executor);
                getLogger().info("§a - Команда /lmvlua зарегистрирована");
            } else {
                getLogger().warning("§e - Команда lmvlua не найдена в plugin.yml!");
            }

        } catch (Exception e) {
            getLogger().severe("§c - Ошибка инициализации lmvlua:");
            e.printStackTrace();
        }
    }

    public void onDisable() {
        CheckManager.negativeTimer.shutdown();
        CheckManager.timer.shutdown();
        if (customCheckManager != null) {
            customCheckManager.getLoadedChecks().forEach(check -> {
                try {
                    check.unregister();
                } catch (Exception e) {
                    getLogger().warning("§cОшибка отключения чека: " + check.getName());
                }
            });
        }
        for (UUID uuid : NpcManager.npcMap.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            NpcManager.TrackedNpc npc = NpcManager.npcMap.get(uuid);
            NpcManager.destroyNpc(player,npc.entityId);
        }
        for (Player senderPlr : Bukkit.getOnlinePlayers()) {
            if (!Main_Command.spectatorsUUIDS.containsKey(senderPlr.getUniqueId())) {
                return;
            }

            UUID senderUUID = senderPlr.getUniqueId();
            Location returnLoc = (Location) Main_Command.spectatorsLOCS.get(senderUUID);
            GameMode returnGM = (GameMode) Main_Command.spectatorsGMS.get(senderUUID);
            senderPlr.teleport(returnLoc);
            senderPlr.setGameMode(returnGM);

            for (Player online : Bukkit.getOnlinePlayers()) {
                online.showPlayer(getInstance(), senderPlr);
            }

            Main_Command.spectatorsUUIDS.remove(senderUUID);
            Main_Command.spectatorsLOCS.remove(senderUUID);
            Main_Command.spectatorsGMS.remove(senderUUID);
            senderPlr.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&eВы прекратили спекать за игроком. Причина: Плагин отключился."));
            senderPlr.sendMessage(ColorUtil.setColorCodes(" &cВнимание! У вас возможно был отключена невидимость для других игроков&8(/vanish,/v)"));
            NpcManager.clearAllNpcs();
        }
    }
}