package dev.lmv.lmvac.api.implement.utils.data;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerDataManager {
   private static File file;
   private static FileConfiguration config;
   private static final Set<UUID> dirtyPlayers = new HashSet<>();
   private static JavaPlugin plugin;

   public static void setup(JavaPlugin pl) {
      plugin = pl;
      file = new File(plugin.getDataFolder(), "player-data.yml");
      if (!file.exists()) {
         try {
            file.createNewFile();
         } catch (IOException var2) {
            var2.printStackTrace();
         }
      }

      config = YamlConfiguration.loadConfiguration(file);
      startAutoSaveTask();
   }

   public static Map<String, Double> getAllSuspends(UUID uuid) {
      Map<String, Double> map = new HashMap();
      ConfigurationSection section = config.getConfigurationSection(uuid.toString());
      if (section != null) {

          for (String checkName : section.getKeys(false)) {
              double value = config.getDouble(String.valueOf(uuid) + "." + checkName + ".suspends", 0.0);
              map.put(checkName, value);
          }
      }

      return map;
   }

   public static void saveSuspends(UUID uuid, String suspendArg, double value) {
      config.set(String.valueOf(uuid) + "." + suspendArg + ".suspends", value);
      markDirty(uuid);
   }

   public static void saveTheme(UUID uuid, String name) {
      config.set(String.valueOf(uuid) + ".visual.theme", name);
      markDirty(uuid);
   }

   public static String getTheme(UUID uuid) {
      return config.getString(String.valueOf(uuid) + ".visual.theme");
   }

   public static double getSuspends(UUID uuid, String suspendArg) {
      return config.getDouble(String.valueOf(uuid) + "." + suspendArg + ".suspends", 0.0);
   }

   private static void markDirty(UUID uuid) {
      dirtyPlayers.add(uuid);
   }

   public static void flush() {
      if (!dirtyPlayers.isEmpty()) {
         try {
            config.save(file);
         } catch (IOException var1) {
            var1.printStackTrace();
         }

         dirtyPlayers.clear();
      }
   }

   private static void startAutoSaveTask() {
      Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, PlayerDataManager::flush, 600L, 600L);
   }
}
