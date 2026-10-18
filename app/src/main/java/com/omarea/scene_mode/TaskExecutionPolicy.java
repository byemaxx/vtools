package com.omarea.scene_mode;

public final class TaskExecutionPolicy {
    private TaskExecutionPolicy() {}

    public static long nextTrigger(int minuteOfDay, long now) {
        if (minuteOfDay < 0 || minuteOfDay >= 1440) throw new IllegalArgumentException("Invalid task time");
        java.util.Calendar next = java.util.Calendar.getInstance();
        next.setTimeInMillis(now);
        next.set(java.util.Calendar.HOUR_OF_DAY, minuteOfDay / 60);
        next.set(java.util.Calendar.MINUTE, minuteOfDay % 60);
        next.set(java.util.Calendar.SECOND, 0);
        next.set(java.util.Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= now) next.add(java.util.Calendar.DAY_OF_YEAR, 1);
        return next.getTimeInMillis();
    }

    public static boolean canExecute(boolean enabled, long expiry, long now) {
        return enabled && (expiry <= 0 || expiry > now);
    }
}
