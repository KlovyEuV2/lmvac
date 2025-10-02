package dev.lmv.lmvac.api.implement.checks.custom;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import dev.lmv.lmvac.api.implement.checks.type.Check;
import dev.lmv.lmvac.api.implement.checks.type.SettingCheck;
import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;
import dev.lmv.lmvac.api.implement.checks.type.interfaces.PacketCheck;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import javax.tools.*;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер для загрузки и управления кастомными чеками
 */
public class CustomCheckManager {
    private final Plugin plugin;
    private final Map<String, Check> loadedChecks = new ConcurrentHashMap<>();
    private final File luaFolder;
    private final File javaFolder;
    private final File compiledFolder;
    private final Map<String, Long> lastModified = new ConcurrentHashMap<>();

    public CustomCheckManager(Plugin plugin) {
        this.plugin = plugin;

        File baseFolder = new File(plugin.getDataFolder(), "checks/custom");
        this.luaFolder = new File(baseFolder, "lua");
        this.javaFolder = new File(baseFolder, "java");
        this.compiledFolder = new File(baseFolder, "compiled");

        luaFolder.mkdirs();
        javaFolder.mkdirs();
        compiledFolder.mkdirs();
    }

    public void loadAllChecks() {
        plugin.getLogger().info("§e[Lua] Загрузка кастомных чеков...");

        var luaCount = loadLuaChecks();

        for (Check check : loadedChecks.values()) {
            try {
                check.register();
            } catch (Exception e) {
                plugin.getLogger().warning("§c[Lua] Ошибка регистрации: " + check.getName());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info(String.format("§a[Lua] Загружено: §eLua: %d, Java: §4недоступно",
                luaCount));
    }

    private int loadLuaChecks() {
        var luaFiles = luaFolder.listFiles((dir, name) -> name.endsWith(".lua"));
        if (luaFiles == null) return 0;

        var loaded = 0;
        for (var file : luaFiles) {
            var checkName = file.getName().replace(".lua", "");
            try {
                var check = new LuaCustomCheck(plugin, file);
                loadedChecks.put(checkName, check);
                lastModified.put(checkName, file.lastModified());
                loaded++;
                plugin.getLogger().info("§a[Lua] -  Lua: §e" + checkName);
            } catch (Exception e) {
                plugin.getLogger().severe("§c[Lua] -  Lua ошибка: §e" + file.getName());
                var errorMessage = e.getMessage() != null ? e.getMessage() : "Неизвестная ошибка инициализации.";
                plugin.getLogger().severe("§c  Ошибка: " + errorMessage);
            }
        }
        return loaded;
    }

    private int loadJavaChecks() {
        var javaFiles = javaFolder.listFiles((dir, name) -> name.endsWith(".java"));
        if (javaFiles == null) return 0;

        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            plugin.getLogger().warning("§e[Lua] Java компилятор недоступен (нужен JDK)");
            return 0;
        }

        var loaded = 0;
        for (var file : javaFiles) {
            try {
                if (compileAndLoadJavaCheck(file, compiler)) {
                    loaded++;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("§c[Lua] -  Java ошибка: §e" + file.getName());
                e.printStackTrace();
            }
        }
        return loaded;
    }

    private boolean compileAndLoadJavaCheck(File file, JavaCompiler compiler) throws Exception {
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);

        var optionList = List.of(
                "-classpath", System.getProperty("java.class.path"),
                "-d", compiledFolder.getAbsolutePath(),
                "--release", "11"
        );

        var compilationUnits =
                fileManager.getJavaFileObjectsFromFiles(Collections.singletonList(file));

        var task = compiler.getTask(
                null, fileManager, diagnostics, optionList, null, compilationUnits);

        var success = task.call();

        if (success) {
            var urls = new URL[]{compiledFolder.toURI().toURL()};
            var classLoader = new URLClassLoader(urls, plugin.getClass().getClassLoader());

            var className = file.getName().replace(".java", "");

            try {
                var checkClass = classLoader.loadClass("dev.lmv.lmvac.custom." + className);

                if (Check.class.isAssignableFrom(checkClass)) {
                    var check = (Check) checkClass.getDeclaredConstructor(Plugin.class).newInstance(plugin);
                    loadedChecks.put(className, check);
                    lastModified.put(className, file.lastModified());
                    plugin.getLogger().info("§a[Lua] -  Java: §e" + className);
                    return true;
                } else {
                    plugin.getLogger().warning("§c[Lua] Класс не наследует Check: " + className);
                }
            } catch (ClassNotFoundException e) {
                plugin.getLogger().severe("§c[Lua] Класс должен быть в пакете: dev.lmv.lmvac.custom");
            }
        } else {
            plugin.getLogger().severe("§c[Lua] Ошибки компиляции " + file.getName() + ":");
            for (var diagnostic : diagnostics.getDiagnostics()) {
                plugin.getLogger().severe(String.format("§c  Строка %d: %s",
                        diagnostic.getLineNumber(),
                        diagnostic.getMessage(null)));
            }
        }

        fileManager.close();
        return false;
    }

    public void reloadAllChecks() {
        plugin.getLogger().info("§e[Lua] Перезагрузка...");

        for (Check check : loadedChecks.values()) {
            try {
                check.unregister();
            } catch (Exception ignored) {}
        }

        loadedChecks.clear();
        lastModified.clear();
        loadAllChecks();
    }

    public void checkForChanges() {
        var changed = false;

        var luaFiles = luaFolder.listFiles((dir, name) -> name.endsWith(".lua"));
        if (luaFiles != null) {
            for (var file : luaFiles) {
                var checkName = file.getName().replace(".lua", "");
                var lastMod = lastModified.get(checkName);

                if (lastMod == null || file.lastModified() > lastMod) {
                    changed = true;
                    break;
                }
            }
        }

        if (changed) {
            plugin.getLogger().info("§e[Lua] Обнаружены изменения");
            reloadAllChecks();
        }
    }

    public Collection<Check> getLoadedChecks() {
        return loadedChecks.values();
    }

    public Check getCheck(String name) {
        return loadedChecks.get(name);
    }
}

// ---------------------------------------------------------------------------------------------------------------------

/**
 * Обертка для Lua чеков
 */
@SettingCheck(value = "LuaCustom", cooldown = Cooldown.NO_COOLDOWN)
class LuaCustomCheck extends Check implements PacketCheck, Listener {
    private final Globals globals;
    private LuaValue script;
    private final File scriptFile;
    private String checkName;
    private Cooldown checkCooldown = Cooldown.NO_COOLDOWN;
    private LuaValue onPacketReceivingFunc;
    private LuaValue onPacketSendingFunc;
    private final List<PacketType> receivingTypes = new ArrayList<>();
    private final List<PacketType> sendingTypes = new ArrayList<>();

    public LuaCustomCheck(Plugin plugin, File scriptFile) throws Exception {
        super(plugin);
        this.scriptFile = scriptFile;

        this.checkName = scriptFile.getName().replace(".lua", "");

        this.globals = JsePlatform.standardGlobals();

        try {
            registerLuaFunctions();

            this.script = globals.loadfile(scriptFile.getAbsolutePath());
            script.call();

            loadConfig();
            loadFunctions();
            loadPacketTypes();

        } catch (Exception e) {
            plugin.getLogger().severe("§c[Lua] Критическая ошибка в " + this.checkName + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public String getName() {
        return this.checkName != null ? this.checkName : super.getName();
    }

    private void registerLuaFunctions() {
        globals.set("flag", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue playerValue, LuaValue reasonValue) {
                try {
                    var player = (Player) playerValue.checkuserdata(Player.class);
                    var reason = reasonValue.isnil() ? "" : reasonValue.tojstring();

                    if (reason.isEmpty()) {
                        LuaCustomCheck.this.flag(player);
                    } else {
                        LuaCustomCheck.this.flag(player, reason);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("§c[Lua] Ошибка в flag() для чека " + getName() + ": " + e.getMessage());
                }
                return LuaValue.NIL;
            }
        });
        globals.set("getConfig", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                try {
                    FileConfiguration config = plugin.getConfig();
                    return CoerceJavaToLua.coerce(config);

                } catch (Exception e) {
                    plugin.getLogger().warning("§c[Lua] Ошибка в getConfig() для чека " + getName() + ": " + e.getMessage());
                }
                return LuaValue.NIL;
            }
        });

        globals.set("runTaskSync", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue function) {
                if (!function.isfunction()) {
                    plugin.getLogger().warning("§c[Lua] Ошибка в runTaskSync() для чека " + getName() + ": Аргумент не является функцией.");
                    return LuaValue.NIL;
                }

                getPlugin().getServer().getScheduler().runTask(getPlugin(), () -> {
                    try {
                        function.call();
                    } catch (LuaError e) {
                        plugin.getLogger().warning("§c[Lua] Ошибка выполнения runTaskSync в " + getName() + ": " + e.getMessage());
                    } catch (Exception e) {
                        plugin.getLogger().warning("§c[Lua] Непредвиденная ошибка в runTaskSync в " + getName());
                        e.printStackTrace();
                    }
                });
                return LuaValue.NIL;
            }
        });

        globals.set("runTaskLaterSync", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue function, LuaValue ticks) {
                long longTicks = Long.valueOf(String.valueOf(ticks));
                if (!function.isfunction()) {
                    plugin.getLogger().warning("§c[Lua] Ошибка в runTaskSync() для чека " + getName() + ": Аргумент не является функцией.");
                    return LuaValue.NIL;
                }

                getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> {
                    try {
                        function.call();
                    } catch (LuaError e) {
                        plugin.getLogger().warning("§c[Lua] Ошибка выполнения runTaskSync в " + getName() + ": " + e.getMessage());
                    } catch (Exception e) {
                        plugin.getLogger().warning("§c[Lua] Непредвиденная ошибка в runTaskSync в " + getName());
                        e.printStackTrace();
                    }
                },longTicks);
                return LuaValue.NIL;
            }
        });

        globals.set("runTaskAsync", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue function) {
                if (!function.isfunction()) {
                    plugin.getLogger().warning("§c[Lua] Ошибка в runTaskAsync() для чека " + getName() + ": Аргумент не является функцией.");
                    return LuaValue.NIL;
                }

                getPlugin().getServer().getScheduler().runTaskAsynchronously(getPlugin(), () -> {
                    try {
                        function.call();
                    } catch (LuaError e) {
                        plugin.getLogger().warning("§c[Lua] Ошибка выполнения runTaskAsync в " + getName() + ": " + e.getMessage());
                    } catch (Exception e) {
                        plugin.getLogger().warning("§c[Lua] Непредвиденная ошибка в runTaskAsync в " + getName());
                        e.printStackTrace();
                    }
                });
                return LuaValue.NIL;
            }
        });

        globals.set("sendMessage", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue playerValue, LuaValue messageValue) {
                try {
                    final Player player = (Player) playerValue.checkuserdata(Player.class);
                    final String message = messageValue.tojstring();

                    getPlugin().getServer().getScheduler().runTask(getPlugin(), () -> {
                        player.sendMessage(message);
                    });
                } catch (Exception e) {
                    plugin.getLogger().warning("§c[Lua] Ошибка в sendMessage() для чека " + getName() + ": " + e.getMessage());
                }
                return LuaValue.NIL;
            }
        });

        globals.set("log", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue message) {
                plugin.getLogger().info("[" + getName() + "] " + message.tojstring());
                return LuaValue.NIL;
            }
        });

        globals.set("getClient", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue playerValue) {
                try {
                    var player = (Player) playerValue.checkuserdata(Player.class);
                    var lmvPlayer = LmvPlayer.players.get(player.getEntityId());
                    return CoerceJavaToLua.coerce(lmvPlayer);
                } catch (Exception e) {
                    return LuaValue.NIL;
                }
            }
        });
    }

    private void loadConfig() {
        var config = globals.get("config");
        if (!config.isnil() && config.istable()) {
            var configTable = config.checktable();

            var nameValue = configTable.get("name");
            if (!nameValue.isnil() && !nameValue.tojstring().strip().isBlank()) {
                this.checkName = nameValue.tojstring().strip();
                this.name = nameValue.tojstring().strip();
            }

            var cooldownValue = configTable.get("cooldown");
            if (!cooldownValue.isnil()) {
                try {
                    this.checkCooldown = Cooldown.valueOf(cooldownValue.tojstring());
                } catch (Exception ignored) {}
            }
        }
    }

    private void loadFunctions() {
        this.onPacketReceivingFunc = globals.get("onPacketReceiving");
        this.onPacketSendingFunc = globals.get("onPacketSending");
    }

    private void loadPacketTypes() {
        var receivingList = globals.get("receivingPackets");
        if (!receivingList.isnil() && receivingList.istable()) {
            var table = receivingList.checktable();
            for (var i = 1; i <= table.length(); i++) {
                var packetName = table.get(i).tojstring();
                var type = parsePacketType(packetName);
                if (type != null) {
                    receivingTypes.add(type);
                }
            }
        }

        var sendingList = globals.get("sendingPackets");
        if (!sendingList.isnil() && sendingList.istable()) {
            var table = sendingList.checktable();
            for (var i = 1; i <= table.length(); i++) {
                var packetName = table.get(i).tojstring();
                var type = parsePacketType(packetName);
                if (type != null) {
                    sendingTypes.add(type);
                }
            }
        }

        if (receivingTypes.isEmpty() && sendingTypes.isEmpty()) {
            receivingTypes.addAll(getAllPacketTypes(PacketType.Play.Client.class));
        }
    }

    private PacketType parsePacketType(String name) {
        if (name == null || name.isBlank()) return null;

        try {
            for (var type : getAllPacketTypes(PacketType.Play.Client.class)) {
                if (type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }

            for (var type : getAllPacketTypes(PacketType.Play.Server.class)) {
                if (type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("§c[Lua] Неизвестный тип пакета: " + name);
        }
        return null;
    }

    private Collection<PacketType> getAllPacketTypes(Class<?> packetTypeClass) {
        var types = new ArrayList<PacketType>();
        for (var field : packetTypeClass.getDeclaredFields()) {
            if (PacketType.class.isAssignableFrom(field.getType())
                    && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                try {
                    field.setAccessible(true);
                    types.add((PacketType) field.get(null));
                } catch (IllegalAccessException ignored) {
                }
            }
        }
        return types;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (onPacketReceivingFunc.isnil() || !onPacketReceivingFunc.isfunction()) {
            return;
        }

        try {
            onPacketReceivingFunc.call(
                    CoerceJavaToLua.coerce(event.getPlayer()),
                    CoerceJavaToLua.coerce(event)
            );
        } catch (LuaError e) {
            plugin.getLogger().warning("§c[Lua] " + getName() + ": " + e.getMessage());
        } catch (Exception ignored) {}
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (onPacketSendingFunc.isnil() || !onPacketSendingFunc.isfunction()) {
            return;
        }

        try {
            onPacketSendingFunc.call(
                    CoerceJavaToLua.coerce(event.getPlayer()),
                    CoerceJavaToLua.coerce(event)
            );
        } catch (LuaError e) {
            plugin.getLogger().warning("§c[Lua] " + getName() + ": " + e.getMessage());
        } catch (Exception ignored) {}
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        if (receivingTypes.isEmpty()) {
            return ListeningWhitelist.EMPTY_WHITELIST;
        }
        return ListeningWhitelist.newBuilder()
                .priority(ListenerPriority.NORMAL)
                .types(receivingTypes.toArray(new PacketType[0]))
                .build();
    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        if (sendingTypes.isEmpty()) {
            return ListeningWhitelist.EMPTY_WHITELIST;
        }
        return ListeningWhitelist.newBuilder()
                .priority(ListenerPriority.NORMAL)
                .types(sendingTypes.toArray(new PacketType[0]))
                .build();
    }

    public File getScriptFile() {
        return scriptFile;
    }
}