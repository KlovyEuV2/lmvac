package dev.lmv.lmvac;

import dev.lmv.lmvac.api.ConfigManager;
import dev.lmv.lmvac.api.commands.CustomChecksCommand;
import dev.lmv.lmvac.api.commands.Main_Command;
import dev.lmv.lmvac.api.implement.ai.AimAIDetector;
import dev.lmv.lmvac.api.implement.ai.commands.LMVAICommand;
import dev.lmv.lmvac.api.implement.ai.data.AimDataCollector;
import dev.lmv.lmvac.api.implement.ai.listener.AimPacketListener;
import dev.lmv.lmvac.api.implement.animations.api.Blocker;
import dev.lmv.lmvac.api.implement.api.Geyser;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.api.lmvEvents.api.listeners.ClientListener;
import dev.lmv.lmvac.api.implement.api.npcs.NpcManager;
import dev.lmv.lmvac.api.implement.api.packetListeners.InventoryListener;
import dev.lmv.lmvac.api.implement.api.settings.LocaleLoader;
import dev.lmv.lmvac.api.implement.api.settings.LocaleManager;
import dev.lmv.lmvac.api.implement.api.settings.SettingsSetter;
import dev.lmv.lmvac.api.implement.checks.other.CheckManager;
import dev.lmv.lmvac.api.implement.modutils.AlertsManager;
import dev.lmv.lmvac.api.implement.themes.ThemeManager;
import dev.lmv.lmvac.api.implement.utils.data.PlayerDataManager;
import dev.lmv.lmvac.api.implement.utils.inventory.InventoryUtil;
import dev.lmv.lmvac.api.implement.utils.listeners.MovementListener;
import dev.lmv.lmvac.api.implement.utils.punishments.Punishments;
import dev.lmv.lmvac.api.implement.utils.text.ColorUtil;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LmvAC extends JavaPlugin {
    public static LmvAC instance;

    private static ConfigManager configManager;
    private static AlertsManager alertsManager;
    private static Main_Command mainCommand;
    private static CheckManager checkHandler;
    private static long loadTime = 0L;

    private dev.lmv.lmvac.api.implement.checks.custom.CustomCheckManager customCheckManager;

    private static AimDataCollector mlDataCollector;
    private static AimAIDetector mlAIDetector;
    private File mlDataFolder;

    private static AimDataCollector aimDataCollector;
    private static AimAIDetector aimAIDetector;
    private File aimDataFolder;

    public static boolean placeholderAPI = false;

    public static String version = "v1.2.0F";
    public static String supportLink = "https://dsc.gg/lmvdev";

    public static final JavaPlugin getInstance() {
        return getPlugin(LmvAC.class);
    }

    @Override
    public void onEnable() {
        instance = this;
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
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            Bukkit.getLogger().warning("PlaceholderAPI not found. skipping.");
        } else {
            Bukkit.getLogger().warning("PlaceholderAPI found. started using it.");
            placeholderAPI = true;
        }

        long now = System.currentTimeMillis();
        loadTime = now;

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

        new MovementListener(this);
        now = System.currentTimeMillis();
        Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&bLoaded. &7(" + (now - loadTime) + "ms)"));
        Bukkit.getConsoleSender().sendMessage("");

        loadTime = System.currentTimeMillis();

        checkHandler = new CheckManager(this);
        now = System.currentTimeMillis();
        Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&bLoaded. &7(" + (now - loadTime) + "ms)"));
        Bukkit.getConsoleSender().sendMessage("");

        new Blocker(this);
        new LmvPlayer(this);
        new InventoryListener(this);
        new InventoryUtil(this);
        new NpcManager(this);
        new ClientListener(this);

        loadTime = System.currentTimeMillis();
        Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&bLoading ML AntiCheat..."));
        initMLAntiCheat();
        now = System.currentTimeMillis();
        Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&bLoaded. &7(" + (now - loadTime) + "ms)"));
        Bukkit.getConsoleSender().sendMessage("");

        loadTime = System.currentTimeMillis();
        Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&bLoading Aim AI..."));
        initAimAI();
        now = System.currentTimeMillis();
        Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&bLoaded. &7(" + (now - loadTime) + "ms)"));
        Bukkit.getConsoleSender().sendMessage("");

        Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&aLoaded. Thanks for Downloading."));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.setColorCodes("&bSupport: &9dsc.gg/lmvdev"));

        new SettingsSetter(this);

        new Geyser(this);
        initCustomChecks();
    }

    private void initMLAntiCheat() {
        try {
            File aiFolder = new File(getDataFolder(), "AI");
            mlDataFolder = new File(aiFolder, "datas");
            if (!mlDataFolder.exists()) {
                mlDataFolder.mkdirs();
                getLogger().info("  ├─ Создана папка ML: " + mlDataFolder.getAbsolutePath());
            }

            copyDefaultMLDataIfNeeded();

            mlDataCollector = new AimDataCollector(this);
            mlAIDetector = new AimAIDetector(this);
            getLogger().info("  ├─ ML компоненты инициализированы");

            PluginCommand aiCommand = Bukkit.getPluginCommand("lmvai");
            if (aiCommand != null) {
                LMVAICommand executor = new LMVAICommand(this);
                aiCommand.setExecutor(executor);
                aiCommand.setTabCompleter(executor);
                getLogger().info("  ├─ Команда /lmvai зарегистрирована");
            }

            new AimPacketListener(this);
            getLogger().info("  ├─ ML Combat Listener зарегистрирован");

            mlAIDetector.loadModel();

            if (!mlAIDetector.isTrained()) {
                getLogger().warning("  └─ ⚠ ML модель не обучена! Используйте /lmvai train");
            } else {
                getLogger().info("  └─ ✓ ML модель загружена и готова!");
            }
        } catch (Exception e) {
            getLogger().severe("  └─ ✗ Ошибка инициализации ML AntiCheat:");
            e.printStackTrace();
        }
    }

    private void initAimAI() {
        try {
            File aiFolder = new File(getDataFolder(), "AI");
            aimDataFolder = new File(aiFolder, "aim_data");
            if (!aimDataFolder.exists()) {
                aimDataFolder.mkdirs();
                getLogger().info("  ├─ Создана папка Aim AI: " + aimDataFolder.getAbsolutePath());
            }

            aimDataCollector = new AimDataCollector(this);
            aimAIDetector = new AimAIDetector(this);
            getLogger().info("  ├─ Aim AI компоненты инициализированы");

            PluginCommand aimCommand = Bukkit.getPluginCommand("aimai");
            if (aimCommand != null) {
                LMVAICommand executor = new LMVAICommand(this);
                aimCommand.setExecutor(executor);
                aimCommand.setTabCompleter(executor);
                getLogger().info("  ├─ Команда /aimai зарегистрирована");
            }

            new dev.lmv.lmvac.api.implement.ai.listener.AimPacketListener(this);
            getLogger().info("  ├─ Aim AI Listener зарегистрирован");

            aimAIDetector.loadModel();

            if (!aimAIDetector.isTrained()) {
                getLogger().warning("  └─ ⚠ Aim AI модель не обучена! Используйте /aimai train");
            } else {
                getLogger().info("  └─ ✓ Aim AI модель загружена и готова!");
            }
        } catch (Exception e) {
            getLogger().severe("  └─ ✗ Ошибка инициализации Aim AI:");
            e.printStackTrace();
        }
    }

    private void copyDefaultMLDataIfNeeded() {
        if (mlDataFolder.listFiles((dir, name) -> name.endsWith(".data")) != null &&
                mlDataFolder.listFiles((dir, name) -> name.endsWith(".data")).length > 0) {
            getLogger().info("  ├─ Найдено ML данных");
            return;
        }
        getLogger().info("  ├─ Копирование дефолтных ML данных...");
        String[] defaultFiles = {"cheat_default.data", "player_default.data"};
        for (String fileName : defaultFiles) {
            try (InputStream in = getResource("datas/" + fileName)) {
                if (in != null) {
                    File outFile = new File(mlDataFolder, fileName);
                    Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    getLogger().info("  │  ├─ " + fileName);
                }
            } catch (Exception ignored) {
            }
        }
    }

    public File getMLDataFolder() {
        return mlDataFolder;
    }

    public File getAimDataFolder() {
        return aimDataFolder;
    }

    public static AimDataCollector getMLDataCollector() {
        return mlDataCollector;
    }

    public static AimAIDetector getMLAIDetector() {
        return mlAIDetector;
    }

    public static AimDataCollector getAimDataCollector() {
        return aimDataCollector;
    }

    public static AimAIDetector getAimAIDetector() {
        return aimAIDetector;
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

    @Override
    public void onDisable() {
        CheckManager.negativeTimer.shutdown();
        CheckManager.timer.shutdown();

        PlayerDataManager.close();

        if (mlAIDetector != null) {
            mlAIDetector.saveModel();
        }

        if (aimAIDetector != null) {
            aimAIDetector.saveModel();
        }

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
            NpcManager.destroyNpc(player, npc.entityId);
        }

        for (Player senderPlr : Bukkit.getOnlinePlayers()) {
            if (!Main_Command.spectatorsUUIDS.containsKey(senderPlr.getUniqueId())) {
                continue;
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