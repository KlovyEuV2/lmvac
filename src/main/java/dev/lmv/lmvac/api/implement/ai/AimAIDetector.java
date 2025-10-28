package dev.lmv.lmvac.api.implement.ai;

import dev.lmv.lmvac.LmvAC;
import dev.lmv.lmvac.api.ConfigManager;
import dev.lmv.lmvac.api.implement.ai.data.AimData;
import dev.lmv.lmvac.api.implement.ai.network.NeuralNetwork;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.*;
import java.util.*;

public class AimAIDetector implements Listener {
    private static LmvAC plugin;
    private NeuralNetwork model;
    private boolean isTrained;

    private final Map<UUID, Queue<AimData.AimSnapshot>> recentSnapshots;
    public final Map<UUID, Integer> violationLevel;
    private final Map<UUID, Long> lastViolationTime;
    private final Map<UUID, Integer> consecutiveDetections;
    private static int SNAPSHOTS_TO_ANALYZE = 30;
    private static double DETECTION_THRESHOLD = 0.75;
    private static double MIN_MOVEMENT_THRESHOLD = 5.0;
    private static double SIGNIFICANT_MOVEMENT_THRESHOLD = 15.0;
    private static double CONSISTENCY_THRESHOLD = 0.6;

    private static int vlPerDetect = 5;
    private static int punishVl = 100;
    private static int alertPerVl = 5;
    private static int violationCooldown = 40;

    private static Long resetTicks = 600L;

    private static List<String> punishments = new ArrayList<>();

    public AimAIDetector(LmvAC plugin) {
        this.plugin = plugin;
        this.recentSnapshots = new HashMap<>();
        this.violationLevel = new HashMap<>();
        this.lastViolationTime = new HashMap<>();
        this.consecutiveDetections = new HashMap<>();
        this.isTrained = false;
        this.model = new NeuralNetwork(14, 24, 1);
        Bukkit.getPluginManager().registerEvents(this,plugin);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        clearPlayerData(uuid);
    }

    public static void reload() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("aimML");
        try {
            SNAPSHOTS_TO_ANALYZE = section.getInt("analyze.min-snapshots",30);
            DETECTION_THRESHOLD = section.getDouble("threshold",0.75);
            MIN_MOVEMENT_THRESHOLD = section.getDouble("min-movement-threshold",5.0);
            SIGNIFICANT_MOVEMENT_THRESHOLD = section.getDouble("significant-movement-threshold",15.0);
            CONSISTENCY_THRESHOLD = section.getDouble("consistency-threshold",0.6);
            vlPerDetect = section.getInt("vl.per-detect",5);
            punishVl = section.getInt("vl.punishment",100);
            alertPerVl = section.getInt("vl.alerts",5);
            punishments = section.getStringList("punish-commands");
            resetTicks = section.getLong("vl.reset",600);
            violationCooldown = section.getInt("vl.cooldown",40);
        } catch (Exception e) {
            SNAPSHOTS_TO_ANALYZE = 30;
            DETECTION_THRESHOLD = 0.75;
            MIN_MOVEMENT_THRESHOLD = 5.0;
            SIGNIFICANT_MOVEMENT_THRESHOLD = 15.0;
            CONSISTENCY_THRESHOLD = 0.6;
            vlPerDetect = 5;
            punishVl = 100;
            alertPerVl = 5;
            punishments = new ArrayList<>();
            resetTicks = 600L;
            violationCooldown = 40;
        }
    }

    public String appendPlaceholders(Player player, String message) {
        return message
                .replaceAll("%player%",player.getName())
                .replaceAll("%prefix%", ConfigManager.prefix);
    }

    public void trainModel() {
        plugin.getLogger().info("════════════════════════════════════");
        plugin.getLogger().info("  Начинаю обучение AI модели для детекции аима...");
        plugin.getLogger().info("════════════════════════════════════");

        File dataFolder = new File(plugin.getDataFolder(), "AI/aim_data");
        if (!dataFolder.exists() || !dataFolder.isDirectory()) {
            plugin.getLogger().warning("└─ ✗ ОШИБКА: Папка с данными не найдена!");
            return;
        }

        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".aim"));
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("└─ ✗ ОШИБКА: Нет данных для обучения!");
            return;
        }

        List<AimData> allData = new ArrayList<>();
        int cheatSessions = 0, legitSessions = 0;
        int cheatSnapshots = 0, legitSnapshots = 0;

        for (File file : files) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                AimData data = (AimData) ois.readObject();
                allData.add(data);

                if (data.isCheat()) {
                    cheatSessions++;
                    cheatSnapshots += data.getSnapshotCount();
                } else {
                    legitSessions++;
                    legitSnapshots += data.getSnapshotCount();
                }
            } catch (IOException | ClassNotFoundException e) {
                plugin.getLogger().warning("Ошибка загрузки файла: " + file.getName());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("├─ Загружено сессий: " + allData.size());
        plugin.getLogger().info("│  ├─ Читеры: " + cheatSessions + " сессий (" + cheatSnapshots + " снапшотов)");
        plugin.getLogger().info("│  └─ Легит: " + legitSessions + " сессий (" + legitSnapshots + " снапшотов)");

        List<double[]> features = new ArrayList<>();
        List<Double> labels = new ArrayList<>();

        for (AimData data : allData) {
            for (AimData.AimSnapshot snapshot : data.getSnapshots()) {
                features.add(snapshot.toFeatures());
                labels.add(data.isCheat() ? 1.0 : 0.0);
            }
        }

        plugin.getLogger().info("├─ Всего примеров для обучения: " + features.size());

        double[][] normalized = normalizeFeatures(features);

        int epochs = 1500;
        double learningRate = 0.01;

        plugin.getLogger().info("├─ Параметры обучения:");
        plugin.getLogger().info("│  ├─ Эпохи: " + epochs);
        plugin.getLogger().info("│  ├─ Learning rate: " + learningRate);
        plugin.getLogger().info("│  └─ Архитектура: 14->24->1");
        plugin.getLogger().info("│");
        plugin.getLogger().info("├─ Обучение модели...");

        long startTime = System.currentTimeMillis();

        for (int epoch = 0; epoch < epochs; epoch++) {
            double totalLoss = 0.0;

            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < normalized.length; i++) indices.add(i);
            Collections.shuffle(indices);

            for (int idx : indices) {
                double prediction = model.forward(normalized[idx]);
                double target = labels.get(idx);
                double loss = Math.pow(prediction - target, 2);
                totalLoss += loss;

                model.backward(target, learningRate);
            }

            if (epoch % 150 == 0 || epoch == epochs - 1) {
                double avgLoss = totalLoss / normalized.length;
                int progress = (int) ((epoch + 1) * 100.0 / epochs);
                plugin.getLogger().info(String.format("│  [%3d%%] Эпоха %4d/%d - Loss: %.4f",
                        progress, epoch + 1, epochs, avgLoss));
            }
        }

        long trainTime = System.currentTimeMillis() - startTime;

        int correct = 0;
        int totalTests = Math.min(normalized.length, 1000);

        for (int i = 0; i < totalTests; i++) {
            double prediction = model.forward(normalized[i]);
            double actual = labels.get(i);
            if ((prediction > 0.5 && actual == 1.0) || (prediction <= 0.5 && actual == 0.0)) {
                correct++;
            }
        }

        double accuracy = (correct * 100.0) / totalTests;

        isTrained = true;

        plugin.getLogger().info("│");
        plugin.getLogger().info("├─ ✓ Обучение завершено!");
        plugin.getLogger().info("│  ├─ Время обучения: " + (trainTime / 1000.0) + " сек");
        plugin.getLogger().info("│  └─ Точность на тесте: " + String.format("%.2f%%", accuracy));
        plugin.getLogger().info("│");
        plugin.getLogger().info("└─ Модель готова к работе!");
        plugin.getLogger().info("════════════════════════════════════");
    }

    public void addSnapshotForAnalysis(Player player, AimData.AimSnapshot snapshot, boolean hasTarget) {
        if (!isTrained || !hasTarget) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Queue<AimData.AimSnapshot> snapshots = recentSnapshots.computeIfAbsent(uuid, k -> new LinkedList<>());

        snapshots.offer(snapshot);
        if (snapshots.size() > SNAPSHOTS_TO_ANALYZE) {
            snapshots.poll();
        }

        if (snapshots.size() >= 10 && snapshots.size() % 5 == 0) {
            analyzePlayer(player, snapshots);
        }
    }

    private void analyzePlayer(Player player, Queue<AimData.AimSnapshot> snapshots) {
        double totalYawSpeed = 0;
        double totalPitchSpeed = 0;
        int snapshotCount = snapshots.size();
        double maxYawSpeed = 0, maxPitchSpeed = 0;
        double minYawSpeed = Double.MAX_VALUE, minPitchSpeed = Double.MAX_VALUE;

        for (AimData.AimSnapshot snapshot : snapshots) {
            double yawSpeed = Math.abs(snapshot.getYawSpeed());
            double pitchSpeed = Math.abs(snapshot.getPitchSpeed());

            totalYawSpeed += yawSpeed;
            totalPitchSpeed += pitchSpeed;

            maxYawSpeed = Math.max(maxYawSpeed, yawSpeed);
            maxPitchSpeed = Math.max(maxPitchSpeed, pitchSpeed);
            minYawSpeed = Math.min(minYawSpeed, yawSpeed);
            minPitchSpeed = Math.min(minPitchSpeed, pitchSpeed);
        }

        double avgYawSpeed = totalYawSpeed / snapshotCount;
        double avgPitchSpeed = totalPitchSpeed / snapshotCount;

        if (avgYawSpeed < MIN_MOVEMENT_THRESHOLD && avgPitchSpeed < MIN_MOVEMENT_THRESHOLD) {
            UUID uuid = player.getUniqueId();
            int currentVL = violationLevel.getOrDefault(uuid, 0);
            if (currentVL > 0) {
                violationLevel.put(uuid, Math.max(0, currentVL - 1));
            }
            return;
        }

        double yawConsistency = (maxYawSpeed - minYawSpeed) / Math.max(1.0, maxYawSpeed);
        double pitchConsistency = (maxPitchSpeed - minPitchSpeed) / Math.max(1.0, maxPitchSpeed);
        double overallConsistency = (yawConsistency + pitchConsistency) / 2.0;

        if (overallConsistency < 0.2 && avgYawSpeed < SIGNIFICANT_MOVEMENT_THRESHOLD && avgPitchSpeed < SIGNIFICANT_MOVEMENT_THRESHOLD) {
            UUID uuid = player.getUniqueId();
            int currentVL = violationLevel.getOrDefault(uuid, 0);
            if (currentVL > 0) {
                violationLevel.put(uuid, Math.max(0, currentVL - 1));
            }
            return;
        }

        double totalCheatProbability = 0.0;
        int count = 0;
        double avgYawDiff = 0, avgPitchDiff = 0;
        int suspiciousSnapshots = 0;

        for (AimData.AimSnapshot snapshot : snapshots) {
            double[] features = normalizeFeatures(snapshot.toFeatures());
            double probability = model.forward(features);
            totalCheatProbability += probability;
            count++;

            avgYawDiff += Math.abs(snapshot.getYawDiff());
            avgPitchDiff += Math.abs(snapshot.getPitchDiff());

            if (probability > DETECTION_THRESHOLD) {
                suspiciousSnapshots++;
            }
        }

        double avgProbability = totalCheatProbability / count;
        avgYawDiff /= count;
        avgPitchDiff /= count;

        double suspiciousRatio = (double) suspiciousSnapshots / count;

        UUID uuid = player.getUniqueId();
        int currentVL = violationLevel.getOrDefault(uuid, 0);
        Long lastViolation = lastViolationTime.getOrDefault(uuid, 0L);
        int consecutive = consecutiveDetections.getOrDefault(uuid, 0);

        long currentTime = System.currentTimeMillis();
        boolean inCooldown = (currentTime - lastViolation) < (violationCooldown * 50);

        if (avgProbability > DETECTION_THRESHOLD &&
                suspiciousRatio > CONSISTENCY_THRESHOLD &&
                !inCooldown &&
                (avgYawSpeed > SIGNIFICANT_MOVEMENT_THRESHOLD || avgPitchSpeed > SIGNIFICANT_MOVEMENT_THRESHOLD)) {

            int vlIncrease = vlPerDetect;
            if (consecutive > 3) {
                vlIncrease *= 2;
            }

            currentVL += vlIncrease;
            violationLevel.put(uuid, currentVL);
            lastViolationTime.put(uuid, currentTime);
            consecutiveDetections.put(uuid, consecutive + 1);

            int finalCurrentVL = currentVL;
            if (finalCurrentVL % alertPerVl == 0) {
                plugin.getServer().getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("lmvai.alerts") &&
                                (LmvPlayer.players.get(p.getEntityId()) == null || LmvPlayer.players.get(p.getEntityId()).alerts))
                        .forEach(admin -> admin.sendMessage(
                                String.format("§c[AIM AI] §e%s §7подозревается в AimML §c(%.0f%% | VL:%d)",
                                        player.getName(), avgProbability * 100, finalCurrentVL)
                        ));
                Bukkit.getConsoleSender().sendMessage(String.format("§c[AIM AI] §e%s §7подозревается в AimML §c(%.0f%% | VL:%d)",
                        player.getName(), avgProbability * 100, finalCurrentVL));
            }

            if (currentVL >= punishVl) {
                for (String punish : punishments) {
                    String message = appendPlaceholders(player,punish);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),message);
                    });
                }
                plugin.getLogger().severe("[LmvAI] " + player.getName() + " наказан за AimML!");
            }

            Bukkit.getScheduler().runTaskLater(plugin,() -> {
                AimAIDetector aimAIDetector = LmvAC.getAimAIDetector();
                if (aimAIDetector.violationLevel.containsKey(player.getUniqueId())) {
                    aimAIDetector.violationLevel.compute(player.getUniqueId(), (k, from) -> Math.max(0, from - vlPerDetect));
                }
            }, resetTicks);
        } else {
            if (avgProbability < 0.3) {
                consecutiveDetections.put(uuid, 0);
            }

            if (currentVL > 0 && avgProbability < 0.4) {
                currentVL = Math.max(0, currentVL - 1);
                violationLevel.put(uuid, currentVL);
            }
        }
    }

    private double[][] normalizeFeatures(List<double[]> features) {
        if (features.isEmpty()) return new double[0][];

        int featureCount = features.get(0).length;
        double[] mins = new double[featureCount];
        double[] maxs = new double[featureCount];
        Arrays.fill(mins, Double.MAX_VALUE);
        Arrays.fill(maxs, Double.MIN_VALUE);

        for (double[] feature : features) {
            for (int i = 0; i < featureCount; i++) {
                mins[i] = Math.min(mins[i], feature[i]);
                maxs[i] = Math.max(maxs[i], feature[i]);
            }
        }

        double[][] normalized = new double[features.size()][];
        for (int i = 0; i < features.size(); i++) {
            normalized[i] = normalizeFeatures(features.get(i), mins, maxs);
        }

        return normalized;
    }

    private double[] normalizeFeatures(double[] features) {
        double[] normalized = new double[features.length];
        normalized[0] = Math.min(features[0] / 6.0, 1.0);
        normalized[1] = Math.min(features[1] / 180.0, 1.0);
        normalized[2] = Math.min(features[2] / 90.0, 1.0);
        normalized[3] = Math.min(features[3] / 1000.0, 1.0);
        normalized[4] = Math.min(features[4] / 500.0, 1.0);
        normalized[5] = Math.min(features[5] / 500.0, 1.0);
        normalized[6] = features[6];
        normalized[7] = features[7];
        normalized[8] = Math.min(features[8] / 500.0, 1.0);
        normalized[9] = Math.min(features[9] / 18000.0, 1.0);
        normalized[10] = Math.min(features[10] / 500.0, 1.0);
        normalized[11] = Math.min(features[11] / 500.0, 1.0);
        normalized[12] = Math.min(features[12] / 180.0, 1.0);
        normalized[13] = Math.min(features[13] / 500.0, 1.0);
        return normalized;
    }

    private double[] normalizeFeatures(double[] features, double[] mins, double[] maxs) {
        double[] normalized = new double[features.length];
        for (int i = 0; i < features.length; i++) {
            double range = maxs[i] - mins[i];
            normalized[i] = range > 0 ? (features[i] - mins[i]) / range : 0;
        }
        return normalized;
    }

    public void saveModel() {
        if (!isTrained) return;

        File modelFile = new File(plugin.getDataFolder(), "AI/aim_model.dat");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(modelFile))) {
            oos.writeObject(model);
            plugin.getLogger().info("AI модель для детекции аима сохранена!");
        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка сохранения модели: " + e.getMessage());
        }
    }

    public void loadModel() {
        File modelFile = new File(plugin.getDataFolder(), "AI/aim_model.dat");
        if (!modelFile.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelFile))) {
            model = (NeuralNetwork) ois.readObject();
            isTrained = true;
            plugin.getLogger().info("AI модель для детекции аима загружена!");
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().warning("Не удалось загрузить модель: " + e.getMessage());
        }
    }

    public boolean isTrained() {
        return isTrained;
    }

    public void clearPlayerData(UUID uuid) {
        recentSnapshots.remove(uuid);
        violationLevel.remove(uuid);
        lastViolationTime.remove(uuid);
        consecutiveDetections.remove(uuid);
    }
}