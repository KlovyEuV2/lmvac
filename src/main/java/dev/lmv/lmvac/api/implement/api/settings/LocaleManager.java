package dev.lmv.lmvac.api.implement.api.settings;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class LocaleManager {
    public static YamlConfiguration current_;
    public LocaleManager(Plugin plugin) {
        reload(plugin);
    }
    public static void reload(Plugin plugin) {
        String localeKey = plugin.getConfig().getString("locale","en_us");
        current_ = LocaleLoader.getLocale(localeKey);
    }
}
