package dev.lmv.lmvac.api.implement.api.npcs;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.stream.Collectors;

public class NPCNameManager {

    private final Plugin plugin;
    private final Random random = new Random();
    private List<String> configNames;

    public NPCNameManager(Plugin plugin) {
        this.plugin = plugin;
        loadConfigNames();
    }

    private void loadConfigNames() {
        String namesString = plugin.getConfig().getString("npc.names",
                "akvi4;MrDomer;MrZenyYT;KondrMs;bro9i;FlugerNew;grimac;grim;matrix;stint;t2x2");

        configNames = new ArrayList<>();
        if (namesString != null && !namesString.isEmpty()) {
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
            return getRandomNameFromConfig(player.getName());
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