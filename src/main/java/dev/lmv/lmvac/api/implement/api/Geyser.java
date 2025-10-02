package dev.lmv.lmvac.api.implement.api;

import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Geyser {
   private static Method geyserApiMethod = null;
   private static Method isBedrockPlayerMethod = null;
   private static boolean reflectionInitialized = false;

   public static boolean isBedrockPlayer(UUID uuid) {
      if (Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null && Bukkit.getPluginManager().getPlugin("Geyser-Spigot").isEnabled()) {
         if (!reflectionInitialized) {
            initializeReflection();
         }

         if (geyserApiMethod != null && isBedrockPlayerMethod != null) {
            try {
               Object geyserApiInstance = geyserApiMethod.invoke((Object)null);
               return (Boolean)isBedrockPlayerMethod.invoke(geyserApiInstance, uuid);
            } catch (Exception var2) {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void initializeReflection() {
      try {
         Class geyserApiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
         geyserApiMethod = geyserApiClass.getMethod("api");
         isBedrockPlayerMethod = geyserApiClass.getMethod("isBedrockPlayer", UUID.class);
      } catch (Exception var4) {
      } finally {
         reflectionInitialized = true;
      }

   }

   public static boolean isBedrockPlayer(Player player) {
      return isBedrockPlayer(player.getUniqueId());
   }
}
