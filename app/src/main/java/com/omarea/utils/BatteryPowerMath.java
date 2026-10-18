package com.omarea.utils;

public final class BatteryPowerMath {
    private BatteryPowerMath() {}

    public static double currentMa(long raw, int divisor) {
        if (raw == Long.MIN_VALUE || raw == Long.MAX_VALUE || divisor == 0) {
            return Double.NaN;
        }
        return raw / (double) divisor;
    }

    public static double watts(double currentMa, double voltage, float factor) {
        if (!Double.isFinite(currentMa) || !Double.isFinite(voltage) || voltage <= 0 ||
                (factor != 0.5f && factor != 1f && factor != 2f)) {
            return Double.NaN;
        }
        return currentMa * voltage * factor / 1000.0;
    }
}
