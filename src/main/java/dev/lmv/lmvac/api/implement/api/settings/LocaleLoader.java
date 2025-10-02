package dev.lmv.lmvac.api.implement.api.settings;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocaleLoader {

    private static final ConcurrentHashMap<String, YamlConfiguration> localeMap = new ConcurrentHashMap<>();
    private final Plugin plugin;

    public LocaleLoader(Plugin plugin) {
        this.plugin = plugin;
        loadLocales();
    }

    private void loadLocales() {
        File localesDir = new File(plugin.getDataFolder(), "locales");

        if (!localesDir.exists()) {
            localesDir.mkdirs();
        }

        copyDefaultLocales(localesDir);

        File[] files = localesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String localeKey = file.getName().substring(0, file.getName().length() - 4);
            try {
                YamlConfiguration config = new YamlConfiguration();
                config.load(file);
                localeMap.put(localeKey, config);
                System.out.println("[LocaleLoader] Loaded locale: " + localeKey);
            } catch (IOException | InvalidConfigurationException e) {
                System.err.println("[LocaleLoader] Failed to load locale " + localeKey);
                e.printStackTrace();
            }
        }
    }

    private void copyDefaultLocales(File localesDir) {
        try {
            String[] defaultLocales = {
                    "en_us.yml",
                    "ru_ru.yml"
            };

            for (String fileName : defaultLocales) {
                File targetFile = new File(localesDir, fileName);
                if (!targetFile.exists()) {
                    try (InputStream in = plugin.getResource("locales/" + fileName)) {
                        if (in == null) {
                            System.err.println("[LocaleLoader] Default locale resource not found: " + fileName);
                            continue;
                        }
                        Files.copy(in, targetFile.toPath());
                        System.out.println("[LocaleLoader] Copied default locale: " + fileName);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[LocaleLoader] Error copying default locales");
            e.printStackTrace();
        }
    }

    public static YamlConfiguration getLocale(String locale) {
        return localeMap.get(locale);
    }

    public static Map<String, YamlConfiguration> getAllLocales() {
        return localeMap;
    }

    public static boolean hasLocale(String locale) {
        return localeMap.containsKey(locale);
    }
}
