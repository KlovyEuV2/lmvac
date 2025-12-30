package dev.lmv.lmvac.api.implement.checks.type.interfaces;

import org.bukkit.plugin.Plugin;

public interface Configurable {
    default void reloadConfiguration(Plugin plugin) {
    }
}
