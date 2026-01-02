package dev.lmv.lmvac.api.implement.utils.simulation;

public class SimulationUtil {
    public static double applySpeed(double attribute, double base) {
        return base * (attribute / 0.1);
    }


    public static boolean isWalk(double at, double deltaXZ, double thr) {
        double a = applySpeed(at,0.2154);
        boolean b = isWalkDoubleInput(at, deltaXZ, thr);
        return (Math.abs(deltaXZ-a) < thr) || b;
    }

    public static boolean isStrafe(double at, double deltaXZ, double thr) {
        return false;
    }

    public static boolean isWalkDoubleInput(double at, double deltaXZ, double thr) {
        double a = applySpeed(at,0.22026434533275843);
        return (Math.abs(deltaXZ-a) < thr);
    }

    public static boolean isSwim(double deltaXZ, double thr) {
        double a = 0.19599999999;
        boolean b = isSwimDoubleInput(deltaXZ, thr);
        return (Math.abs(deltaXZ-a) < thr) || b;
    }

    public static boolean isSwimDoubleInput(double deltaXZ, double thr) {
        double a = 0.19999999999;
        return (Math.abs(deltaXZ-a) < thr);
    }

    public static boolean isSwimJump(double deltaY, double thr) {
        return false;
    }

    public static boolean isFluidFalling(double deltaXZ, double thr) {
        double a = -0.05;
        return (Math.abs(deltaXZ-a) < thr);
    }

    public static boolean isSneak(double at, double deltaXZ, double thr) {
        double a = applySpeed(at,0.0649);
        boolean b = isSneakDoubleInput2(at, deltaXZ, thr);
        boolean c = isSneakDoubleInput3(at, deltaXZ, thr);
        return (Math.abs(deltaXZ-a) < thr) || b || c;
    }

    public static boolean isSneakDoubleInput2(double at, double deltaXZ, double thr) {
        double a = applySpeed(at,0.09157)
//                * (1+(sLvl * 0.20207))
                ;
        return (Math.abs(deltaXZ-a) < thr);
    }

    public static boolean isSneakDoubleInput3(double at, double deltaXZ, double thr) {
        double a = applySpeed(at,0.1190556);
        return (Math.abs(deltaXZ-a) < thr);
    }

    public static boolean isShield(double at, double deltaXZ, double thr) {
        double a = applySpeed(at,0.0433);
        boolean b = isShieldDoubleInput(at, deltaXZ, thr);
        boolean c = isShieldSneak(at, deltaXZ, thr);
        return (Math.abs(deltaXZ-a) < thr) || b || c;
    }

    public static boolean isShieldDoubleInput(double at, double deltaXZ, double thr) {
        double a = applySpeed(at,0.0610541);
        return (Math.abs(deltaXZ-a) < thr);
    }

    public static boolean isShieldSneak(double at, double deltaXZ, double thr) {
        double a = applySpeed(at,0.03885);
        boolean b = isShieldSneakDoubleInput(at, deltaXZ, thr);
        return (Math.abs(deltaXZ-a) < thr) || b;
    }

    public static boolean isShieldSneakDoubleInput(double at, double deltaXZ, double thr) {
        double a = applySpeed(at,0.0366324);
        return (Math.abs(deltaXZ-a) < thr);
    }

    public static boolean isJump(int sLvl, double deltaY, double thr) {
        double a = 0.41999998688697815 * (1+(sLvl * 0.2381));
        return (Math.abs(deltaY-a) < thr);
    }

    public static boolean isBlockCollision(double deltaY, double thr, boolean boat, boolean half) {
        boolean a = isBoatCollision(deltaY, thr);
        boolean b = isHalf(deltaY, thr);
        return (boat && a) || (half && b);
    }

    public static boolean isWaterExit(double deltaY, double thr) {
        double a = 0.3400000110268593;
        return (Math.abs(deltaY-a) < thr);
    }

    public static boolean isClimbing(double deltaY, double thr) {
        boolean a = isClimbUp(deltaY, thr);
        boolean b = isClimbDown(deltaY, thr);
        return a || b;
    }

    public static boolean isClimbUp(double deltaY, double thr) {
        double a = 0.1176000022888175;
        return (Math.abs(deltaY-a) < thr);
    }

    public static boolean isBoatCollision(double deltaY, double thr) {
        return isBoatGroundCollision(deltaY, thr) || isBoatJumpCollision(deltaY, thr);
    }

    public static boolean isBoatGroundCollision(double deltaY, double thr) {
        double a = 0.5625;
        return (Math.abs(deltaY-a) < thr);
    }

    public static boolean isBoatJumpCollision(double deltaY, double thr) {
        double a = 0.5380759117863079;
        return (Math.abs(deltaY-a) < thr);
    }

    public static boolean isClimbDown(double deltaY, double thr) {
        double a = -0.15000000596046448;
        return (Math.abs(deltaY-a) < thr);
    }

    public static boolean isHalf(double deltaY, double thr) {
        double a = 0.5;
        return (Math.abs(deltaY-a) < thr);
    }

    public static boolean isSlowMovement(double at, double xzLast, double xz, double thr) {
        return isSlowMovementT1(at, xzLast, xz, thr)
                || isSlowMovementT2(at, xzLast, xz, thr);
    }

    public static boolean isSlowMovementT1(double at, double xzLast, double xz, double thr) {
        boolean tick1 = isWalk(at, xzLast, thr) && isSneak(at, xz, thr);
        return tick1;
    }

    public static boolean isSlowMovementT2(double at, double xzLast, double xz, double thr) {
        boolean tick2 = isSneak(at, xzLast, thr) && isShield(at, xz, thr);
        return tick2;
    }
}
