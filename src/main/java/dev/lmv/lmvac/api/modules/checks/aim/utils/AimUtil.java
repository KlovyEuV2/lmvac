package dev.lmv.lmvac.api.modules.checks.aim.utils;

import com.comphenix.protocol.PacketType.Play.Client;
import com.comphenix.protocol.events.PacketEvent;
import dev.lmv.lmvac.api.implement.api.LmvPlayer;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class AimUtil {

    private static final int MAX_CONSECUTIVE_MISSES = 2;

    public static int getLockCount(List<LmvPlayer.LookInformation> looks) {
        return getLockCountWithTolerance(looks, MAX_CONSECUTIVE_MISSES);
    }

    public static int getLockCountWithTolerance(List<LmvPlayer.LookInformation> looks, int maxMisses) {
        if (looks.isEmpty()) {
            return 0;
        }

        int count = 0;
        int consecutiveMisses = 0;
        Entity target = looks.get(0).target;

        for (int i = 1; i < looks.size(); ++i) {
            LmvPlayer.LookInformation look = looks.get(i);
            Location lastLocation = looks.get(i - 1).location;
            Location currentLocation = looks.get(i).location;

            if (currentLocation != null && lastLocation != null) {
                if (look.target != null && look.target.equals(target)) {
                    consecutiveMisses = 0;
                    ++count;
                } else {
                    ++consecutiveMisses;
                    if (consecutiveMisses > maxMisses) {
                        break;
                    }
                }
            }
        }

        return count;
    }

    public static int getSimilarYawDiffCount(List<LmvPlayer.LookInformation> looks, float tolerance) {
        return getSimilarYawDiffCountWithMissTolerance(looks, tolerance, MAX_CONSECUTIVE_MISSES);
    }

    public static int getSimilarYawDiffCountWithMissTolerance(List<LmvPlayer.LookInformation> looks, float tolerance, int maxMisses) {
        if (looks.size() < 3) {
            return 0;
        }

        int count = 0;
        int consecutiveMisses = 0;
        float lastYawDiff = -1.0F;

        for (int i = 2; i < looks.size(); ++i) {
            Location prevLocation = looks.get(i - 2).location;
            Location lastLocation = looks.get(i - 1).location;
            Location currentLocation = looks.get(i).location;

            if (currentLocation != null && lastLocation != null && prevLocation != null) {
                float currentYawDiff = Math.abs(lastLocation.getYaw() - currentLocation.getYaw());
                if (currentYawDiff > 180.0F) {
                    currentYawDiff = 360.0F - currentYawDiff;
                }

                if (lastYawDiff != -1.0F) {
                    float diffBetweenDiffs = Math.abs(lastYawDiff - currentYawDiff);
                    if (diffBetweenDiffs <= tolerance) {
                        consecutiveMisses = 0;
                        ++count;
                    } else {
                        ++consecutiveMisses;
                        if (consecutiveMisses > maxMisses) {
                            break;
                        }
                    }
                }

                lastYawDiff = currentYawDiff;
            }
        }

        return count;
    }

    public static int getRepeatingYawDiffPattern(List<LmvPlayer.LookInformation> looks, float tolerance, int minRepeats) {
        return getRepeatingYawDiffPatternWithMissTolerance(looks, tolerance, minRepeats, MAX_CONSECUTIVE_MISSES);
    }

    public static int getRepeatingYawDiffPatternWithMissTolerance(List<LmvPlayer.LookInformation> looks, float tolerance, int minRepeats, int maxMisses) {
        if (looks.size() < 4) {
            return 0;
        }

        int maxCount = 0;

        for (int start = 1; start < looks.size() - 2; ++start) {
            int currentCount = 1;
            int consecutiveMisses = 0;
            Location startPrevLoc = looks.get(start - 1).location;
            Location startCurrentLoc = looks.get(start).location;

            if (startPrevLoc != null && startCurrentLoc != null) {
                float patternYawDiff = Math.abs(startPrevLoc.getYaw() - startCurrentLoc.getYaw());
                if (patternYawDiff > 180.0F) {
                    patternYawDiff = 360.0F - patternYawDiff;
                }

                for (int i = start + 1; i < looks.size(); ++i) {
                    Location prevLoc = looks.get(i - 1).location;
                    Location currentLoc = looks.get(i).location;

                    if (prevLoc == null || currentLoc == null) {
                        break;
                    }

                    float currentYawDiff = Math.abs(prevLoc.getYaw() - currentLoc.getYaw());
                    if (currentYawDiff > 180.0F) {
                        currentYawDiff = 360.0F - currentYawDiff;
                    }

                    if (Math.abs(patternYawDiff - currentYawDiff) <= tolerance) {
                        consecutiveMisses = 0;
                        ++currentCount;
                    } else {
                        ++consecutiveMisses;
                        if (consecutiveMisses > maxMisses) {
                            break;
                        }
                    }
                }

                if (currentCount >= minRepeats) {
                    maxCount = Math.max(maxCount, currentCount);
                }
            }
        }

        return maxCount;
    }

    public static int getArithmeticYawDiffProgression(List<LmvPlayer.LookInformation> looks, float tolerance) {
        return getArithmeticYawDiffProgressionWithMissTolerance(looks, tolerance, MAX_CONSECUTIVE_MISSES);
    }

    public static int getArithmeticYawDiffProgressionWithMissTolerance(List<LmvPlayer.LookInformation> looks, float tolerance, int maxMisses) {
        if (looks.size() < 4) {
            return 0;
        }

        int maxCount = 0;

        for (int start = 1; start < looks.size() - 2; ++start) {
            Location loc1 = looks.get(start - 1).location;
            Location loc2 = looks.get(start).location;
            Location loc3 = looks.get(start + 1).location;

            if (loc1 != null && loc2 != null && loc3 != null) {
                float yawDiff1 = Math.abs(loc1.getYaw() - loc2.getYaw());
                if (yawDiff1 > 180.0F) {
                    yawDiff1 = 360.0F - yawDiff1;
                }

                float yawDiff2 = Math.abs(loc2.getYaw() - loc3.getYaw());
                if (yawDiff2 > 180.0F) {
                    yawDiff2 = 360.0F - yawDiff2;
                }

                float difference = yawDiff1 - yawDiff2;
                int count = 2;
                int consecutiveMisses = 0;
                float lastYawDiff = yawDiff2;

                for (int i = start + 2; i < looks.size(); ++i) {
                    Location prevLoc = looks.get(i - 1).location;
                    Location currentLoc = looks.get(i).location;

                    if (prevLoc == null || currentLoc == null) {
                        break;
                    }

                    float expectedYawDiff = lastYawDiff - difference;
                    if (expectedYawDiff < 0.0F) {
                        expectedYawDiff = 0.0F;
                    }

                    float actualYawDiff = Math.abs(prevLoc.getYaw() - currentLoc.getYaw());
                    if (actualYawDiff > 180.0F) {
                        actualYawDiff = 360.0F - actualYawDiff;
                    }

                    if (Math.abs(expectedYawDiff - actualYawDiff) <= tolerance) {
                        consecutiveMisses = 0;
                        ++count;
                        lastYawDiff = actualYawDiff;
                    } else {
                        ++consecutiveMisses;
                        if (consecutiveMisses > maxMisses) {
                            break;
                        }
                    }
                }

                maxCount = Math.max(maxCount, count);
            }
        }

        return maxCount;
    }

    public static int getConsistentMovementAim(List<LmvPlayer.LookInformation> looks, PacketEvent packetEvent, float minYaw, float maxYaw, float minPitch, float maxPitch) {
        return getConsistentMovementAimWithMissTolerance(looks, packetEvent, minYaw, maxYaw, minPitch, maxPitch, MAX_CONSECUTIVE_MISSES);
    }

    public static int getConsistentMovementAimWithMissTolerance(List<LmvPlayer.LookInformation> looks, PacketEvent packetEvent, float minYaw, float maxYaw, float minPitch, float maxPitch, int maxMisses) {
        if (looks.size() < 2) {
            return 0;
        } else if (packetEvent.getPacketType() != Client.POSITION_LOOK) {
            return 0;
        } else {
            int count = 0;
            int consecutiveMisses = 0;
            float lastYaw = looks.get(0).location.getYaw();
            float lastPitch = looks.get(0).location.getPitch();

            for (int i = 1; i < looks.size(); ++i) {
                Location lastLocation = looks.get(i - 1).location;
                Location currentLocation = looks.get(i).location;

                if (currentLocation != null && lastLocation != null) {
                    float yawDiff = Math.abs(lastYaw - currentLocation.getYaw());
                    if (yawDiff > 180.0F) {
                        yawDiff = 360.0F - yawDiff;
                    }

                    float pitchDiff = Math.abs(lastPitch - currentLocation.getPitch());
                    if (pitchDiff > 180.0F) {
                        pitchDiff = 360.0F - pitchDiff;
                    }

                    if ((yawDiff > minYaw && yawDiff < maxYaw) || (pitchDiff > minPitch && pitchDiff < maxPitch)) {
                        consecutiveMisses = 0;
                        ++count;
                    } else {
                        ++consecutiveMisses;
                        if (consecutiveMisses > maxMisses) {
                            break;
                        }
                    }

                    lastYaw = currentLocation.getYaw();
                    lastPitch = currentLocation.getPitch();
                }
            }

            return count;
        }
    }

    public static int getLockOnPointCount(List<LmvPlayer.LookInformation> looks, float yaw, float pitch) {
        return getLockOnPointCountWithMissTolerance(looks, yaw, pitch, MAX_CONSECUTIVE_MISSES);
    }

    public static int getLockOnPointCountWithMissTolerance(List<LmvPlayer.LookInformation> looks, float yaw, float pitch, int maxMisses) {
        if (looks.size() < 2) {
            return 0;
        }

        int count = 0;
        int consecutiveMisses = 0;
        Location firstLocation = looks.get(0).location;

        if (firstLocation == null) {
            return 0;
        }

        for (int i = 1; i < looks.size(); ++i) {
            Location lastLocation = looks.get(i - 1).location;
            Location currentLocation = looks.get(i).location;

            if (currentLocation != null && lastLocation != null) {
                boolean yawMatches = Math.abs(firstLocation.getYaw() - currentLocation.getYaw()) < yaw;
                boolean pitchMatches = Math.abs(firstLocation.getPitch() - currentLocation.getPitch()) < pitch;

                if (yawMatches && pitchMatches) {
                    consecutiveMisses = 0;
                    ++count;
                } else {
                    ++consecutiveMisses;
                    if (consecutiveMisses > maxMisses) {
                        break;
                    }
                }
            }
        }

        return count;
    }

    public static int getSmoothAim(List<LmvPlayer.LookInformation> looks, float minYaw, float maxYaw, float minPitch, float maxPitch) {
        if (looks.size() < 2) {
            return 0;
        }

        int count = 0;

        for (int i = 1; i < looks.size(); ++i) {
            Location lastLocation = looks.get(i - 1).location;
            Location currentLocation = looks.get(i).location;

            if (currentLocation != null && lastLocation != null) {
                float yawDiff = Math.abs(lastLocation.getYaw() - currentLocation.getYaw());
                if (yawDiff > 180.0F) {
                    yawDiff = 360.0F - yawDiff;
                }

                float pitchDiff = Math.abs(lastLocation.getPitch() - currentLocation.getPitch());
                if (pitchDiff > 180.0F) {
                    pitchDiff = 360.0F - pitchDiff;
                }

                if (yawDiff > minYaw && yawDiff < maxYaw || pitchDiff > minPitch && pitchDiff < maxPitch) {
                    ++count;
                }
            }
        }

        return count;
    }

    public static int getSmoothDirectionChange(List<LmvPlayer.LookInformation> looks, float product) {
        return getSmoothDirectionChangeWithMissTolerance(looks, product, MAX_CONSECUTIVE_MISSES);
    }

    public static int getSmoothDirectionChangeWithMissTolerance(List<LmvPlayer.LookInformation> looks, float product, int maxMisses) {
        if (looks.size() < 2) {
            return 0;
        }

        int count = 0;
        int consecutiveMisses = 0;

        for (int i = 1; i < looks.size(); ++i) {
            Location lastLocation = looks.get(i - 1).location;
            Location currentLocation = looks.get(i).location;

            if (currentLocation != null && lastLocation != null) {
                Vector lastDirection = lastLocation.getDirection().normalize();
                Vector currentDirection = currentLocation.getDirection().normalize();
                double dotProduct = lastDirection.dot(currentDirection);

                if (dotProduct > (double)product) {
                    consecutiveMisses = 0;
                    ++count;
                } else {
                    ++consecutiveMisses;
                    if (consecutiveMisses > maxMisses) {
                        break;
                    }
                }
            }
        }

        return count;
    }
}