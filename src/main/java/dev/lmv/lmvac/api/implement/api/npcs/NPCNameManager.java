package dev.lmv.lmvac.api.implement.api.npcs;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.luaj.vm2.ast.Str;

import java.util.*;
import java.util.stream.Collectors;

public class NPCNameManager {

    private final Plugin plugin;
    private final Random random = new Random();
    private List<String> configNames;
    private List<ChanceMode> chances = new ArrayList<>();
    public static int max_name = 16;

    public static class ChanceMode {
        public RandomMode mode;
        public Double chance;
        public ChanceMode(RandomMode mode, Double chance) {
            this.mode = mode;
            this.chance = chance;
        }
    }

    public NPCNameManager(Plugin plugin) {
        this.plugin = plugin;
        loadConfigNames();
    }

    private void loadConfigNames() {
        ConfigurationSection randomSection = plugin.getConfig().getConfigurationSection("npc.random");
        ConfigurationSection chanceSection = randomSection.getConfigurationSection("chance");
        max_name = randomSection.getInt("max-name",16);

        if (chanceSection != null) {
            chances.clear();
            for (String key : chanceSection.getKeys(false)) {
                RandomMode mode = RandomMode.valueOf(String.valueOf(key).toUpperCase());
                double chance = chanceSection.getDouble(key);
                for (ChanceMode chanceMode : chances) {
                    if (chanceMode.mode.equals(mode)) {
                        chances.remove(chanceMode);
                        continue;
                    }
                }
                chances.add(new ChanceMode(mode,chance));
            }
        }

        String namesString = plugin.getConfig().getString("npc.names",
                "akvi4;MrDomer;MrZenyYT;KondrMs;bro9i;FlugerNew;grimac;grim;matrix;stint;t2x2");

        configNames = new ArrayList<>();
        if (!namesString.isEmpty()) {
            String[] nameArray = namesString.split(";");
            for (String name : nameArray) {
                String trimmedName = name.trim();
                if (!trimmedName.isEmpty()) {
                    configNames.add(trimmedName);
                }
            }
        }

        if (configNames.isEmpty()) {
            configNames.add("akvi4");
        }
    }

    public String getRandomName(Player player, RandomMode mode) {
        if (mode == RandomMode.CONFIG) {
            String name = getRandomNameFromConfig(player.getName());
            name = name.length() > 16 ? name.substring(0, 16) : name;
            return name;
        }

        if (mode == RandomMode.TAB) {
            Player randomPlayer = getRandomOnlinePlayer(player);
            if (randomPlayer != null
                    && !randomPlayer.equals(player)) {

                GameMode gm = randomPlayer.getGameMode();
                if (gm == GameMode.SURVIVAL || gm == GameMode.ADVENTURE) {
                    return randomPlayer.getName();
                }
            }
        }

        if (mode == RandomMode.RANDOM) {
            double chance = random.nextDouble() * 100.0;

            List<ChanceMode> sortedList = new ArrayList<>(chances);
            sortedList.sort(Comparator.comparingDouble(cMode -> cMode.chance));

            double cumulative = 0.0;
            ChanceMode selectedMode = new ChanceMode(RandomMode.TAB, 0.0);

            for (ChanceMode cMode : sortedList) {
                cumulative += cMode.chance;
                if (chance <= cumulative) {
                    selectedMode = cMode;
                    break;
                }
            }

            RandomMode randomMode = selectedMode.mode;

            if (randomMode.equals(RandomMode.TAB)) {
                Player randomPlayer = getRandomOnlinePlayer(player);
                if (randomPlayer != null && !randomPlayer.equals(player)) {
                    GameMode gm = randomPlayer.getGameMode();
                    if (gm == GameMode.SURVIVAL || gm == GameMode.ADVENTURE) {
                        return randomPlayer.getName();
                    }
                }
            } else if (randomMode.equals(RandomMode.CONFIG)) {
                String name = getRandomNameFromConfig(player.getName());
                name = name.length() > 16 ? name.substring(0, 16) : name;
                return name;
            } else {
                int randomID = random.nextInt(9000) + 1000;
                String name = "LmvAC_" + randomID;

                if (!configNames.isEmpty()) {
                    name = configNames.get(random.nextInt(configNames.size())) + randomID;
                }

                name = name.length() > 16 ? name.substring(0, 16) : name;
                return name;
            }
        }

        return getRandomNameFromConfig(player.getName());
    }

    private String getRandomNameFromConfig(String excludeName) {
        List<String> availableNames = new ArrayList<>();

        for (String configName : configNames) {
            if (!configName.equals(excludeName)) {
                availableNames.add(configName);
            }
        }

        if (availableNames.isEmpty()) {
            availableNames = new ArrayList<>(configNames);
        }

        return availableNames.get(random.nextInt(availableNames.size()));
    }

    private Player getRandomOnlinePlayer(Player excludedPlayer) {
        List<Player> candidates = Bukkit.getOnlinePlayers().stream()
                .filter(p -> {
                    GameMode gm = p.getGameMode();
                    return gm == GameMode.SURVIVAL || gm == GameMode.ADVENTURE;
                })
                .filter(p -> !p.equals(excludedPlayer))
                .filter(p -> excludedPlayer.canSee(p))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }


    public void reloadConfigNames() {
        loadConfigNames();
    }

    public List<String> getConfigNames() {
        return new ArrayList<>(configNames);
    }

    public void addNameToConfig(String name) {
        if (!configNames.contains(name)) {
            configNames.add(name);
            saveNamesToConfig();
        }
    }

    public void removeNameFromConfig(String name) {
        if (configNames.remove(name)) {
            if (configNames.isEmpty()) {
                configNames.add("akvi4");
            }
            saveNamesToConfig();
        }
    }

    private void saveNamesToConfig() {
        String namesString = String.join(";", configNames);
        plugin.getConfig().set("npc.names", namesString);
        plugin.saveConfig();
    }
}