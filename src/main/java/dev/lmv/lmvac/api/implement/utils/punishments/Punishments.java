package dev.lmv.lmvac.api.implement.utils.punishments;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    private static final ConcurrentHashMap<String, PunishmentEntry> entries = new ConcurrentHashMap<>();

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
            ConcurrentHashMap<Integer, List<String>> punishmentMap = new ConcurrentHashMap<>();

            for (String line : punishmentLines) {
                String[] parts = line.split(";", 2);
                if (parts.length == 2) {
                    try {
                        int vl = Integer.parseInt(parts[0]);
                        String action = parts[1];
                        punishmentMap.computeIfAbsent(vl, k -> new ArrayList<>()).add(action);
                    } catch (NumberFormatException ignored) {}
                }
            }

            entries.put(key, new PunishmentEntry(checks, punishmentMap));
        }
    }

    private static boolean matchesCheck(List<String> checks, String targetCheck) {
        boolean matched = false;
        for (String check : checks) {
            if (check.startsWith("!")) {
                String neg = check.substring(1);
                if (neg.endsWith("^")) {
                    String prefix = neg.substring(0, neg.length() - 1);
                    if (targetCheck.startsWith(prefix)) return false;
                } else {
                    if (targetCheck.equals(neg)) return false;
                }
            } else if (check.endsWith("^")) {
                String prefix = check.substring(0, check.length() - 1);
                if (targetCheck.startsWith(prefix)) matched = true;
            } else {
                if (targetCheck.equals(check)) matched = true;
            }
        }
        return matched;
    }

    private PunishmentEntry getMatchingEntry(String check) {
        for (PunishmentEntry entry : entries.values()) {
            if (matchesCheck(entry.getChecks(), check)) {
                return entry;
            }
        }
        return null;
    }

    public static long getValueLong(String check, int vl, String variablePattern, long defaultValue) {
        String varName = variablePattern.replace("$", "").replace("_{}", "").replace("{}", "");
        PunishmentEntry entry = getInstance().getMatchingEntry(check);
        if (entry != null) {
            for (int i = vl; i >= 1; i--) {
                List<String> punishments = entry.getPunishmentsForVL(i);
                if (punishments != null) {
                    Pattern pattern = Pattern.compile("\\$" + varName + "_([0-9]+\\.?[0-9]*)\\$");
                    for (String punishment : punishments) {
                        Matcher matcher = pattern.matcher(punishment);
                        if (matcher.find()) {
                            try {
                                double parsed = Double.parseDouble(matcher.group(1));
                                return (long) parsed;
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        }
        return defaultValue;
    }

    public static int getValueInt(String check, int vl, String variablePattern, int defaultValue) {
        String varName = variablePattern.replace("$", "").replace("_{}", "").replace("{}", "");
        PunishmentEntry entry = getInstance().getMatchingEntry(check);
        if (entry != null) {
            for (int i = vl; i >= 1; i--) {
                List<String> punishments = entry.getPunishmentsForVL(i);
                if (punishments != null) {
                    Pattern pattern = Pattern.compile("\\$" + varName + "_([0-9]+\\.?[0-9]*)\\$");
                    for (String punishment : punishments) {
                        Matcher matcher = pattern.matcher(punishment);
                        if (matcher.find()) {
                            try {
                                double parsed = Double.parseDouble(matcher.group(1));
                                return (int) parsed;
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        }
        return defaultValue;
    }

    public static double getValue(String check, int vl, String variablePattern, double defaultValue) {
        String varName = variablePattern.replace("$", "").replace("_{}", "").replace("{}", "");
        PunishmentEntry entry = getInstance().getMatchingEntry(check);
        if (entry != null) {
            for (int i = vl; i >= 1; i--) {
                List<String> punishments = entry.getPunishmentsForVL(i);
                if (punishments != null) {
                    Pattern pattern = Pattern.compile("\\$" + varName + "_([0-9]+\\.?[0-9]*)\\$");
                    for (String punishment : punishments) {
                        Matcher matcher = pattern.matcher(punishment);
                        if (matcher.find()) {
                            try {
                                return Double.parseDouble(matcher.group(1));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        }
        return defaultValue;
    }

    public static List<String> getAll(String check, int vl) {
        PunishmentEntry entry = getInstance().getMatchingEntry(check);
        if (entry != null) return entry.getPunishmentsForVL(vl);
        return null;
    }

    public static String get(String check, int vl) {
        List<String> punishments = getAll(check, vl);
        return (punishments != null && !punishments.isEmpty()) ? punishments.get(0) : null;
    }

    public static boolean getLower(String check, int vl, String command, int def) {
        for (PunishmentEntry entry : entries.values()) {
            if (matchesCheck(entry.getChecks(), check)) {
                for (int i = 1; i <= vl; i++) {
                    List<String> punishments = entry.getPunishmentsForVL(i);
                    if (punishments != null) {
                        for (String punishment : punishments) {
                            if (punishment != null && punishment.startsWith(command)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        for (PunishmentEntry entry : entries.values()) {
            if (matchesCheck(entry.getChecks(), "default")) {
                for (int i = 1; i <= vl; i++) {
                    List<String> punishments = entry.getPunishmentsForVL(i);
                    if (punishments != null) {
                        for (String punishment : punishments) {
                            if (punishment != null && punishment.startsWith(command)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return vl >= def;
    }

    public static Punishments getInstance() {
        return instance;
    }

    public void reload() {
        try {
            this.config.load(this.file);
        } catch (InvalidConfigurationException | IOException e) {
            throw new RuntimeException(e);
        }
        entries.clear();
        this.loadEntries();
    }

    private static class PunishmentEntry {
        private final List<String> checks;
        private final ConcurrentHashMap<Integer, List<String>> punishments;

        public PunishmentEntry(List<String> checks, ConcurrentHashMap<Integer, List<String>> punishments) {
            this.checks = checks;
            this.punishments = punishments;
        }

        public List<String> getChecks() {
            return this.checks;
        }

        public List<String> getPunishmentsForVL(int vl) {
            return this.punishments.get(vl);
        }
    }
}
