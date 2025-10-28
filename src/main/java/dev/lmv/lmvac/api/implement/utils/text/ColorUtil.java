package dev.lmv.lmvac.api.implement.utils.text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;

public class ColorUtil {
   private static final Pattern RGB_PATTERN = Pattern.compile("([&§])#([A-Fa-f0-9]{6})");
   private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("(?i)([&§][0-9A-FK-OR])");

   public static String setColorCodes(String text) {
      if (text == null) {
         return null;
      } else {
         Matcher matcher = RGB_PATTERN.matcher(text);
         StringBuilder buffer = new StringBuilder();

         while(matcher.find()) {
            String hexColor = matcher.group(2);
            StringBuilder colorBuilder = new StringBuilder("§x");
            char[] var5 = hexColor.toCharArray();

             for (char c : var5) {
                 colorBuilder.append('§').append(c);
             }

            matcher.appendReplacement(buffer, Matcher.quoteReplacement(colorBuilder.toString()));
         }

         matcher.appendTail(buffer);
         return ChatColor.translateAlternateColorCodes('&', buffer.toString());
      }
   }

   public static String stripColors(String text) {
      return text == null ? null : ChatColor.stripColor(setColorCodes(text));
   }

   public static boolean hasColorCodes(String text) {
      return text != null && (RGB_PATTERN.matcher(text).find() || LEGACY_COLOR_PATTERN.matcher(text).find());
   }
}
