
package dev.lmv.lmvac.api.commands;

import dev.lmv.lmvac.api.ConfigManager;
import dev.lmv.lmvac.api.implement.animations.DefaultAnimation;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.api.npcs.NpcManager;
import dev.lmv.lmvac.api.implement.api.packetListeners.InventoryListener;
import dev.lmv.lmvac.api.implement.api.settings.LocaleManager;
import dev.lmv.lmvac.api.implement.checks.other.CheckManager;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.modutils.AlertsManager;
import dev.lmv.lmvac.api.implement.themes.ThemeManager;
import dev.lmv.lmvac.api.implement.utils.data.PlayerDataManager;
import dev.lmv.lmvac.api.implement.utils.punishments.Punishments;
import dev.lmv.lmvac.api.implement.utils.text.ColorUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import dev.lmv.lmvac.api.modules.checks.aim.AimNpc;
import dev.lmv.lmvac.api.modules.checks.inventory.InventoryC;
import dev.lmv.lmvac.api.modules.checks.inventory.InventoryF;
import dev.lmv.lmvac.api.modules.checks.inventory.InventoryG;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static dev.lmv.lmvac.api.implement.api.LmvPlayer.players;

public class Main_Command implements CommandExecutor, TabCompleter {
    static Plugin plugin;
    static String name = "lmvac";

    public static ConcurrentHashMap<UUID, UUID> spectatorsUUIDS = new ConcurrentHashMap<UUID, UUID>();
    public static ConcurrentHashMap<UUID, Location> spectatorsLOCS = new ConcurrentHashMap<UUID, Location>();
    public static ConcurrentHashMap<UUID, GameMode> spectatorsGMS = new ConcurrentHashMap<UUID, GameMode>();

    public static ConcurrentHashMap<UUID, Boolean> doubleTgList = new ConcurrentHashMap<UUID, Boolean>();

    public Main_Command(JavaPlugin plugin) {
        Main_Command.plugin = plugin;
        PluginCommand command = Bukkit.getPluginCommand(name);
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length > 0) {
            switch (args[0]) {
                case "help":
                    if (handleHelp(sender, command, args)) {
                        return true;
                    }
                    sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cНе удалось загрузить список доступных комманд!"));
                    return false;
                case "theme":
                    if (handleTheme(sender, command, args)) {
                        return true;
                    }
                    sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cНе удалось использовать редактор тем!"));
                    return false;
                case "spec":
                    if (handleSpec(sender, command, args)) {
                        return true;
                    }
                    sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cНе удалось заспекать за игроком!"));
                    return false;
                case "reload":
                    if (handleReload(sender, command, args)) {
                        return true;
                    }
                    sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cНе удалось перезагрузить конфиг!"));
                    return false;
                case "profile":
                    if (handleProfile(sender, command, args)) {
                        return true;
                    }
                    sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cНе удалось получить профиль игрока!"));
                    return false;
                case "alerts":
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cКонсоль не может переключать уведомления ;( !"));
                        return true;
                    }
                    if (!sender.hasPermission("lmvac.alerts")) {
                        sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cУ вас нету прав на это!"));
                        return true;
                    }
                    Player player = (Player) sender;
                    try {
                        int id = player.getEntityId();
                        LmvPlayer client = (LmvPlayer)players.get(id);
                        if (client == null) {
                            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cНе удалось переключить уведомления! &f(Перезайдите)"));
                            return false;
                        }
                        client.alerts = !client.alerts;
                        if (!client.alerts) {
                            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cУведомления успешно выключены!"));
                        } else {
                            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&aУведомления успешно включены!"));
                        }
                        return true;
                    } catch (Exception ev) {
                        sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cНе удалось переключить уведомления!"));
                        return false;
                    }
                case "ban":
                    if (handleBan(sender, args)) {
                        return true;
                    }
                    sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cНе удалось забанить игрока!"));
                    return false;
                case "suspend":
                    if (handleSuspend(sender, args)) {
                        return true;
                    }
                    sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cНе удалось получить данные по проверке."));
                    return false;
                case "checks":
                    if (handleCheckList(sender, args)) {
                        return true;
                    }
                    sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cНе удалось получить список проверок."));
                    return false;
                case "npc":
                    if (handleNpcSpawn(sender, args)) {
                        return true;
                    }
                    sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cНе удалось заспавнить Анти-Чит бота."));
                    return false;
                default:
                    if (handleHelp(sender, command, args)) {
                        return true;
                    } else {
                        sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cНе удалось загрузить список доступных комманд!"));
                        return false;
                    }
            }
        } else if (handleHelp(sender, command, args)) {
            return true;
        } else {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cНе удалось загрузить список доступных комманд!"));
            return false;
        }
    }

    public static boolean handleNpcSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lmvac.npc.spawn")) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cУ вас нет прав на просмотр списка чеков!"));
            return false;
        } else {
            String targetName = args[1];
            String vanish = args[args.length-1];
            Player target = Bukkit.getPlayer(targetName);
            if (target == null || !target.isOnline()) {
                if (!vanish.equals("-s")) {
                    sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cИгрок не найден!"));
                }
                return false;
            }
            if (target.hasPermission("lmvac.npc.bypass")) {
                if (!vanish.equals("-s")) {
                    sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cВы не можете заспавнить Анти-Чит бота данному игроку!"));
                }
                return false;
            }
            String name = NpcManager.nameManager.getRandomName(target, NpcManager.randomMode);
            NpcManager.spawnNpcFor(target,name,NpcManager.createProfile(name,"",""));
            if (!vanish.equals("-s")) {
                sender.sendMessage(ColorUtil.setColorCodes(""));
                sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&aВы успешно заспавнили &bАнти-Чит бота &aигроку &f"+targetName));
                sender.sendMessage(ColorUtil.setColorCodes(""));
            }
            return true;
        }
    }

    public static boolean handleCheckList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lmvac.admin")) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cУ вас нет прав на просмотр списка чеков!"));
            return false;
        } else {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&8Список проверок античита : "));
            sender.sendMessage(ColorUtil.setColorCodes(""));

            for (Check check : CheckManager.getChecks()) {
                if (check.isEnabled()) {
                    sender.sendMessage(ColorUtil.setColorCodes(" &a" + check.getName()));
                }
            }

            for (Check check : CheckManager.getChecks()) {
                if (!check.isEnabled()) {
                    sender.sendMessage(ColorUtil.setColorCodes(" &8" + check.getName()));
                }
            }

            sender.sendMessage(ColorUtil.setColorCodes(""));
            return true;
        }
    }

    public static boolean handleBan(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lmvac.ban")) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cУ вас нет прав на бан игроков!"));
            return false;
        } else if (args.length < 3) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cИспользование: /lmvac ban <игрок> \"команда\""));
            return false;
        } else {
            String targetName = args[1];
            Player target = Bukkit.getPlayer(targetName);
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);

            if (target == null && !offlineTarget.hasPlayedBefore()) {
                sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cИгрок не найден!"));
                return false;
            } else if (target != null && target.hasPermission("lmvac.ban.bypass") && !target.equals(sender)) {
                sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cВы не можете забанить данного игрока!"));
                return false;
            } else if (target == null) {
                return false;
            } else {
                StringBuilder commandBuilder = new StringBuilder();

                for(int i = 2; i < args.length; ++i) {
                    commandBuilder.append(args[i]);
                    if (i < args.length - 1) {
                        commandBuilder.append(" ");
                    }
                }

                String command = commandBuilder.toString().trim();
                if ((command.startsWith("\"") && command.endsWith("\"")) ||
                        (command.startsWith("'") && command.endsWith("'"))) {
                    command = command.substring(1, command.length() - 1);
                }

                command = command.replace("%prefix%", ConfigManager.prefix).replace("%player%", targetName);

                try {
                    List<String> disabled = plugin.getConfig().getStringList("punish.blocked-commands");

                    for (String dis : disabled) {
                        if (command.startsWith(dis)) {
                            return false;
                        }
                    }

                    sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&7Выполняю команду: &f" + command));
                    List<String> commands = new ArrayList<String>();
                    commands.add(command);

                    final String finalCommand = command;
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            DefaultAnimation.play(target, commands);
                        }
                    });

                    sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&aКоманда успешно выполнена!"));
                    return true;
                } catch (Exception e) {
                    sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cОшибка выполнения команды!"));
                    plugin.getLogger().warning("Error executing ban command: " + command + " - " + e.getMessage());
                    return false;
                }
            }
        }
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<String>();

            if (sender.hasPermission("lmvac.help")) {
                completions.add("help");
            }
            if (sender.hasPermission("lmvac.theme")) {
                completions.add("theme");
            }
            if (sender.hasPermission("lmvac.spec")) {
                completions.add("spec");
            }
            if (sender.hasPermission("lmvac.npc.spawn")) {
                completions.add("npc");
            }
            if (sender.hasPermission("lmvac.alerts")) {
                completions.add("alerts");
            }
            if (sender.hasPermission("lmvac.profile")) {
                completions.add("profile");
            }
            if (sender.hasPermission("lmvac.suspend")) {
                completions.add("suspend");
            }
            if (sender.hasPermission("lmvac.ban")) {
                completions.add("ban");
            }
            if (sender.hasPermission("lmvac.admin")) {
                completions.add("checks");
            }
            if (sender.hasPermission("lmvac.reload")) {
                completions.add("reload");
            }

            String prefix = args[0].toLowerCase();
            return completions.stream()
                    .filter(s -> prefix.isEmpty() || s.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());

        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String prefix = args[1].toLowerCase();

            if (subCommand.equals("theme")) {
                List<String> suggestions = new ArrayList<String>();
                if (sender.hasPermission("lmvac.theme.set")) {
                    suggestions.add("set");
                }
                if (sender.hasPermission("lmvac.theme.reset")) {
                    suggestions.add("reset");
                }
                if (sender.hasPermission("lmvac.theme.get")) {
                    suggestions.add("get");
                }

                return suggestions.stream()
                        .filter(s -> prefix.isEmpty() || s.toLowerCase().startsWith(prefix))
                        .collect(Collectors.toList());
            }

            if ((subCommand.equals("ban") && sender.hasPermission("lmvac.ban")) ||
                    (subCommand.equals("spec") && sender.hasPermission("lmvac.spec")) ||
                    (subCommand.equals("profile") && sender.hasPermission("lmvac.profile")) ||
                    (subCommand.equals("suspend") && sender.hasPermission("lmvac.suspend")) ||
                    (subCommand.equals("npc") && sender.hasPermission("lmvac.npc.spawn"))) {

                return getFilteredPlayerNames(prefix, subCommand, sender);
            }

        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String secondArg = args[1].toLowerCase();
            String prefix = args[2].toLowerCase();

            if (subCommand.equals("theme") && secondArg.equals("set") && sender.hasPermission("lmvac.theme.set")) {
                return ThemeManager.themes.keySet().stream()
                        .filter(k -> prefix.isEmpty() || k.toLowerCase().startsWith(prefix))
                        .collect(Collectors.toList());
            }

            if (subCommand.equals("ban") && sender.hasPermission("lmvac.ban")) {
                List<String> suggestions = new ArrayList<String>();
                String suggestion1 = "\"kick %player% %prefix%читы\"";
                String suggestion2 = "\"ban %player% %prefix%читы\"";
                String suggestion3 = "\"tempban %player% 1d %prefix%читы\"";

                if (prefix.isEmpty() || suggestion1.toLowerCase().startsWith(prefix)) {
                    suggestions.add(suggestion1);
                }
                if (prefix.isEmpty() || suggestion2.toLowerCase().startsWith(prefix)) {
                    suggestions.add(suggestion2);
                }
                if (prefix.isEmpty() || suggestion3.toLowerCase().startsWith(prefix)) {
                    suggestions.add(suggestion3);
                }
                return suggestions;
            }

            if (subCommand.equals("suspend") && sender.hasPermission("lmvac.suspend")) {
                List<String> suggestions = new ArrayList<String>();

                suggestions.add("проверка");

                for (Check check : CheckManager.getChecks()) {
                    String checkName = check.getName().toLowerCase();
                    suggestions.add(checkName);
                }

                return suggestions.stream()
                        .filter(s -> prefix.isEmpty() || s.toLowerCase().startsWith(prefix))
                        .distinct()
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private List<String> getFilteredPlayerNames(String prefix, String subCommand, CommandSender sender) {
        List<String> playerNames = new ArrayList<String>();

        if (!(sender instanceof Player)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerName = player.getName();

                if (!prefix.isEmpty() && !playerName.toLowerCase().startsWith(prefix)) {
                    continue;
                }

                if (shouldSkipPlayer(player, subCommand, sender)) {
                    continue;
                }

                playerNames.add(playerName);
            }
        } else {
            Player senderPlayer = (Player) sender;

            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerName = player.getName();

                if (!prefix.isEmpty() && !playerName.toLowerCase().startsWith(prefix)) {
                    continue;
                }

                if (!senderPlayer.canSee(player)) {
                    continue;
                }

                if (shouldSkipPlayer(player, subCommand, sender)) {
                    continue;
                }

                playerNames.add(playerName);
            }
        }

        Collections.sort(playerNames, String.CASE_INSENSITIVE_ORDER);

        return playerNames;
    }

    private boolean shouldSkipPlayer(Player player, String subCommand, CommandSender sender) {
        switch (subCommand) {
            case "ban":
                return player.hasPermission("lmvac.ban.bypass") && !player.equals(sender);

            case "spec":
                return player.hasPermission("lmvac.spec.bypass") || player.equals(sender);

            case "profile":
                return player.hasPermission("lmvac.profile.bypass") && !player.equals(sender);

            case "npc":
                return player.hasPermission("lmvac.npc.bypass");

            default:
                return false;
        }
    }

    private static boolean handleSuspend(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lmvac.suspend")) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cУ вас нет прав на просмотр suspends!"));
            return false;
        } else if (args.length < 3) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cИспользование: /lmvac suspend <игрок> <проверка>"));
            return false;
        } else {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (target != null && target.hasPlayedBefore()) {
                UUID uuid = target.getUniqueId();
                String checkQuery = args[2].toLowerCase();
                Map<String, Double> suspends = PlayerDataManager.getAllSuspends(uuid);

                List<Map.Entry<String, Double>> matching = suspends.entrySet().stream()
                        .filter(entry -> entry.getKey().toLowerCase().contains(checkQuery))
                        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                        .collect(Collectors.toList());

                if (matching.isEmpty()) {
                    sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cНе найдено ни одной проверки с \"" + checkQuery + "\""));
                    return true;
                } else {
                    sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + " &7Проверки игрока &6" + target.getName() + " &7по запросу: &f" + checkQuery));

                    for (Map.Entry<String, Double> entry : matching) {
                        sender.sendMessage(ColorUtil.setColorCodes(" &8- &f" + entry.getKey() + ": &6" + String.format("%.8f", entry.getValue())));
                    }
                    return true;
                }
            } else {
                sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cИгрок не найден."));
                return false;
            }
        }
    }

    private static boolean handleTheme(CommandSender sender, Command command, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        } else {
            Player player = (Player)sender;
            if (!player.hasPermission("lmvac.theme")) {
                player.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cУ вас нет прав на использования редактора тем!"));
                return false;
            } else if (args.length < 2) {
                if (player.hasPermission("lmvac.theme")) {
                    player.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&f/lmvac theme &8- &7Данное сообщение&8(help)."));
                }
                if (player.hasPermission("lmvac.theme.set")) {
                    player.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&f/lmvac theme set &8- &7Установить тему&8(set)."));
                }
                if (player.hasPermission("lmvac.theme.reset")) {
                    player.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&f/lmvac theme reset &8- &7Сбросить тему&8(reset)."));
                }
                if (player.hasPermission("lmvac.theme.get")) {
                    player.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&f/lmvac theme get &8- &7Получить текущую тему&8(get)."));
                }
                return true;
            } else {
                switch (args[1]) {
                    case "set":
                        if (!player.hasPermission("lmvac.theme.set")) {
                            player.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cУ вас нет прав на смену темы!"));
                            return false;
                        } else if (args.length < 3) {
                            player.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cИспользование: &4/lmvac theme set <название>"));
                            return false;
                        } else {
                            String themeName = args[2].toLowerCase();
                            if (!ThemeManager.themes.containsKey(themeName)) {
                                player.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cТема &b\"" + themeName + "\" &cне найдена!"));
                                return false;
                            }

                            ThemeManager.setTheme(player, themeName);
                            player.sendMessage("");
                            sender.sendMessage(ColorUtil.setColorCodes("&aВы установили тему: &a" + themeName));

                            @SuppressWarnings("unchecked")
                            List<String> messages = (List<String>) ThemeManager.themes.getOrDefault(themeName, plugin.getConfig().getStringList("main.alerts"));

                            List<String> replacedMessages = messages.stream().map(msg -> {
                                return msg.replace("%player%", player.getName())
                                        .replace("%prefix%", ConfigManager.prefix)
                                        .replace("%check%", "BadPacketsB")
                                        .replace("%vl%", String.valueOf((int)(Math.random() * 100.0)));
                            }).collect(Collectors.toList());

                            sender.sendMessage(ColorUtil.setColorCodes("&8пример: "));
                            List<String> coloredMessages = new ArrayList<>();
                            for (String message : replacedMessages) {
                                coloredMessages.add(ChatColor.translateAlternateColorCodes('&', message));
                            }

                            List<BaseComponent[]> hoverMessages = AlertsManager.parseHoverMessages(coloredMessages, player);

                            for (BaseComponent[] components : hoverMessages) {
                                player.spigot().sendMessage(components);
                            }
                            player.sendMessage("");
                            return true;
                        }
                    case "reset":
                        if (!player.hasPermission("lmvac.theme.reset")) {
                            player.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cУ вас нет прав на сброс темы!"));
                            return false;
                        }

                        ThemeManager.setTheme(player, "default");
                        player.sendMessage("");
                        sender.sendMessage(ColorUtil.setColorCodes("&aВы сбросили тему на: &adefault"));

                        @SuppressWarnings("unchecked")
                        List<String> messages = (List<String>) ThemeManager.themes.getOrDefault("default", plugin.getConfig().getStringList("main.alerts"));
                        messages = messages.stream().map(msg -> {
                            return msg.replace("%player%", player.getName())
                                    .replace("%prefix%", ConfigManager.prefix)
                                    .replace("%check%", "BadPacketsB")
                                    .replace("%vl%", String.valueOf((int)(Math.random() * 100.0)));
                        }).collect(Collectors.toList());

                        sender.sendMessage(ColorUtil.setColorCodes("&8пример: "));
                        List<String> coloredMessages = new ArrayList<>();
                        for (String message : messages) {
                            coloredMessages.add(ChatColor.translateAlternateColorCodes('&', message));
                        }

                        List<BaseComponent[]> replacedMessages = AlertsManager.parseHoverMessages(coloredMessages, player);

                        for (BaseComponent[] components : replacedMessages) {
                            player.spigot().sendMessage(components);
                        }
                        player.sendMessage("");
                        return true;
                    case "get":
                        if (!player.hasPermission("lmvac.theme.get")) {
                            player.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cУ вас нет прав на просмотр темы!"));
                            return false;
                        }

                        String themeName = ThemeManager.players.getOrDefault(player.getUniqueId(), "default");
                        @SuppressWarnings("unchecked")
                        List<String> themeMessages = (List<String>) ThemeManager.themes.getOrDefault(themeName, plugin.getConfig().getStringList("main.alerts"));

                        List<String> processedMessages = themeMessages.stream().map(msg -> {
                            return msg.replace("%player%", player.getName())
                                    .replace("%prefix%", ConfigManager.prefix)
                                    .replace("%check%", "BadPacketsB")
                                    .replace("%vl%", String.valueOf((int)(Math.random() * 100.0)));
                        }).collect(Collectors.toList());

                        player.sendMessage("");
                        sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&8Пример вашей текущей темы: "));
                        List<String> coloredMessages2 = processedMessages.stream()
                                .map(message -> ChatColor.translateAlternateColorCodes('&', message))
                                .collect(Collectors.toList());

                        List<BaseComponent[]> hoverMessages = AlertsManager.parseHoverMessages(coloredMessages2, player);

                        for (BaseComponent[] components : hoverMessages) {
                            player.spigot().sendMessage(components);
                        }
                        player.sendMessage("");
                        return true;
                    default:
                        if (player.hasPermission("lmvac.theme")) {
                            player.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&f/lmvac theme &8- &7Данное сообщение&8(help)."));
                        }
                        if (player.hasPermission("lmvac.theme.set")) {
                            player.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&f/lmvac theme set &8- &7Установить тему&8(set)."));
                        }
                        if (player.hasPermission("lmvac.theme.reset")) {
                            player.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&f/lmvac theme reset &8- &7Сбросить тему&8(reset)."));
                        }
                        if (player.hasPermission("lmvac.theme.get")) {
                            player.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&f/lmvac theme get &8- &7Получить текущую тему&8(get)."));
                        }
                        return true;
                }
            }
        }
    }

    private static boolean handleProfile(final CommandSender sender, Command command, String[] args) {
        if (!sender.hasPermission("lmvac.profile")) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cУ вас нет прав на просмотр профиля игроков!"));
            return false;
        } else if (args.length < 2) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cВы не указали имя игрока!"));
            return false;
        } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null && target.hasPermission("lmvac.profile.bypass") && target != sender) {
                sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cВы не можете посмотреть профиль данного игрока!"));
                return false;
            } else {
                if (target == null) {
                    sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cИгрок не найден или не в сети, пытаемся получить инфо..."));
                    OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
                    if (offline.getLastLogin() <= 0L) {
                        sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cОшибка, возможно данный игрок не заходил на сервер!"));
                        return false;
                    } else {
                        UUID uuid = offline.getUniqueId();
                        String brand = "&8# Неизвестно(Offline)";
                        sender.sendMessage(ColorUtil.setColorCodes(""));
                        sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + " &7Профиль игрока &6" + offline.getName()));
                        sender.sendMessage(ColorUtil.setColorCodes(" &b&l| &7Клиент: &f" + brand));

                        Map<String, Double> suspends = PlayerDataManager.getAllSuspends(uuid);
                        if (suspends.isEmpty()) {
                            sender.sendMessage(ColorUtil.setColorCodes(" &b&l| &7Жалобы: &8нет данных"));
                            return false;
                        } else {
                            sender.sendMessage(ColorUtil.setColorCodes(" &b&l| &7Топ-8 жалоб:"));

                            final AtomicInteger index = new AtomicInteger(0);
                            suspends.entrySet().stream()
                                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                                    .limit(8L)
                                    .forEach(entry -> {
                                        int idx = index.incrementAndGet();
                                        String color;
                                        switch (idx) {
                                            case 1:
                                                color = "&4";
                                                break;
                                            case 2:
                                                color = "&c";
                                                break;
                                            case 3:
                                                color = "&6";
                                                break;
                                            case 4:
                                            case 5:
                                                color = "&e";
                                                break;
                                            default:
                                                color = "&7";
                                        }
                                        sender.sendMessage(ColorUtil.setColorCodes("   &8- " + color + entry.getKey() + "&7: &f" + String.format("%.3f", entry.getValue())));
                                    });
                            return true;
                        }
                    }
                } else {
                    UUID uuid = target.getUniqueId();
                    String brand = getClientBrand(target);
                    sender.sendMessage(ColorUtil.setColorCodes(""));
                    sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + " &7Профиль игрока &6" + target.getName()));
                    sender.sendMessage(ColorUtil.setColorCodes(" &b&l| &7Клиент: &f" + brand));

                    Map<String, Double> suspends = PlayerDataManager.getAllSuspends(uuid);
                    if (suspends.isEmpty()) {
                        sender.sendMessage(ColorUtil.setColorCodes(" &b&l| &7Жалобы: &8нет данных"));
                    } else {
                        sender.sendMessage(ColorUtil.setColorCodes(" &b&l| &7Топ-8 жалоб:"));

                        final AtomicInteger index = new AtomicInteger(0);
                        suspends.entrySet().stream()
                                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                                .limit(8L)
                                .forEach(entry -> {
                                    int idx = index.incrementAndGet();
                                    String color;
                                    switch (idx) {
                                        case 1:
                                            color = "&4";
                                            break;
                                        case 2:
                                            color = "&c";
                                            break;
                                        case 3:
                                            color = "&6";
                                            break;
                                        case 4:
                                        case 5:
                                            color = "&e";
                                            break;
                                        default:
                                            color = "&7";
                                    }
                                    sender.sendMessage(ColorUtil.setColorCodes("   &8- " + color + entry.getKey() + "&7: &f" + String.format("%.3f", entry.getValue())));
                                });
                    }
                    int id = target.getEntityId();
                    LmvPlayer client = (LmvPlayer)LmvPlayer.players.get(id);
                    if (client != null) {
                        sender.sendMessage(client.clientSettings.toString());
                    }

                    sender.sendMessage("");
                    return true;
                }
            }
        }
    }

   public static String getClientBrand(Player player) {
      try {
         String brand = player.getClientBrandName();
         return brand != null && !brand.trim().isEmpty() ? brand : "&8#неизвестно";
      } catch (Exception var2) {
         return "&8#неизвестно";
      }
   }

   private static boolean handleTGSet(CommandSender sender, Command command, String[] args) {
      return true;
   }

   private static boolean handleHelp(CommandSender sender, Command command, String[] args) {
      if (!sender.hasPermission("lmvac.help")) {
         sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cУ вас нет прав на просмотр доступных комманд!"));
         return false;
      } else {
         sender.sendMessage("");
         sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + " &7Список доступных комманд:"));
         sender.sendMessage(ColorUtil.setColorCodes(""));
         sender.sendMessage(ColorUtil.setColorCodes(" &6&l| "));
         if (sender.hasPermission("lmvac.help")) {
            sender.sendMessage(ColorUtil.setColorCodes(" &6&l&n|&r &7/" + command.getName() + " help &8- &eПоказать данную помощь"));
         }

          if (sender.hasPermission("lmvac.alerts")) {
              sender.sendMessage(ColorUtil.setColorCodes(" &6&l&n|&r &7/" + command.getName() + " alerts &8- &eПереключить жалобы анти-чита"));
          }

         if (sender.hasPermission("lmvac.spec")) {
            sender.sendMessage(ColorUtil.setColorCodes(" &6&l&n|&r &7/" + command.getName() + " spec <игрок> &8- &eПроследить за игроком"));
         }

         if (sender.hasPermission("lmvac.profile")) {
            sender.sendMessage(ColorUtil.setColorCodes(" &6&l&n|&r &7/" + command.getName() + " profile <игрок> &8- &eПосмотреть профиль игрока"));
         }

         if (sender.hasPermission("lmvac.suspend")) {
            sender.sendMessage(ColorUtil.setColorCodes(" &6&l&n|&r &7/" + command.getName() + " suspend <игрок> <проверка> &8- &eПосмотреть инфо о нужном чеке игрока"));
         }

          if (sender.hasPermission("lmvac.npc.spawn")) {
              sender.sendMessage(ColorUtil.setColorCodes(" &6&l&n|&r &7/" + command.getName() + " npc <игрок> &8- &eЗаспавнить игроку анти-чит бота"));
          }

         if (sender.hasPermission("lmvac.ban")) {
            sender.sendMessage(ColorUtil.setColorCodes(" &6&l&n|&r &7/" + command.getName() + " ban <игрок> \"команда\" &8- &eЗабанить игрока"));
         }

          if (sender.hasPermission("lmvac.admin")) {
              sender.sendMessage(ColorUtil.setColorCodes(" &6&l&n|&r &7/" + command.getName() + " checks &8- &eУзнать список и информацию о проверках"));
          }

         if (sender.hasPermission("lmvac.reload")) {
            sender.sendMessage(ColorUtil.setColorCodes(" &6&l&n|&r &7/" + command.getName() + " reload &8- &eПерезагрузить конфиг"));
         }

         sender.sendMessage(ColorUtil.setColorCodes(" &6&l| "));
         sender.sendMessage("");
         return true;
      }
   }

   private static boolean handleSpec(CommandSender sender, Command command, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cКонсоль не может спекать за игроками!"));
         return true;
      } else {
         Player senderPlr = (Player)sender;
         UUID senderUUID = senderPlr.getUniqueId();
         if (!sender.hasPermission("lmvac.spec")) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cУ вас нет прав на спек за игроками!"));
            return true;
         } else if (!spectatorsUUIDS.containsKey(senderUUID)) {
            if (args.length < 2) {
               sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cВы не указали ник игрока для наблюдения."));
               return true;
            } else {
               Player target = Bukkit.getPlayer(args[1]);
               if (target != null && target.isOnline()) {
                  if (!target.hasPermission("lmvac.spec.bypass") && !senderPlr.equals(target)) {
                     spectatorsUUIDS.put(senderUUID, target.getUniqueId());
                     spectatorsLOCS.put(senderUUID, senderPlr.getLocation());
                     spectatorsGMS.put(senderUUID, senderPlr.getGameMode());
                     senderPlr.setGameMode(GameMode.SPECTATOR);
                     senderPlr.teleport(target);
                     Iterator var10 = Bukkit.getOnlinePlayers().iterator();

                     while(var10.hasNext()) {
                        Player online = (Player)var10.next();
                        if (!online.equals(senderPlr) && !online.hasPermission("lmvac.spec.view")) {
                           online.hidePlayer(plugin, senderPlr);
                        }
                     }

                     String var10001 = ConfigManager.prefix;
                     sender.sendMessage(ColorUtil.setColorCodes(var10001 + "&bВы успешно начали спекать за &f" + target.getName() + "&b!"));
                     return true;
                  } else {
                     sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cВы не можете проспекать за данным игроком!"));
                     return true;
                  }
               } else {
                  sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cИгрок не найден или не в сети!"));
                  return true;
               }
            }
         } else {
            Location returnLoc = (Location)spectatorsLOCS.get(senderUUID);
            GameMode returnGM = (GameMode)spectatorsGMS.get(senderUUID);
            senderPlr.teleport(returnLoc);
            senderPlr.setGameMode(returnGM);

             for (Player online : Bukkit.getOnlinePlayers()) {
                 online.showPlayer(plugin, senderPlr);
             }

            spectatorsUUIDS.remove(senderUUID);
            spectatorsLOCS.remove(senderUUID);
            spectatorsGMS.remove(senderUUID);
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&aВы прекратили спекать за игроком."));
            return true;
         }
      }
   }

   private static boolean handleReload(CommandSender sender, Command command, String[] args) {
      if (!sender.hasPermission("lmvac.reload")) {
         sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cУ вас нет прав на перезагрузку конфига!"));
         return false;
      } else {
         plugin.reloadConfig();
         reloadAPI();
         sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&aКонфиг успешно перезагружен!"));
         return true;
      }
   }

   private static void reloadAPI() {
       LocaleManager.reload(plugin);
       ConfigManager.reloadConfig(plugin.getConfig());
       Punishments.getInstance().reload();
       ThemeManager.reload();
       NpcManager.nameManager.reloadConfigNames();
       AimNpc.resetMode(plugin);
       NpcManager.loadSettings(plugin);
       InventoryListener.reload();
       InventoryF.reloadCfg(plugin);
       InventoryC.reloadCfg(plugin);
       InventoryG.reloadCfg(plugin);
       LmvPlayer.reload();
   }
}
