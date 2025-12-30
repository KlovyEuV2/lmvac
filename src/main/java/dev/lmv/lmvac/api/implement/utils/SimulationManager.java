package dev.lmv.lmvac.api.implement.utils;

import java.util.*;

// Основной менеджер симуляции
public class SimulationManager {

    private final Map<UUID, PlayerSimulationData> playerData = new HashMap<>();
    private final CollisionEngine collisionEngine = new CollisionEngine();

    // Константы физики Minecraft
    private static final double GRAVITY = 0.08;
    private static final double DRAG = 0.98;
    private static final double FRICTION = 0.6;
    private static final double AIR_RESISTANCE = 0.91;
    private static final double JUMP_VELOCITY = 0.42;
    private static final double SPRINT_MULTIPLIER = 1.3;
    private static final double BASE_ACCELERATION = 0.1;
    private static final double SNEAK_MULTIPLIER = 0.3;
    private static final double WALK_SPEED = 0.1;

    /**
     * Симулирует движение игрока и возвращает все возможные позиции
     */
    public PredictionResult simulateMovement(UUID playerId, PlayerInput input) {
        PlayerSimulationData data = playerData.get(playerId);
        if (data == null) {
            return new PredictionResult(new ArrayList<>());
        }

        List<Vector3d> possiblePositions = new ArrayList<>();

        // Основная симуляция
        Vector3d mainPrediction = simulateTick(data.getState().clone(), input);
        possiblePositions.add(mainPrediction);

        // Добавляем варианты с неопределенностью
        possiblePositions.addAll(simulateUncertainties(data.getState(), input));

        return new PredictionResult(possiblePositions);
    }

    /**
     * Симулирует один тик движения
     */
    private Vector3d simulateTick(PlayerState state, PlayerInput input) {
        Vector3d velocity = state.velocity.clone();
        Vector3d position = new Vector3d(state.x, state.y, state.z);

        // 1. Применяем входные данные игрока
        applyPlayerInput(velocity, input, state);

        // 2. Применяем гравитацию
        if (!state.onGround) {
            velocity.y -= GRAVITY;
            velocity.y *= DRAG;
        }

        // 3. Обрабатываем коллизии
        Vector3d newPosition = position.clone().add(velocity);
        CollisionResult collision = collisionEngine.checkCollisions(
                state.boundingBox,
                position,
                velocity,
                state.world
        );

        if (collision.hasCollision()) {
            velocity = collision.adjustedVelocity;
            newPosition = collision.adjustedPosition;
            state.onGround = collision.onGround;
        } else {
            state.onGround = false;
        }

        // 4. Применяем трение
        applyFriction(velocity, state);

        // 5. Обновляем состояние
        state.velocity = velocity;
        state.x = newPosition.x;
        state.y = newPosition.y;
        state.z = newPosition.z;

        return newPosition;
    }

    /**
     * Применяет входные данные игрока (WASD, прыжок, спринт)
     */
    private void applyPlayerInput(Vector3d velocity, PlayerInput input, PlayerState state) {
        float forward = input.forward;
        float strafe = input.strafe;

        // Базовое ускорение
        double acceleration = BASE_ACCELERATION;

        // Модификатор спринта
        if (state.sprinting && forward > 0 && !state.sneaking) {
            acceleration *= SPRINT_MULTIPLIER;
        }

        // Модификатор подкрадывания
        if (state.sneaking) {
            acceleration *= SNEAK_MULTIPLIER;
        }

        // Модификатор использования предмета (лук, еда)
        if (state.usingItem) {
            acceleration *= 0.2;
        }

        // Прыжок
        if (state.onGround && input.jumping && !state.blockingJump) {
            velocity.y = JUMP_VELOCITY;

            // Бонус к горизонтальной скорости при прыжке со спринтом
            if (state.sprinting && forward > 0) {
                double bonus = 0.2;
                velocity.x += Math.sin(Math.toRadians(state.yaw)) * bonus;
                velocity.z -= Math.cos(Math.toRadians(state.yaw)) * bonus;
            }
        }

        // Применяем движение с учетом направления взгляда
        if (forward != 0 || strafe != 0) {
            double yawRad = Math.toRadians(state.yaw);

            // Нормализуем вектор движения
            double movementLength = Math.sqrt(forward * forward + strafe * strafe);
            if (movementLength > 0) {
                forward /= movementLength;
                strafe /= movementLength;
            }

            // Применяем ускорение
            velocity.x += acceleration * (strafe * Math.cos(yawRad) - forward * Math.sin(yawRad));
            velocity.z += acceleration * (forward * Math.cos(yawRad) + strafe * Math.sin(yawRad));
        }

        // Ограничиваем максимальную скорость
        double maxSpeed = state.sprinting ? 0.286 : 0.22;
        if (state.sneaking) maxSpeed = 0.066;

        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontalSpeed > maxSpeed) {
            double factor = maxSpeed / horizontalSpeed;
            velocity.x *= factor;
            velocity.z *= factor;
        }
    }

    /**
     * Применяет трение
     */
    private void applyFriction(Vector3d velocity, PlayerState state) {
        if (state.onGround) {
            // Трение на земле (зависит от блока)
            double slipperiness = getBlockSlipperiness(state.groundBlock);
            double friction = 0.6 * slipperiness;

            velocity.x *= friction;
            velocity.z *= friction;
        } else {
            // Воздушное сопротивление
            velocity.x *= AIR_RESISTANCE;
            velocity.z *= AIR_RESISTANCE;
        }

        // Дополнительное трение в воде
        if (state.inWater) {
            velocity.x *= 0.8;
            velocity.y *= 0.8;
            velocity.z *= 0.8;
        }

        // Дополнительное трение в паутине
        if (state.inWeb) {
            velocity.x *= 0.25;
            velocity.y *= 0.05;
            velocity.z *= 0.25;
        }
    }

    /**
     * Симулирует все возможные варианты движения с учетом неопределенности
     */
    private List<Vector3d> simulateUncertainties(PlayerState baseState, PlayerInput input) {
        List<Vector3d> results = new ArrayList<>();

        // Неопределенность коллизий (игрок может быть на краю блока)
        double[] collisionOffsets = {-0.03, 0.03};

        for (double xOffset : collisionOffsets) {
            for (double zOffset : collisionOffsets) {
                PlayerState modifiedState = baseState.clone();
                modifiedState.x += xOffset;
                modifiedState.z += zOffset;

                results.add(simulateTick(modifiedState, input));
            }
        }

        // Неопределенность onGround (пограничные случаи)
        if (Math.abs(baseState.velocity.y) < 0.1) {
            PlayerState modifiedState = baseState.clone();
            modifiedState.onGround = !modifiedState.onGround;
            results.add(simulateTick(modifiedState, input));
        }

        // Неопределенность при прыжке (клиент может прыгнуть в любой момент)
        if (input.jumping && !baseState.onGround && baseState.velocity.y < 0.1) {
            PlayerState modifiedState = baseState.clone();
            modifiedState.velocity.y += 0.02; // Небольшая вариация
            results.add(simulateTick(modifiedState, input));
        }

        // Неопределенность Y позиции (0.03 блока погрешность)
        for (double yOffset : new double[]{-0.03, 0.03}) {
            PlayerState modifiedState = baseState.clone();
            modifiedState.y += yOffset;
            results.add(simulateTick(modifiedState, input));
        }

        return results;
    }

    /**
     * Обновляет состояние игрока
     */
    public void updatePlayerState(UUID playerId, double x, double y, double z,
                                  Vector3d velocity, float yaw, float pitch,
                                  boolean sprinting, boolean sneaking, boolean onGround) {
        PlayerSimulationData data = playerData.computeIfAbsent(playerId,
                k -> new PlayerSimulationData());

        PlayerState state = data.getState();
        state.x = x;
        state.y = y;
        state.z = z;
        state.velocity = velocity;
        state.yaw = yaw;
        state.pitch = pitch;
        state.sprinting = sprinting;
        state.sneaking = sneaking;
        state.onGround = onGround;
        state.lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Получает данные игрока
     */
    public PlayerSimulationData getPlayerData(UUID playerId) {
        return playerData.get(playerId);
    }

    /**
     * Очищает данные игрока
     */
    public void removePlayer(UUID playerId) {
        playerData.remove(playerId);
    }

    /**
     * Устанавливает состояние окружения для игрока
     */
    public void updateEnvironmentState(UUID playerId, boolean inWater, boolean inWeb,
                                       boolean inLava, String groundBlock) {
        PlayerSimulationData data = playerData.get(playerId);
        if (data != null) {
            PlayerState state = data.getState();
            state.inWater = inWater;
            state.inWeb = inWeb;
            state.inLava = inLava;
            state.groundBlock = groundBlock;
        }
    }

    /**
     * Устанавливает состояние действия для игрока
     */
    public void updateActionState(UUID playerId, boolean usingItem, boolean blockingJump) {
        PlayerSimulationData data = playerData.get(playerId);
        if (data != null) {
            PlayerState state = data.getState();
            state.usingItem = usingItem;
            state.blockingJump = blockingJump;
        }
    }

    /**
     * Получает скольжение блока
     */
    private double getBlockSlipperiness(String blockType) {
        if (blockType == null) return 0.6;

        switch (blockType.toLowerCase()) {
            case "ice":
            case "packed_ice":
                return 0.98;
            case "blue_ice":
                return 0.989;
            case "slime_block":
                return 0.8;
            case "soul_sand":
                return 0.4;
            case "honey_block":
                return 0.4;
            default:
                return 0.6;
        }
    }

    // ==================== Вспомогательные классы ====================

    /**
     * Состояние игрока для симуляции
     */
    public static class PlayerState implements Cloneable {
        // Позиция
        public double x = 0;
        public double y = 0;
        public double z = 0;

        // Скорость
        public Vector3d velocity = new Vector3d(0, 0, 0);

        // Ротация
        public float yaw = 0;
        public float pitch = 0;

        // Состояние движения
        public boolean onGround = false;
        public boolean sprinting = false;
        public boolean sneaking = false;
        public boolean usingItem = false;
        public boolean blockingJump = false;

        // Состояние окружения
        public boolean inWater = false;
        public boolean inLava = false;
        public boolean inWeb = false;
        public String groundBlock = null;

        // Метаданные
        public BoundingBox boundingBox = new BoundingBox(0.6, 1.8);
        public Object world = null;
        public long lastUpdateTime = 0;

        /**
         * Получает текущую позицию как вектор
         */
        public Vector3d getPosition() {
            return new Vector3d(x, y, z);
        }

        /**
         * Устанавливает позицию из вектора
         */
        public void setPosition(Vector3d position) {
            this.x = position.x;
            this.y = position.y;
            this.z = position.z;
        }

        /**
         * Проверяет, находится ли игрок в особом состоянии окружения
         */
        public boolean isInSpecialEnvironment() {
            return inWater || inLava || inWeb;
        }

        /**
         * Проверяет, может ли игрок бежать
         */
        public boolean canSprint() {
            return !sneaking && !usingItem && onGround;
        }

        @Override
        public PlayerState clone() {
            try {
                PlayerState cloned = (PlayerState) super.clone();
                cloned.velocity = this.velocity.clone();
                cloned.boundingBox = this.boundingBox.clone();
                return cloned;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return String.format(
                    "PlayerState{pos=(%.2f, %.2f, %.2f), vel=(%.3f, %.3f, %.3f), " +
                            "yaw=%.1f, onGround=%s, sprinting=%s, sneaking=%s}",
                    x, y, z, velocity.x, velocity.y, velocity.z,
                    yaw, onGround, sprinting, sneaking
            );
        }
    }

    /**
     * Входные данные от игрока
     */
    public static class PlayerInput {
        public float forward = 0;  // -1 до 1
        public float strafe = 0;   // -1 до 1
        public boolean jumping = false;

        public PlayerInput(float forward, float strafe, boolean jumping) {
            this.forward = Math.max(-1, Math.min(1, forward));
            this.strafe = Math.max(-1, Math.min(1, strafe));
            this.jumping = jumping;
        }

        public boolean isMoving() {
            return forward != 0 || strafe != 0;
        }

        @Override
        public String toString() {
            return String.format("Input{forward=%.2f, strafe=%.2f, jump=%s}",
                    forward, strafe, jumping);
        }
    }

    /**
     * Результат предсказания
     */
    public static class PredictionResult {
        private final List<Vector3d> possiblePositions;

        public PredictionResult(List<Vector3d> possiblePositions) {
            this.possiblePositions = possiblePositions;
        }

        public List<Vector3d> getPossiblePositions() {
            return possiblePositions;
        }

        public double getMinOffset(Vector3d actualPosition) {
            if (possiblePositions.isEmpty()) {
                return Double.MAX_VALUE;
            }

            double minOffset = Double.MAX_VALUE;
            for (Vector3d predicted : possiblePositions) {
                double offset = predicted.distance(actualPosition);
                minOffset = Math.min(minOffset, offset);
            }
            return minOffset;
        }

        public Vector3d getClosestPosition(Vector3d actualPosition) {
            if (possiblePositions.isEmpty()) {
                return null;
            }

            Vector3d closest = possiblePositions.get(0);
            double minDistance = closest.distance(actualPosition);

            for (int i = 1; i < possiblePositions.size(); i++) {
                Vector3d pos = possiblePositions.get(i);
                double distance = pos.distance(actualPosition);
                if (distance < minDistance) {
                    minDistance = distance;
                    closest = pos;
                }
            }

            return closest;
        }
    }

    /**
     * Данные симуляции игрока
     */
    public static class PlayerSimulationData {
        private final PlayerState state = new PlayerState();
        private final Deque<Vector3d> positionHistory = new ArrayDeque<>();
        private final Deque<Long> timestampHistory = new ArrayDeque<>();
        private static final int MAX_HISTORY = 20;

        public PlayerState getState() {
            return state;
        }

        public void addToHistory(Vector3d position) {
            positionHistory.addLast(position);
            timestampHistory.addLast(System.currentTimeMillis());

            if (positionHistory.size() > MAX_HISTORY) {
                positionHistory.removeFirst();
                timestampHistory.removeFirst();
            }
        }

        public Deque<Vector3d> getHistory() {
            return positionHistory;
        }

        public Deque<Long> getTimestampHistory() {
            return timestampHistory;
        }

        /**
         * Получает среднюю скорость за последние N тиков
         */
        public double getAverageSpeed(int ticks) {
            if (positionHistory.size() < 2) {
                return 0;
            }

            int samples = Math.min(ticks, positionHistory.size() - 1);
            double totalDistance = 0;

            Vector3d[] positions = positionHistory.toArray(new Vector3d[0]);
            for (int i = positions.length - samples - 1; i < positions.length - 1; i++) {
                totalDistance += positions[i].distance(positions[i + 1]);
            }

            return totalDistance / samples;
        }
    }

    /**
     * 3D вектор
     */
    public static class Vector3d implements Cloneable {
        public double x, y, z;

        public Vector3d(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Vector3d add(Vector3d other) {
            this.x += other.x;
            this.y += other.y;
            this.z += other.z;
            return this;
        }

        public Vector3d subtract(Vector3d other) {
            this.x -= other.x;
            this.y -= other.y;
            this.z -= other.z;
            return this;
        }

        public Vector3d multiply(double scalar) {
            this.x *= scalar;
            this.y *= scalar;
            this.z *= scalar;
            return this;
        }

        public double distance(Vector3d other) {
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            double dz = this.z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        public double distanceXZ(Vector3d other) {
            double dx = this.x - other.x;
            double dz = this.z - other.z;
            return Math.sqrt(dx * dx + dz * dz);
        }

        public double length() {
            return Math.sqrt(x * x + y * y + z * z);
        }

        public double lengthSquared() {
            return x * x + y * y + z * z;
        }

        public Vector3d normalize() {
            double length = length();
            if (length > 0) {
                this.x /= length;
                this.y /= length;
                this.z /= length;
            }
            return this;
        }

        @Override
        public Vector3d clone() {
            return new Vector3d(x, y, z);
        }

        @Override
        public String toString() {
            return String.format("(%.3f, %.3f, %.3f)", x, y, z);
        }
    }

    /**
     * Ограничивающий бокс
     */
    public static class BoundingBox implements Cloneable {
        public double width, height;

        public BoundingBox(double width, double height) {
            this.width = width;
            this.height = height;
        }

        public double getMinX(double centerX) {
            return centerX - width / 2;
        }

        public double getMaxX(double centerX) {
            return centerX + width / 2;
        }

        public double getMinY(double footY) {
            return footY;
        }

        public double getMaxY(double footY) {
            return footY + height;
        }

        public double getMinZ(double centerZ) {
            return centerZ - width / 2;
        }

        public double getMaxZ(double centerZ) {
            return centerZ + width / 2;
        }

        @Override
        public BoundingBox clone() {
            return new BoundingBox(width, height);
        }
    }

    /**
     * Движок коллизий (упрощенная версия)
     */
    private static class CollisionEngine {

        public CollisionResult checkCollisions(BoundingBox box, Vector3d position,
                                               Vector3d velocity, Object world) {
            // Упрощенная проверка коллизий
            // В реальном античите здесь должна быть полноценная система коллизий

            Vector3d adjustedVelocity = velocity.clone();
            Vector3d adjustedPosition = position.clone().add(velocity);
            boolean onGround = false;

            // Проверка коллизии с землей
            if (velocity.y < 0) {
                double groundY = Math.floor(position.y);
                double feetY = adjustedPosition.y;

                if (feetY <= groundY + 0.001) {
                    adjustedPosition.y = groundY;
                    adjustedVelocity.y = 0;
                    onGround = true;
                }
            }

            // Проверка коллизии с потолком
            if (velocity.y > 0) {
                double ceilingY = Math.ceil(position.y + box.height);
                double headY = adjustedPosition.y + box.height;

                if (headY >= ceilingY - 0.001) {
                    adjustedPosition.y = ceilingY - box.height;
                    adjustedVelocity.y = 0;
                }
            }

            // Проверка горизонтальных коллизий (стены)
            double halfWidth = box.width / 2;

            // X-ось
            double minX = Math.floor(adjustedPosition.x - halfWidth);
            double maxX = Math.ceil(adjustedPosition.x + halfWidth);
            if (adjustedPosition.x - halfWidth < minX + 0.001) {
                adjustedPosition.x = minX + halfWidth;
                adjustedVelocity.x = 0;
            } else if (adjustedPosition.x + halfWidth > maxX - 0.001) {
                adjustedPosition.x = maxX - halfWidth;
                adjustedVelocity.x = 0;
            }

            // Z-ось
            double minZ = Math.floor(adjustedPosition.z - halfWidth);
            double maxZ = Math.ceil(adjustedPosition.z + halfWidth);
            if (adjustedPosition.z - halfWidth < minZ + 0.001) {
                adjustedPosition.z = minZ + halfWidth;
                adjustedVelocity.z = 0;
            } else if (adjustedPosition.z + halfWidth > maxZ - 0.001) {
                adjustedPosition.z = maxZ - halfWidth;
                adjustedVelocity.z = 0;
            }

            return new CollisionResult(adjustedPosition, adjustedVelocity, onGround);
        }
    }

    /**
     * Результат проверки коллизий
     */
    private static class CollisionResult {
        public final Vector3d adjustedPosition;
        public final Vector3d adjustedVelocity;
        public final boolean onGround;

        public CollisionResult(Vector3d adjustedPosition, Vector3d adjustedVelocity, boolean onGround) {
            this.adjustedPosition = adjustedPosition;
            this.adjustedVelocity = adjustedVelocity;
            this.onGround = onGround;
        }

        public boolean hasCollision() {
            return onGround;
        }
    }
}