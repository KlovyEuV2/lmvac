package dev.lmv.lmvac.api.implement.utils.text;

import org.bukkit.ChatColor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ColorUtil {

    private static final Pattern RGB_PATTERN = Pattern.compile("([&§])#([A-Fa-f0-9]{6})");
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("(?i)([&§][0-9A-FK-OR])");

    public static String setColorCodes(String text) {
        if (text == null) return null;

        Matcher matcher = RGB_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();

        while (matcher.find()) {
            String hexColor = matcher.group(2);
            StringBuilder colorBuilder = new StringBuilder("§x");

            for (char c : hexColor.toCharArray()) {
                colorBuilder.append('§').append(c);
            }

            matcher.appendReplacement(buffer, Matcher.quoteReplacement(colorBuilder.toString()));
        }

        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static String stripColors(String text) {
        return text == null ? null : ChatColor.stripColor(setColorCodes(text));
    }

    public static boolean hasColorCodes(String text) {
        return text != null && (RGB_PATTERN.matcher(text).find() || LEGACY_COLOR_PATTERN.matcher(text).find());
    }

    public static List<String> setColorCodes(List<String> list) {
        if (list == null) return null;
        return list.stream()
                .map(ColorUtil::setColorCodes)
                .collect(Collectors.toList());
    }

    public static List<String> stripColors(List<String> list) {
        if (list == null) return null;
        return list.stream()
                .map(ColorUtil::stripColors)
                .collect(Collectors.toList());
    }
}
