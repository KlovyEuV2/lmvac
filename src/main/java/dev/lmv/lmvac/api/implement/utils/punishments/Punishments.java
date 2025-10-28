package dev.lmv.lmvac.api.implement.utils.punishments;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class Punishments {
    private static Punishments instance;
    private final File file;
    private final FileConfiguration config;
    private static final ConcurrentHashMap<String,PunishmentEntry> entries = new ConcurrentHashMap<>();
//   private static final ConcurrentHashMap<String,Integer> maxVLong = new ConcurrentHashMap<>();

    private static final Pattern FORMAT_PATTERN = Pattern.compile("%([a-zA-Z0-9_]+)_\\{([^}]*)\\}%");
    private static final Pattern SET_VARIABLE_PATTERN = Pattern.compile("%([a-zA-Z0-9_]+)_([0-9]+\\.?[0-9]*)%");


    public Punishments(Plugin plugin) {
        this.file = new File(plugin.getDataFolder(), "punishments.yml");
        if (!this.file.exists()) {
            plugin.saveResource("punishments.yml", false);
        }

        this.config = YamlConfiguration.loadConfiguration(this.file);
        this.loadEntries();
        instance = this;
    }

    private void loadEntries() {

        for (String key : this.config.getKeys(false)) {
            List<String> checks = this.config.getStringList(key + ".checks");
            List<String> punishmentLines = this.config.getStringList(key + ".punishments");
            ConcurrentHashMap<Integer, String> punishmentMap = new ConcurrentHashMap<>();

            for (String line : punishmentLines) {
                String[] parts = line.split(";", 2);
                if (parts.length == 2) {
                    try {
                        int vl = Integer.parseInt(parts[0]);
                        punishmentMap.put(vl, parts[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            entries.put(key, new PunishmentEntry(checks, punishmentMap));
        }

    }

    public static double getValue(String check, int vl, String variablePattern, double defaultValue) {
        String varName = variablePattern.replace("$", "").replace("_{}", "").replace("{}", "");

        for (PunishmentEntry entry : entries.values()) {
            if (!entry.getChecks().contains(check)) {
                continue;
            }

            for (int i = vl; i >= 1; i--) {
                String punishment = entry.getPunishmentForVL(i);
                if (punishment != null) {
                    Pattern pattern = Pattern.compile("\\$" + varName + "_([0-9]+\\.?[0-9]*)\\$");
                    Matcher matcher = pattern.matcher(punishment);

                    if (matcher.find()) {
                        try {
                            return Double.parseDouble(matcher.group(1));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }

        return defaultValue;
    }

    public static String get(String check, int vl) {
        Iterator<PunishmentEntry> var2 = entries.values().iterator();

        PunishmentEntry entry;
        do {
            if (!var2.hasNext()) {
                return null;
            }

            entry = var2.next();
        } while(!entry.getChecks().contains(check));

        return entry.getPunishmentForVL(vl);
    }

    public static boolean getLower(String check, int vl, String command, int def) {
        Iterator<PunishmentEntry> var4 = entries.values().iterator();

        while(true) {
            PunishmentEntry entry;
            int i;
            String punishment;
            do {
                if (!var4.hasNext()) {
                    var4 = entries.values().iterator();

                    while(true) {
                        do {
                            if (!var4.hasNext()) {
                                return vl >= def;
                            }

                            entry = var4.next();
                        } while(!entry.getChecks().contains("default"));

                        for(i = 1; i <= vl; ++i) {
                            punishment = entry.getPunishmentForVL(i);
                            if (punishment != null && punishment.startsWith(command)) {
                                return true;
                            }
                        }
                    }
                }

                entry = var4.next();
            } while(!entry.getChecks().contains(check));

            for(i = 1; i <= vl; ++i) {
                punishment = entry.getPunishmentForVL(i);
                if (punishment != null && punishment.startsWith(command)) {
                    return true;
                }
            }
        }
    }

    public static Punishments getInstance() {
        return instance;
    }

    public void reload() {
        try {
            this.config.load(this.file);
        } catch (InvalidConfigurationException | IOException var2) {
            throw new RuntimeException(var2);
        }

        entries.clear();
        this.loadEntries();
    }

    private static class PunishmentEntry {
        private final List<String> checks;
        private final ConcurrentHashMap<Integer,String> punishments;

        public PunishmentEntry(List<String> checks, ConcurrentHashMap<Integer,String> punishments) {
            this.checks = checks;
            this.punishments = punishments;
        }

        public List<String> getChecks() {
            return this.checks;
        }

        public String getPunishmentForVL(int vl) {
            return this.punishments.get(vl);
        }
    }
}
