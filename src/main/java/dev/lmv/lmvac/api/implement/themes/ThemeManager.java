package dev.lmv.lmvac.api.implement.themes;

import dev.lmv.lmvac.LmvAC;
import dev.lmv.lmvac.api.implement.utils.data.PlayerDataManager;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class ThemeManager implements Listener {
   public static ConcurrentHashMap<UUID,String> players = new ConcurrentHashMap<>();
   public static ConcurrentHashMap<String,List<String>> themes = new ConcurrentHashMap<>();
   private static File file;
   private static FileConfiguration config;

   public ThemeManager(Plugin plugin) {
      setup(plugin);
      loadThemes();
      loadOnlinePlayers();
      Bukkit.getPluginManager().registerEvents(this, plugin);
   }

   public static void setup(Plugin plugin) {
      file = new File(plugin.getDataFolder(), "themes.yml");
      if (!file.exists()) {
         try {
            plugin.saveResource("themes.yml", false);
         } catch (Exception var4) {
            try {
               file.createNewFile();
            } catch (IOException var3) {
               throw new RuntimeException(var3);
            }
         }
      }

      config = YamlConfiguration.loadConfiguration(file);
   }

   public static void setTheme(Player player, String name) {
      players.put(player.getUniqueId(), name);
      PlayerDataManager.saveTheme(player.getUniqueId(), name);
   }

   private static void loadOnlinePlayers() {
       for (Player player : Bukkit.getOnlinePlayers()) {
           String theme = PlayerDataManager.getTheme(player.getUniqueId());
           String validTheme = theme != null ? theme : "default";
           players.put(player.getUniqueId(), validTheme);
       }
   }

   private static void loadThemes() {
      themes.put("default", LmvAC.getInstance().getConfig().getStringList("main.alerts"));
      ConfigurationSection themeSection = config.getConfigurationSection("themes");
      if (themeSection != null) {
          for (String themeName : themeSection.getKeys(false)) {
              List<String> alerts = themeSection.getStringList(themeName + ".alerts");
              themes.put(themeName, alerts);
          }
      }

   }

   @EventHandler
   private void playerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      String theme = PlayerDataManager.getTheme(player.getUniqueId());
      String validTheme = theme != null ? theme : "default";
      players.put(player.getUniqueId(), validTheme);
   }

   public static void reload() {
      config = YamlConfiguration.loadConfiguration(file);
      themes.clear();
      loadThemes();
       for (Player player : Bukkit.getOnlinePlayers()) {
           String theme = PlayerDataManager.getTheme(player.getUniqueId());
           String validTheme = theme != null ? theme : "default";
           players.put(player.getUniqueId(), validTheme);
       }
   }

   @EventHandler
   private void playerQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      players.remove(player.getUniqueId());
   }
}
