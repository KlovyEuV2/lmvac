package dev.lmv.lmvac.api.implement.utils.data;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerDataManager {
    private static File file;
    private static Connection connection;
    private static JavaPlugin plugin;

    private static final Map<String, Double> dbSnapshotCache = new ConcurrentHashMap<>();
    private static final Map<String, Double> suspendsDeltaCache = new ConcurrentHashMap<>();
    private static final Map<UUID, String> themeCache = new ConcurrentHashMap<>();

    private static final AtomicBoolean isSaving = new AtomicBoolean(false);

    public static boolean enabled = false;

    public static void reload(Plugin plugin) {
        enabled = plugin.getConfig().getBoolean("save-data", true);
    }

    public static void setup(JavaPlugin pl) {
        plugin = pl;
        file = new File(plugin.getDataFolder(), "player-data.db");

        initDatabase();
        reload(plugin);
        startAutoSaveTask();
    }

    private static void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());

            Statement stmt = connection.createStatement();

            stmt.execute("CREATE TABLE IF NOT EXISTS player_suspends (" +
                    "uuid TEXT NOT NULL," +
                    "check_name TEXT NOT NULL," +
                    "suspends REAL NOT NULL," +
                    "PRIMARY KEY (uuid, check_name))");

            stmt.execute("CREATE TABLE IF NOT EXISTS player_visual (" +
                    "uuid TEXT PRIMARY KEY," +
                    "theme TEXT NOT NULL)");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_uuid ON player_suspends(uuid)");

            stmt.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Double> getAllSuspends(UUID uuid) {
        Map<String, Double> resultMap = new HashMap<>();
        String uuidPrefix = uuid.toString() + ":";
        Set<String> processedChecks = new HashSet<>();

        for (Map.Entry<String, Double> entry : suspendsDeltaCache.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(uuidPrefix)) {
                String checkName = key.substring(uuidPrefix.length());
                double delta = entry.getValue();
                double dbValue = dbSnapshotCache.getOrDefault(key, 0.0);
                resultMap.put(checkName, dbValue + delta);
                processedChecks.add(checkName);
            }
        }

        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT check_name, suspends FROM player_suspends WHERE uuid = ?");
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String checkName = rs.getString("check_name");
                if (!processedChecks.contains(checkName)) {
                    double value = rs.getDouble("suspends");
                    resultMap.put(checkName, value);
                    dbSnapshotCache.put(uuidPrefix + checkName, value);
                }
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new ConcurrentHashMap<>(resultMap);
    }

    public static void saveSuspends(UUID uuid, String suspendArg, double value) {
        if (!enabled) return;

        String key = uuid.toString() + ":" + suspendArg;

        suspendsDeltaCache.merge(key, value, Double::sum);
    }

    public static void saveTheme(UUID uuid, String name) {
        themeCache.put(uuid, name);
    }

    public static String getTheme(UUID uuid) {
        if (themeCache.containsKey(uuid)) {
            return themeCache.get(uuid);
        }

        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT theme FROM player_visual WHERE uuid = ?");
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String theme = rs.getString("theme");
                rs.close();
                stmt.close();

                themeCache.put(uuid, theme);
                return theme;
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static double getSuspends(UUID uuid, String suspendArg) {
        String key = uuid.toString() + ":" + suspendArg;

        double delta = suspendsDeltaCache.getOrDefault(key, 0.0);

        if (dbSnapshotCache.containsKey(key)) {
            return dbSnapshotCache.get(key) + delta;
        }

        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT suspends FROM player_suspends WHERE uuid = ? AND check_name = ?");
            stmt.setString(1, uuid.toString());
            stmt.setString(2, suspendArg);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                double dbValue = rs.getDouble("suspends");
                rs.close();
                stmt.close();

                dbSnapshotCache.put(key, dbValue);

                return dbValue + delta;
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return delta;
    }

    public static void flush() {
        if (!enabled || isSaving.getAndSet(true)) {
            return;
        }

        try {
            if (!suspendsDeltaCache.isEmpty()) {
                Map<String, Double> deltaSnapshot = new HashMap<>(suspendsDeltaCache);
                suspendsDeltaCache.clear();

                try {
                    connection.setAutoCommit(false);

                    PreparedStatement stmt = connection.prepareStatement(
                            "INSERT INTO player_suspends (uuid, check_name, suspends) VALUES (?, ?, ?) " +
                                    "ON CONFLICT(uuid, check_name) DO UPDATE SET suspends = suspends + ?");

                    for (Map.Entry<String, Double> entry : deltaSnapshot.entrySet()) {
                        String[] parts = entry.getKey().split(":", 2);
                        String uuidStr = parts[0];
                        String checkName = parts[1];
                        double delta = entry.getValue();

                        stmt.setString(1, uuidStr);
                        stmt.setString(2, checkName);
                        stmt.setDouble(3, delta);
                        stmt.setDouble(4, delta);
                        stmt.addBatch();
                    }

                    stmt.executeBatch();
                    connection.commit();
                    stmt.close();

                    for (Map.Entry<String, Double> entry : deltaSnapshot.entrySet()) {
                        dbSnapshotCache.merge(entry.getKey(), entry.getValue(), Double::sum);
                    }

                } catch (SQLException e) {
                    try {
                        connection.rollback();
                        for (Map.Entry<String, Double> entry : deltaSnapshot.entrySet()) {
                            suspendsDeltaCache.merge(entry.getKey(), entry.getValue(), Double::sum);
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                    e.printStackTrace();
                } finally {
                    try {
                        connection.setAutoCommit(true);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (!themeCache.isEmpty()) {
                Map<UUID, String> themeSnapshot = new HashMap<>(themeCache);
                themeCache.clear();

                try {
                    connection.setAutoCommit(false);

                    PreparedStatement stmt = connection.prepareStatement(
                            "INSERT OR REPLACE INTO player_visual (uuid, theme) VALUES (?, ?)");

                    for (Map.Entry<UUID, String> entry : themeSnapshot.entrySet()) {
                        stmt.setString(1, entry.getKey().toString());
                        stmt.setString(2, entry.getValue());
                        stmt.addBatch();
                    }

                    stmt.executeBatch();
                    connection.commit();
                    stmt.close();

                } catch (SQLException e) {
                    try {
                        connection.rollback();
                        themeCache.putAll(themeSnapshot);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                    e.printStackTrace();
                } finally {
                    try {
                        connection.setAutoCommit(true);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            isSaving.set(false);
        }
    }

    public static void close() {
        flush();

        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void startAutoSaveTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, PlayerDataManager::flush, 600L, 600L);
    }
}