package com.omarea.utils;

import java.util.ArrayList;
import java.util.TreeSet;

public final class TelemetryMath {
    private TelemetryMath() {}

    // Do not integrate across missing samples or a suspended recorder.
    public static double[] energy(long[] times, double[] watts) {
        double wh = 0, coveredMs = 0;
        for (int i = 1; i < times.length; i++) {
            long dt = times[i] - times[i - 1];
            if (dt > 0 && dt <= 10000 && Double.isFinite(watts[i - 1]) && Double.isFinite(watts[i])) {
                wh += (watts[i - 1] + watts[i]) * 0.5 * dt / 3600000.0;
                coveredMs += dt;
            }
        }
        return new double[]{coveredMs > 0 ? wh : Double.NaN,
                coveredMs > 0 ? wh * 3600000 / coveredMs : Double.NaN, coveredMs};
    }

    public static final class Frames {
        public long last;
        public boolean gap;
        public final ArrayList<Double> durations = new ArrayList<>();
        public final ArrayList<Long> endTimes = new ArrayList<>();
    }

    public static Frames frames(String text, long previous) {
        TreeSet<Long> timestamps = new TreeSet<>();
        for (String line : text.split("\n")) {
            String[] values = line.trim().split("\\s+");
            if (values.length != 3) continue;
            try {
                long actual = Long.parseLong(values[1]);
                if (actual > 0 && actual != Long.MAX_VALUE) timestamps.add(actual);
            } catch (NumberFormatException ignored) {}
        }
        Frames result = new Frames();
        result.last = previous;
        if (timestamps.isEmpty()) return result;
        long cutoff = previous;
        boolean collecting = previous > 0;
        if (previous > 0 && !timestamps.contains(previous) && timestamps.last() > previous) {
            result.gap = true;
            previous = 0;
        }
        long last = previous;
        for (long timestamp : timestamps) {
            if (timestamp <= cutoff) continue;
            if (last > 0 && collecting) {
                result.durations.add((timestamp - last) / 1000000.0);
                result.endTimes.add(timestamp);
            }
            last = timestamp;
        }
        result.last = Math.max(result.last, timestamps.last());
        return result;
    }
}
