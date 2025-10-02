package dev.lmv.lmvac.api.commands;

import dev.lmv.lmvac.api.ConfigManager;
import dev.lmv.lmvac.api.implement.checks.custom.CustomCheckManager;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.utils.text.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Команда для управления кастомными чеками
 * Использование: /lmvlua <reload|list|info|enable|disable|test|hotreload>
 */
public class CustomChecksCommand implements CommandExecutor, TabCompleter {
    private final CustomCheckManager checkManager;

    public CustomChecksCommand(CustomCheckManager checkManager) {
        this.checkManager = checkManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lmvac.admin")) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cУ вас нет прав!"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;

            case "list":
                handleList(sender);
                break;

            case "info":
                handleInfo(sender, args);
                break;

            case "enable":
                handleEnable(sender, args);
                break;

            case "disable":
                handleDisable(sender, args);
                break;

            case "test":
                handleTest(sender, args);
                break;

            case "hotreload":
                handleHotReload(sender);
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        sender.sendMessage(ColorUtil.setColorCodes(""));
        sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + " &eПерезагрузка кастомных чеков..."));

        long startTime = System.currentTimeMillis();

        try {
            checkManager.reloadAllChecks();
            long timeTaken = System.currentTimeMillis() - startTime;

            sender.sendMessage(ColorUtil.setColorCodes(" &b&l| &aКастомные чеки успешно перезагружены!"));
            sender.sendMessage(ColorUtil.setColorCodes(" &b&l| &7Загружено чеков: &e" + checkManager.getLoadedChecks().size()));
            sender.sendMessage(ColorUtil.setColorCodes(" &b&l| &7Время: &e" + timeTaken + "ms"));
        } catch (Exception e) {
            sender.sendMessage(ColorUtil.setColorCodes(" &b&l| &cОшибка при перезагрузке: " + e.getMessage()));
            e.printStackTrace();
        }
        sender.sendMessage(ColorUtil.setColorCodes(""));
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage(ColorUtil.setColorCodes(""));
        sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + " &7Список кастомных чеков:"));
        sender.sendMessage(ColorUtil.setColorCodes(""));

        if (checkManager.getLoadedChecks().isEmpty()) {
            sender.sendMessage(ColorUtil.setColorCodes(" &b&l| &7Нет загруженных кастомных чеков"));
            sender.sendMessage(ColorUtil.setColorCodes(" &b&l| &7Поместите .lua или .java файлы в:"));
            sender.sendMessage(ColorUtil.setColorCodes(" &b&l| &fplugins/LmvAC/checks/custom/lua/"));
            sender.sendMessage(ColorUtil.setColorCodes(" &b&l| &fplugins/LmvAC/checks/custom/java/"));
            sender.sendMessage(ColorUtil.setColorCodes(""));
            return;
        }

        for (Check check : checkManager.getLoadedChecks()) {
            String status = check.isEnabled() ? "&aʏ" : "&cx";
            String registered = check.isRegistered() ? "&aВключен" : "&7Выключен";
            String type = check.getClass().getSimpleName().contains("Lua") ? "&eLua" : "&bJava";

            sender.sendMessage(ColorUtil.setColorCodes(
                    " &b&l| " + status + " &f" + check.getName() + " &8[" + type + "&8] &8[" + registered + "&8]"));
        }

        sender.sendMessage(ColorUtil.setColorCodes(""));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cИспользование: /lmvlua info <название>"));
            return;
        }

        String checkName = args[1];
        Check check = checkManager.getLoadedChecks().stream()
                .filter(c -> c.getName().equalsIgnoreCase(checkName))
                .findFirst()
                .orElse(null);

        if (check == null) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cЧек не найден: &e" + checkName));
            sender.sendMessage(ColorUtil.setColorCodes(" &7Используйте &e/lmvlua list &7для списка"));
            return;
        }

        sender.sendMessage(ColorUtil.setColorCodes(""));
        sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + " &7Информация о чеке &6" + check.getName()));
        sender.sendMessage(ColorUtil.setColorCodes(""));
        sender.sendMessage(ColorUtil.setColorCodes(" &b&l| &7Тип: &f" +
                (check.getClass().getSimpleName().contains("Lua") ? "Lua" : "Java")));
        sender.sendMessage(ColorUtil.setColorCodes(" &b&l| &7Статус: " +
                (check.isEnabled() ? "&aВключен" : "&cВыключен")));
        sender.sendMessage(ColorUtil.setColorCodes(" &b&l| &7Зарегистрирован: " +
                (check.isRegistered() ? "&aДа" : "&cНет")));
        sender.sendMessage(ColorUtil.setColorCodes(" &b&l| &7Класс: &8" + check.getClass().getSimpleName()));
        sender.sendMessage(ColorUtil.setColorCodes(""));
    }

    private void handleEnable(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cИспользование: /lmvlua enable <название>"));
            return;
        }

        String checkName = args[1];
        Check check = checkManager.getCheck(checkName);

        if (check == null) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cЧек не найден: &e" + checkName));
            return;
        }

        if (check.isEnabled()) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&eЧек &6" + checkName + " &eуже включен!"));
            return;
        }

        try {
            check.register();
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&aЧек &e" + checkName + " &aуспешно включен!"));
        } catch (Exception e) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cОшибка при включении: " + e.getMessage()));
        }
    }

    private void handleDisable(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cИспользование: /lmvlua disable <название>"));
            return;
        }

        String checkName = args[1];
        Check check = checkManager.getCheck(checkName);

        if (check == null) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cЧек не найден: &e" + checkName));
            return;
        }

        if (!check.isEnabled()) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&eЧек &6" + checkName + " &eуже выключен!"));
            return;
        }

        try {
            check.unregister();
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&aЧек &e" + checkName + " &aуспешно выключен!"));
        } catch (Exception e) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cОшибка при выключении: " + e.getMessage()));
        }
    }

    private void handleTest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cЭта команда только для игроков!"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cИспользование: /lmvlua test <название>"));
            return;
        }

        Player player = (Player) sender;
        String checkName = args[1];
        Check check = checkManager.getCheck(checkName);

        if (check == null) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cЧек не найден: &e" + checkName));
            return;
        }

        sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&eТестирование чека &6" + checkName + "&e..."));

        try {
            check.flag(player, "Test flag from command");
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&aТестовый флаг отправлен!"));
        } catch (Exception e) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cОшибка: " + e.getMessage()));
        }
    }

    private void handleHotReload(CommandSender sender) {
        sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&eПроверка изменений в файлах..."));

        try {
            checkManager.checkForChanges();
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&aПроверка завершена!"));
        } catch (Exception e) {
            sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + "&cОшибка: " + e.getMessage()));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.setColorCodes(""));
        sender.sendMessage(ColorUtil.setColorCodes(ConfigManager.prefix + " &7Lua чеки - Помощь:"));
        sender.sendMessage(ColorUtil.setColorCodes(""));
        sender.sendMessage(ColorUtil.setColorCodes(" &6&l| "));
        sender.sendMessage(ColorUtil.setColorCodes(" &6&l&n|&r &7/lmvlua reload &8- &eПерезагрузить чеки"));
        sender.sendMessage(ColorUtil.setColorCodes(" &6&l&n|&r &7/lmvlua list &8- &eСписок чеков"));
        sender.sendMessage(ColorUtil.setColorCodes(" &6&l&n|&r &7/lmvlua info <чек> &8- &eИнформация"));
        sender.sendMessage(ColorUtil.setColorCodes(" &6&l&n|&r &7/lmvlua enable <чек> &8- &eВключить чек"));
        sender.sendMessage(ColorUtil.setColorCodes(" &6&l&n|&r &7/lmvlua disable <чек> &8- &eВыключить чек"));
        sender.sendMessage(ColorUtil.setColorCodes(" &6&l&n|&r &7/lmvlua test <чек> &8- &eТест чека"));
        sender.sendMessage(ColorUtil.setColorCodes(" &6&l&n|&r &7/lmvlua hotreload &8- &eПроверить изменения"));
        sender.sendMessage(ColorUtil.setColorCodes(" &6&l| "));
        sender.sendMessage(ColorUtil.setColorCodes(""));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("lmvac.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Arrays.asList("reload", "list", "info", "enable", "disable", "test", "hotreload")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("info") ||
                args[0].equalsIgnoreCase("enable") ||
                args[0].equalsIgnoreCase("disable") ||
                args[0].equalsIgnoreCase("test"))) {
            return checkManager.getLoadedChecks().stream()
                    .map(Check::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}