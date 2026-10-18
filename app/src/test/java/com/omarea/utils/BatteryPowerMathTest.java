package com.omarea.utils;

// Run with javac and java -ea; no Android runtime is needed.
public final class BatteryPowerMathTest {
    private static void equal(double actual, double expected) {
        assert Math.abs(actual - expected) < 0.000001 : actual + " != " + expected;
    }

    public static void main(String[] args) {
        double current = BatteryPowerMath.currentMa(-1500000, -1000);
        equal(current, 1500);
        equal(BatteryPowerMath.watts(current, 4, 1), 6);
        equal(BatteryPowerMath.watts(current, 4, 2), 12);
        equal(BatteryPowerMath.watts(current, 8, 1), 12);
        equal(BatteryPowerMath.watts(3000, 8, 0.5f), 12);
        equal(BatteryPowerMath.watts(-1500, 4, 2), -12);
        equal(BatteryPowerMath.watts(BatteryPowerMath.currentMa(1500000, 1000), 4, 1), 6);
        equal(BatteryPowerMath.watts(BatteryPowerMath.currentMa(-1500000, 1000), 4, 1), -6);
        equal(BatteryPowerMath.currentMa(500, 1000), 0.5);
        equal(BatteryPowerMath.currentMa(1500, 1), 1500);
        equal(BatteryPowerMath.watts(0, 4, 1), 0);
        assert Double.isNaN(BatteryPowerMath.currentMa(Long.MIN_VALUE, 1000));
        assert Double.isNaN(BatteryPowerMath.currentMa(Long.MAX_VALUE, 1000));
        assert Double.isNaN(BatteryPowerMath.currentMa(1000, 0));
        assert Double.isNaN(BatteryPowerMath.watts(Double.NaN, 4, 1));
        assert Double.isNaN(BatteryPowerMath.watts(1000, 0, 1));
        assert Double.isNaN(BatteryPowerMath.watts(1000, -1, 1));
        assert Double.isNaN(BatteryPowerMath.watts(1000, Double.POSITIVE_INFINITY, 1));
        assert Double.isNaN(BatteryPowerMath.watts(1000, 4, 3));
        System.out.println("Battery power checks passed");
    }
}
