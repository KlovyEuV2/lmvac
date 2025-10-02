package dev.lmv.lmvac.api;

import dev.lmv.lmvac.api.implement.checks.other.CheckManager;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManager {
   private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
   static Plugin plugin;
   public static Configuration lastConfig;
   public static String prefix;
   public static Long toVlRemove;
   private static CheckManager checkHandler;

   public static boolean advertising = false;

   public ConfigManager(Plugin plugin) {
      try {
         ConfigManager.plugin = plugin;
         lastConfig = plugin.getConfig();
         reloadConfig(lastConfig);
      } catch (Exception var3) {
         Bukkit.getLogger().warning(prefix + "§cОшибка при загрузке конфиг-менеджера!");
      }

   }

   public static void setCheckManager(CheckManager manager) {
      checkHandler = manager;
   }

   public static Boolean reloadConfig(Configuration config) {
      try {
         lastConfig = config;
         lastConfig = plugin.getConfig();
         prefix = getString(lastConfig, "main-prefix", "§7[LMVAC] ");
         toVlRemove = getLong(lastConfig, "vl-remove-time", 5000L);
         advertising = Boolean.TRUE.equals(getBoolean(lastConfig, "notification.advertising", false));
         if (checkHandler != null) {
            checkHandler.reloadChecks();
         } else {
            log.warn("CheckManager не установлен, перезагрузка чеков пропущена.");
         }
      } catch (Exception var2) {
         log.error("Ошибка при перезагрузке конфига", var2);
         return false;
      }

      return true;
   }

   public static String getString(Configuration config, String path) {
      return config.getString(path);
   }

   public static Double getDouble(Configuration config, String path) {
      return config.getDouble(path);
   }

   public static Integer getInteger(Configuration config, String path) {
      return config.getInt(path);
   }

   public static Boolean getBoolean(Configuration config, String path) {
      return config.getBoolean(path);
   }

   public static Long getLong(Configuration config, String path) {
      return config.getLong(path);
   }

   public static List getList(Configuration config, String path) {
      return config.getList(path);
   }

   public static String getString(Configuration config, String path, Object def) {
      String value = config.getString(path);
      return value != null ? value : (def != null ? def.toString() : null);
   }

   public static Double getDouble(Configuration config, String path, Object def) {
      if (config.contains(path)) {
         return config.getDouble(path);
      } else {
         return def instanceof Number ? ((Number)def).doubleValue() : null;
      }
   }

   public static Integer getInteger(Configuration config, String path, Object def) {
      if (config.contains(path)) {
         return config.getInt(path);
      } else {
         return def instanceof Number ? ((Number)def).intValue() : null;
      }
   }

   public static Boolean getBoolean(Configuration config, String path, Object def) {
      if (config.contains(path)) {
         return config.getBoolean(path);
      } else {
         return def instanceof Boolean ? (Boolean)def : null;
      }
   }

   public static Long getLong(Configuration config, String path, Object def) {
      if (config.contains(path)) {
         return config.getLong(path);
      } else {
         return def instanceof Number ? ((Number)def).longValue() : null;
      }
   }

   public static List getList(Configuration config, String path, Object def) {
      if (config.contains(path)) {
         return Collections.singletonList(config.getList(path));
      } else {
         return def instanceof List ? (List)def : null;
      }
   }
}
