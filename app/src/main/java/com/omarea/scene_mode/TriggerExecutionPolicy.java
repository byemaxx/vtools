package com.omarea.scene_mode;

public final class TriggerExecutionPolicy {
    private TriggerExecutionPolicy() {}

    public static boolean canExecute(boolean enabled, boolean eventMatches, boolean limited,
                                     int start, int end, int minute) {
        if (!enabled || !eventMatches) return false;
        if (!limited) return true;
        if (start < 0 || start >= 1440 || end < 0 || end >= 1440 || minute < 0 || minute >= 1440) return false;
        return start < end ? minute >= start && minute <= end
                : start > end && (minute >= start || minute <= end);
    }
}
