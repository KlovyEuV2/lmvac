package dev.lmv.lmvac.api.implement.modutils;

import dev.lmv.lmvac.api.ConfigManager;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.api.lmvEvents.events.flag.FlagEvent;
import dev.lmv.lmvac.api.implement.api.lmvEvents.events.flag.FlagType;
import dev.lmv.lmvac.api.implement.api.settings.LocaleManager;
import dev.lmv.lmvac.api.implement.themes.ThemeManager;
import dev.lmv.lmvac.api.implement.utils.data.PlayerDataManager;
import dev.lmv.lmvac.api.implement.utils.punishments.Punishments;
import dev.lmv.lmvac.api.implement.utils.text.ColorUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class AlertsManager implements Listener {
    private static final String ALERT_PERMISSION = "lmvac.alerts";
    private static final Vector ZERO_VELOCITY = new Vector(0.0, -0.1, 0.0);
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HOVER_PATTERN = Pattern.compile("%//([^/]*)//%");
    private static final int MAX_COMMANDS_PER_BATCH = 3;
    private static final long COMMAND_EXECUTION_DELAY = 20L;
    private static final long COMMAND_BATCH_INTERVAL = 100L;

    private static Plugin plugin;
    private static long timeWithoutAlertMessage;

    private static final ConcurrentHashMap<UUID, Long> lastMessageMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> vlMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Double> suspendsMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> lastUpdateTimeMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Integer> skippedAlertsMap = new ConcurrentHashMap<>();
    private static final Queue<CommandData> commandQueue = new ConcurrentLinkedQueue<>();

    private static ScheduledExecutorService commandExecutor;
    private static ScheduledExecutorService cleanupExecutor;

    private static final Map<String, BaseComponent[]> componentCache = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> themeCache = new ConcurrentHashMap<>();

    private static volatile double cachedTps = 20.0;
    private static volatile long lastTpsUpdate = 0L;

    public AlertsManager(JavaPlugin plugin) {
        AlertsManager.plugin = plugin;
        timeWithoutAlertMessage = plugin.getConfig().getLong("time-without-alert", 1380L);
        PlayerDataManager.setup(plugin);
        initExecutors();

        try {
            Bukkit.getPluginManager().registerEvents(this, plugin);
        } catch (Exception e) {
            Logger logger = Bukkit.getLogger();
            String prefix = ConfigManager.prefix;
            logger.severe(ColorUtil.setColorCodes(prefix + "Ошибка при инициализации AlertsManager: " + e.getMessage()));
            throw new RuntimeException(e);
        }
    }

    private static void initExecutors() {
        shutdown();
        commandExecutor = Executors.newSingleThreadScheduledExecutor((r) -> {
            Thread t = new Thread(r, "AlertsManager-CommandExecutor");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor((r) -> {
            Thread t = new Thread(r, "AlertsManager-Cleanup");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
        startCommandProcessor();
        startDataCleanup();
    }

    private static void startCommandProcessor() {
        commandExecutor.scheduleAtFixedRate(() -> {
            if (!commandQueue.isEmpty()) {
                try {
                    List<CommandData> batch = new ArrayList<>(MAX_COMMANDS_PER_BATCH);

                    for (int i = 0; i < MAX_COMMANDS_PER_BATCH && !commandQueue.isEmpty(); ++i) {
                        CommandData cmd = commandQueue.poll();
                        if (cmd != null) {
                            batch.add(cmd);
                        }
                    }

                    if (!batch.isEmpty()) {
                        executeCommandBatch(batch);
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().warning("Ошибка в обработчике команд: " + e.getMessage());
                }
            }
        }, 0L, COMMAND_BATCH_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private static void executeCommandBatch(List<CommandData> batch) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (CommandData cmdData : batch) {
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdData.command);
                    if (batch.size() > 1) {
                        try {
                            Thread.sleep(COMMAND_EXECUTION_DELAY);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().warning(String.format("Ошибка выполнения команды для %s: %s - %s",
                            cmdData.playerName, cmdData.command, e.getMessage()));
                }
            }
        });
    }

    private static void startDataCleanup() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                long cleanupThreshold = ConfigManager.toVlRemove;

                lastUpdateTimeMap.entrySet().removeIf((entry) -> {
                    if (currentTime - entry.getValue() > cleanupThreshold) {
                        vlMap.remove(entry.getKey());
                        return true;
                    }
                    return false;
                });

                if (componentCache.size() > 1000) {
                    componentCache.clear();
                }

                if (themeCache.size() > 100) {
                    themeCache.clear();
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("Ошибка в очистке данных: " + e.getMessage());
            }
        }, 0L, 1000L, TimeUnit.MILLISECONDS);
    }

    public static boolean sendFlagAlerts(List<String> messages, String alertPerm, boolean alertConsole,
                                         Player player, String suspendArg, Double suspends, long cooldown) {
        if (player == null || suspendArg == null || suspends == null) {
            return false;
        }

        try {
            long now = System.currentTimeMillis();
            String playerKey = player.getUniqueId().toString() + suspendArg;
            Long lastUpdate = lastUpdateTimeMap.get(playerKey);

            if (lastUpdate != null && now - lastUpdate < cooldown) {
                return false;
            }

            int currentVl = addVlToPlayer(player, suspendArg);
            if (Punishments.getLower(suspendArg, currentVl, "$flag$", 99) && !player.isDead()) {
                player.setVelocity(ZERO_VELOCITY);
            }

            updatePlayerSuspends(player, suspendArg, suspends);
            lastUpdateTimeMap.put(playerKey, now);

            int id = player.getEntityId();
            LmvPlayer client = (LmvPlayer) LmvPlayer.players.get(id);

            handleActions(client, player, suspendArg, currentVl);

            if (Punishments.getLower(suspendArg, currentVl, "$alert$", 99)) {
                sendAlerts(null, alertPerm, alertConsole, player, suspendArg);
            }

            executePunishmentCommand(player, suspendArg, currentVl);
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("Ошибка в sendFlagAlerts: " + e.getMessage());
            return false;
        }
    }

    private static void handleActions(LmvPlayer client, Player player, String suspendArg, Integer currentVl) {
        if (Punishments.getLower(suspendArg, currentVl, "$close_inv$", 999999911)) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                    player.updateInventory();
                }
            });
        }

        if (Punishments.getLower(suspendArg, currentVl, "$server_close_inv$", 999999912)) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    if (client == null || !client.isServerInventoryOpened) return;
                    player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                    player.updateInventory();
                }
            });
        }

        if (Punishments.getLower(suspendArg, currentVl, "$stop_sprinting$", 999999913)) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    player.setSprinting(false);
                }
            });
        }

        if (Punishments.getLower(suspendArg, currentVl, "$stop_sneaking$", 999999914)) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    player.setSneaking(false);
                }
            });
        }

        if (Punishments.getLower(suspendArg, currentVl, "$stop_gliding$", 999999915)) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    player.setGliding(false);
                }
            });
        }

        if (Punishments.getLower(suspendArg, currentVl, "$stop_flying$", 999999916)) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    player.setFlying(false);
                }
            });
        }

        if (Punishments.getLower(suspendArg, currentVl, "$disable_flight$", 999999917)) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    player.setAllowFlight(false);
                }
            });
        }

        if (Punishments.getLower(suspendArg, currentVl, "$update_inventory$", 999999918)) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    player.updateInventory();
                }
            });
        }

        if (Punishments.getLower(suspendArg, currentVl, "$player_update_inventory$", 999999919)) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    if (!client.isServerInventoryOpened) {
                        player.updateInventory();
                    }
                }
            });
        }

        if (Punishments.getLower(suspendArg, currentVl, "$server_update_inventory$", 999999920)) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    if (client.isServerInventoryOpened) {
                        player.updateInventory();
                    }
                }
            });
        }
    }

    public static boolean sendFlagAlerts(List<String> messages, String alertPerm, boolean alertConsole,
                                         Player player, String suspendArg, Double suspends, long cooldown, String reason) {
        if (player == null || suspendArg == null || suspends == null) {
            return false;
        }

        try {
            long now = System.currentTimeMillis();
            String playerKey = player.getUniqueId().toString() + suspendArg;
            Long lastUpdate = lastUpdateTimeMap.get(playerKey);

            if (lastUpdate != null && now - lastUpdate < cooldown) {
                return false;
            }

            int currentVl = addVlToPlayer(player, suspendArg);
            if (Punishments.getLower(suspendArg, currentVl, "$flag$", 99) && !player.isDead()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.getPluginManager().callEvent(new FlagEvent(player, FlagType.MOVEMENT_ONE, 1, "unknown", currentVl));
                });
            }

            updatePlayerSuspends(player, suspendArg, suspends);
            lastUpdateTimeMap.put(playerKey, now);

            int id = player.getEntityId();
            LmvPlayer client = (LmvPlayer) LmvPlayer.players.get(id);

            handleActions(client, player, suspendArg, currentVl);

            if (Punishments.getLower(suspendArg, currentVl, "$alert$", 99)) {
                sendAlerts(null, alertPerm, alertConsole, player, suspendArg, reason);
            }

            executePunishmentCommand(player, suspendArg, currentVl);
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("Ошибка в sendFlagAlerts: " + e.getMessage());
            return false;
        }
    }

    private static void updatePlayerSuspends(Player player, String suspendArg, double suspends) {
        String key = player.getUniqueId().toString() + suspendArg;
        double newValue = suspendsMap.merge(key, suspends, Double::sum);
        PlayerDataManager.saveSuspends(player.getUniqueId(), suspendArg, newValue);
    }

    private static void executePunishmentCommand(Player player, String suspendArg, int vl) {
        String command = Punishments.get(suspendArg, vl);
        if (command != null && (!command.startsWith("$") || !command.endsWith("$"))) {
            String processedCommand = command
                    .replace("%prefix%", ConfigManager.prefix)
                    .replace("%player%", player.getName());
            commandQueue.offer(new CommandData(processedCommand, player.getName()));
        }
    }

    public static boolean sendAlerts(List<String> messages, String permission, boolean console,
                                     Player player, String suspendArg) {
        if (player == null) {
            return false;
        }

        try {
            UUID playerUuid = player.getUniqueId();
            long now = System.currentTimeMillis();
            Long lastMsg = lastMessageMap.get(playerUuid);

            if (lastMsg != null && now - lastMsg < timeWithoutAlertMessage) {
                return false;
            }

            int playerVl = getVlOfPlayer(player, suspendArg);
            sendAlertsToPlayers(permission, player, suspendArg, playerVl);

            if (console) {
                sendAlertsToConsole(player, suspendArg, playerVl);
            }

            lastMessageMap.put(playerUuid, now);
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("Ошибка в sendAlerts: " + e.getMessage());
            return false;
        }
    }

    public static boolean sendAlerts(List<String> messages, String permission, boolean console,
                                     Player player, String suspendArg, String reason) {
        if (player == null) {
            return false;
        }

        try {
            UUID playerUuid = player.getUniqueId();
            long now = System.currentTimeMillis();
            Long lastMsg = lastMessageMap.get(playerUuid);

            if (lastMsg != null && now - lastMsg < timeWithoutAlertMessage) {
                return false;
            }

            int playerVl = getVlOfPlayer(player, suspendArg);
            sendAlertsToPlayers(permission, player, suspendArg, playerVl, reason);

            if (console) {
                sendAlertsToConsole(player, suspendArg, playerVl, reason);
            }

            lastMessageMap.put(playerUuid, now);
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("Ошибка в sendAlerts: " + e.getMessage());
            return false;
        }
    }

    private static void sendAlertsToPlayers(String permission, Player player, String suspendArg, int playerVl) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (permission != null && !online.hasPermission(permission)) {
                continue;
            }
            int id = player.getEntityId();
            LmvPlayer client = LmvPlayer.players.get(id);
            if (client != null && !client.alerts) continue;

            List<String> themeMessages = getPlayerThemeMessages(online.getUniqueId());
            List<String> processedMessages = processAlertMessages(themeMessages, player, suspendArg, playerVl);

            for (BaseComponent[] components : parseHoverMessages(processedMessages, player)) {
                online.spigot().sendMessage(components);
            }
        }
    }

    private static void sendAlertsToPlayers(String permission, Player player, String suspendArg, int playerVl, String reason) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (permission != null && !online.hasPermission(permission)) {
                continue;
            }
            int id = player.getEntityId();
            LmvPlayer client = LmvPlayer.players.get(id);
            if (client != null && !client.alerts) continue;

            List<String> themeMessages = getPlayerThemeMessages(online.getUniqueId());
            List<String> processedMessages = processAlertMessages(themeMessages, player, suspendArg, playerVl, reason);

            for (BaseComponent[] components : parseHoverMessages(processedMessages, player)) {
                online.spigot().sendMessage(components);
            }
        }
    }

    private static void sendAlertsToConsole(Player player, String suspendArg, int playerVl) {
        List<String> consoleMessages = getDefaultThemeMessages();
        List<String> processedMessages = processAlertMessages(consoleMessages, player, suspendArg, playerVl);

        for (BaseComponent[] components : parseHoverMessages(processedMessages, player)) {
            Bukkit.getConsoleSender().sendMessage(TextComponent.toLegacyText(components));
        }
    }

    private static void sendAlertsToConsole(Player player, String suspendArg, int playerVl, String reason) {
        List<String> consoleMessages = getDefaultThemeMessages(reason);
        List<String> processedMessages = processAlertMessages(consoleMessages, player, suspendArg, playerVl, reason);

        for (BaseComponent[] components : parseHoverMessages(processedMessages, player)) {
            Bukkit.getConsoleSender().sendMessage(TextComponent.toLegacyText(components));
        }
    }

    private static List<String> getPlayerThemeMessages(UUID playerUuid) {
        String theme = ThemeManager.players.getOrDefault(playerUuid, "default");
        return themeCache.computeIfAbsent(theme, (t) -> {
            @SuppressWarnings("unchecked")
            List<String> messages = (List<String>) ThemeManager.themes.getOrDefault(t, plugin.getConfig().getStringList("main.alerts"));
            return messages;
        });
    }

    private static List<String> getDefaultThemeMessages() {
        return themeCache.computeIfAbsent("default", (t) -> plugin.getConfig().getStringList("main.alerts"));
    }

    private static List<String> getDefaultThemeMessages(String reason) {
        return themeCache.computeIfAbsent("default", (t) -> plugin.getConfig().getStringList("main.alerts"));
    }

    private static List<String> processAlertMessages(List<String> messages, Player player, String suspendArg, int playerVl) {
        String unknown = LocaleManager.current_.getString("unknown", "unknown");
        return messages.stream().map((msg) -> {
            return msg.replace("%prefix%", ConfigManager.prefix)
                    .replace("%check%", suspendArg)
                    .replace("%player%", player.getName())
                    .replace("%vl%", String.valueOf(playerVl))
                    .replace("%reason%", unknown);
        }).collect(Collectors.toList());
    }

    private static List<String> processAlertMessages(List<String> messages, Player player, String suspendArg, int playerVl, String reason) {
        return messages.stream().map((msg) -> {
            return msg.replace("%prefix%", ConfigManager.prefix)
                    .replace("%check%", suspendArg)
                    .replace("%player%", player.getName())
                    .replace("%vl%", String.valueOf(playerVl))
                    .replace("%reason%", reason);
        }).collect(Collectors.toList());
    }

    public static BaseComponent[] toColoredComponent(String message) {
        return componentCache.computeIfAbsent(message, AlertsManager::createColoredComponent);
    }

    private static BaseComponent[] createColoredComponent(String message) {
        Matcher hexMatcher = HEX_PATTERN.matcher(message);
        List<BaseComponent> components = new ArrayList<>();
        int lastEnd = 0;

        ChatColor currentColor = ChatColor.WHITE;

        while (hexMatcher.find()) {
            if (hexMatcher.start() > lastEnd) {
                String text = message.substring(lastEnd, hexMatcher.start());
                ComponentState state = new ComponentState(currentColor);
                List<BaseComponent> parsedParts = parseLegacyColors(text, state);
                components.addAll(parsedParts);
                currentColor = state.color;
            }

            String hex = hexMatcher.group(1);
            currentColor = ChatColor.of("#" + hex);
            lastEnd = hexMatcher.end();
        }

        if (lastEnd < message.length()) {
            ComponentState state = new ComponentState(currentColor);
            components.addAll(parseLegacyColors(message.substring(lastEnd), state));
        }

        return components.toArray(new BaseComponent[0]);
    }

    private static List<BaseComponent> parseLegacyColors(String text, ComponentState state) {
        if (text.isEmpty()) {
            return Collections.emptyList();
        }

        List<BaseComponent> components = new ArrayList<>();
        String converted = ChatColor.translateAlternateColorCodes('&', text);

        StringBuilder currentText = new StringBuilder();

        for (int i = 0; i < converted.length(); i++) {
            char c = converted.charAt(i);

            if (c == '§' && i + 1 < converted.length()) {
                if (currentText.length() > 0) {
                    TextComponent comp = new TextComponent(currentText.toString());
                    applyFormatting(comp, state);
                    components.add(comp);
                    currentText.setLength(0);
                }

                char code = converted.charAt(i + 1);
                updateFormatting(state, code);
                i++;
            } else {
                currentText.append(c);
            }
        }

        if (currentText.length() > 0) {
            TextComponent comp = new TextComponent(currentText.toString());
            applyFormatting(comp, state);
            components.add(comp);
        }

        return components;
    }

    private static void updateFormatting(ComponentState state, char code) {
        switch (code) {
            case 'r':
            case 'R':
                state.color = ChatColor.WHITE;
                state.bold = false;
                state.italic = false;
                state.underlined = false;
                state.strikethrough = false;
                state.obfuscated = false;
                break;
            case 'l':
            case 'L':
                state.bold = true;
                break;
            case 'o':
            case 'O':
                state.italic = true;
                break;
            case 'n':
            case 'N':
                state.underlined = true;
                break;
            case 'm':
            case 'M':
                state.strikethrough = true;
                break;
            case 'k':
            case 'K':
                state.obfuscated = true;
                break;
            default:
                ChatColor color = ChatColor.getByChar(code);
                if (color != null) {
                    state.color = color;
                    state.bold = false;
                    state.italic = false;
                    state.underlined = false;
                    state.strikethrough = false;
                    state.obfuscated = false;
                }
                break;
        }
    }

    private static void applyFormatting(TextComponent comp, ComponentState state) {
        comp.setColor(state.color);
        comp.setBold(state.bold);
        comp.setItalic(state.italic);
        comp.setUnderlined(state.underlined);
        comp.setStrikethrough(state.strikethrough);
        comp.setObfuscated(state.obfuscated);
    }

    public static List<BaseComponent[]> parseHoverMessages(List<String> messages, Player player) {
        List<BaseComponent[]> parsedMessages = new ArrayList<>();

        for (String raw : messages) {
            TextComponent fullMessage = new TextComponent();
            processHoverText(raw, fullMessage, player);
            parsedMessages.add(new BaseComponent[]{fullMessage});
        }

        return parsedMessages;
    }

    private static void processHoverText(String raw, TextComponent fullMessage, Player player) {
        Matcher hoverMatcher = HOVER_PATTERN.matcher(raw);
        int lastEnd = 0;

        while (hoverMatcher.find()) {
            if (hoverMatcher.start() > lastEnd) {
                String remaining = raw.substring(lastEnd, hoverMatcher.start());
                addComponents(fullMessage, toColoredComponent(remaining));
            }

            String hoverText = hoverMatcher.group(1);
            TextComponent hoverComponent = createHoverComponent(hoverText, player);
            fullMessage.addExtra(hoverComponent);

            lastEnd = hoverMatcher.end();
        }

        if (lastEnd < raw.length()) {
            String remaining = raw.substring(lastEnd);
            addComponents(fullMessage, toColoredComponent(remaining));
        }
    }

    private static void addComponents(TextComponent target, BaseComponent[] components) {
        for (BaseComponent comp : components) {
            target.addExtra(comp);
        }
    }

    private static TextComponent createHoverComponent(String hoverText, Player player) {
        List<String> hoverLines = plugin.getConfig().getStringList("main.tab-complete");
        StringBuilder hoverBuilder = new StringBuilder();

        for (String line : hoverLines) {
            hoverBuilder.append(ColorUtil.setColorCodes(
                    line.replace("%ping%", String.valueOf(player.getPing()))
                            .replace("%player%", player.getName())
                            .replace("%tps%", String.valueOf(getCachedTps()))
                            .replace("%ip%", player.getAddress().getAddress().getHostAddress())
                            .replace("%ticking%", String.valueOf(player.isTicking()))
            )).append("\n");
        }

        TextComponent hoverComponent = new TextComponent(ColorUtil.setColorCodes(hoverText));
        hoverComponent.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
                new ComponentBuilder(hoverBuilder.toString().trim()).create()));
        return hoverComponent;
    }

    public static int addVlToPlayer(Player player, String keyArg) {
        String key = player.getUniqueId().toString() + keyArg;
        int newVl = vlMap.merge(key, 1, Integer::sum);
        lastUpdateTimeMap.put(key, System.currentTimeMillis());
        return newVl;
    }

    public static int getVlOfPlayer(Player player, String keyArg) {
        return vlMap.getOrDefault(player.getUniqueId().toString() + keyArg, 0);
    }

    private static double getCachedTps() {
        long now = System.currentTimeMillis();
        if (now - lastTpsUpdate > 1000L) {
            cachedTps = Math.round(Bukkit.getServer().getTPS()[0] * 100.0) / 100.0;
            lastTpsUpdate = now;
        }
        return cachedTps;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        String playerUuidString = playerUuid.toString();

        try {
            vlMap.entrySet().removeIf(entry -> entry.getKey().startsWith(playerUuidString));
            lastUpdateTimeMap.entrySet().removeIf(entry -> entry.getKey().startsWith(playerUuidString));
            suspendsMap.entrySet().removeIf(entry -> entry.getKey().startsWith(playerUuidString));
            lastMessageMap.remove(playerUuid);
            skippedAlertsMap.remove(playerUuid);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Ошибка в onPlayerQuit: " + e.getMessage());
        }
    }

    public static void shutdown() {
        shutdownExecutor(commandExecutor, "CommandExecutor");
        shutdownExecutor(cleanupExecutor, "CleanupExecutor");
        componentCache.clear();
        themeCache.clear();
    }

    private static void shutdownExecutor(ScheduledExecutorService executor, String name) {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();

            try {
                if (!executor.awaitTermination(5L, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    if (!executor.awaitTermination(5L, TimeUnit.SECONDS)) {
                        Bukkit.getLogger().warning("Не удалось корректно завершить " + name);
                    }
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public static int getActivePlayersCount() {
        return (int) vlMap.keySet().stream()
                .map(key -> key.substring(0, 36))
                .distinct()
                .count();
    }

    public static int getCommandQueueSize() {
        return commandQueue.size();
    }

    private static class CommandData {
        final String command;
        final String playerName;
        final long timestamp;

        CommandData(String command, String playerName) {
            this.command = command;
            this.playerName = playerName;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private static class ComponentState {
        ChatColor color;
        boolean bold;
        boolean italic;
        boolean underlined;
        boolean strikethrough;
        boolean obfuscated;

        ComponentState(ChatColor color) {
            this.color = color;
            this.bold = false;
            this.italic = false;
            this.underlined = false;
            this.strikethrough = false;
            this.obfuscated = false;
        }
    }
}