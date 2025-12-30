package dev.lmv.lmvac.api.implement.ai.commands;

import dev.lmv.lmvac.LmvAC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LMVAICommand implements CommandExecutor, TabCompleter {

    private final LmvAC plugin;

    public LMVAICommand(LmvAC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("lmvac.use")) {
            sender.sendMessage("§cУ вас нет прав!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cТолько для игроков!");
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage("§cИспользование: /lmvai start [-cheat/-legit]");
                    return true;
                }

                Player player = (Player) sender;
                boolean isCheat;

                if (args[1].equalsIgnoreCase("-cheat")) {
                    isCheat = true;
                } else if (args[1].equalsIgnoreCase("-legit")) {
                    isCheat = false;
                } else {
                    sender.sendMessage("§cИспользуйте -cheat или -legit");
                    return true;
                }

                LmvAC.getAimDataCollector().startRecording(player, isCheat);
                break;

            case "stop":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cТолько для игроков!");
                    return true;
                }

                LmvAC.getAimDataCollector().stopRecording((Player) sender);
                break;

            case "train":
                if (!sender.hasPermission("lmvac.admin")) {
                    sender.sendMessage("§cУ вас нет прав!");
                    return true;
                }

                int sessionCount = LmvAC.getAimDataCollector().getCompletedSessionsCount();

                if (sessionCount == 0) {
                    sender.sendMessage("§c[LMVAC] Нет данных для обучения!");
                    sender.sendMessage("§7Сначала соберите данные командами:");
                    sender.sendMessage("§e  /lmvai start -cheat §7- запись с аимботом");
                    sender.sendMessage("§e  /lmvai start -legit §7- запись легитного игрока");
                    sender.sendMessage("§e  /lmvai stop §7- остановить запись");
                    return true;
                }

                sender.sendMessage("§a[LMVAC] Начинаю обучение модели аима...");
                sender.sendMessage("§7Это может занять время, следите за консолью");

                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    LmvAC.getAimAIDetector().trainModel();
                    LmvAC.getAimAIDetector().saveModel();

                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        sender.sendMessage("§a[LMVAC] ✓ Обучение завершено!");
                        sender.sendMessage("§bМодель аима активирована и готова детектить!");
                    });
                });
                break;

            case "info":
                sendInfo(sender);
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§e§l════ LMVAC Aim AI ════");
        sender.sendMessage("§b/lmvai start [-cheat/-legit] §7- Начать запись прицеливания");
        sender.sendMessage("§b/lmvai stop §7- Остановить запись");
        sender.sendMessage("§b/lmvai train §7- Обучить модель AI");
        sender.sendMessage("§b/lmvai info §7- Информация о системе");
        sender.sendMessage("§e§l═══════════════════════");
    }

    private void sendInfo(CommandSender sender) {
        int sessionCount = LmvAC.getAimDataCollector().getCompletedSessionsCount();
        boolean isTrained = LmvAC.getAimAIDetector().isTrained();

        sender.sendMessage("§e§l════ LMVAC Aim AI Info ════");
        sender.sendMessage("§7Версия: §av1.0.75F");
        sender.sendMessage("§7Сессий в памяти: §e" + sessionCount);
        sender.sendMessage("§7Модель обучена: " + (isTrained ? "§a✓" : "§c✗"));
        sender.sendMessage("§7Папка данных: §b/plugins/LmvAC/AI/aim_data/");
        sender.sendMessage("§e§l═══════════════════════════════");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("lmvac.use")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("start", "stop", "train", "info");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return Arrays.asList("-cheat", "-legit");
        }

        return new ArrayList<>();
    }
}