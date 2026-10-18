package com.omarea.utils;

public final class TelemetryMathTest {
    private static void equal(double actual, double expected) {
        assert Math.abs(actual - expected) < 0.000001 : actual + " != " + expected;
    }
    public static void main(String[] args) {
        double[] energy = TelemetryMath.energy(new long[]{0, 1000, 3000}, new double[]{2, 4, 4});
        equal(energy[0], 11.0 / 3600); equal(energy[1], 11.0 / 3); equal(energy[2], 3000);
        energy = TelemetryMath.energy(new long[]{0, 1000, 20000, 21000}, new double[]{-4, -4, -2, -2});
        equal(energy[0], -6.0 / 3600); equal(energy[2], 2000);
        energy = TelemetryMath.energy(new long[]{0, 1000, 2000}, new double[]{4, Double.NaN, 4});
        assert Double.isNaN(energy[0]); assert Double.isNaN(energy[1]); equal(energy[2], 0);
        assert Double.isNaN(TelemetryMath.energy(new long[]{0}, new double[]{4})[0]);
        assert Double.isNaN(TelemetryMath.energy(new long[]{1000, 0}, new double[]{4, 4})[0]);
        String output = "16666666\n1 1000000000 1\n1 1016000000 1\n1 1032000000 1\n" +
                "1 1032000000 1\n1 0 1\n1 9223372036854775807 1\nbad row here\n";
        TelemetryMath.Frames first = TelemetryMath.frames(output, 0);
        assert first.durations.isEmpty(); assert first.last == 1032000000L;
        TelemetryMath.Frames next = TelemetryMath.frames(output, 1000000000L);
        assert next.durations.size() == 2; equal(next.durations.get(0), 16);
        assert next.endTimes.get(1) == 1032000000L; assert !next.gap;
        assert TelemetryMath.frames(output, next.last).durations.isEmpty();
        TelemetryMath.Frames gap = TelemetryMath.frames(output, 900000000L);
        assert gap.gap; assert gap.durations.size() == 2;
        TelemetryMath.Frames changed = TelemetryMath.frames("1 1000000000 1\n1 1032000000 1\n1 1048000000 1", 1016000000L);
        assert changed.gap; assert changed.durations.size() == 1; equal(changed.durations.get(0), 16);
        assert TelemetryMath.frames("Permission denied", 0).last == 0;
        System.out.println("Telemetry energy / frame checks passed");
    }
}
