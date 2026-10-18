package com.omarea.scene_mode;

public class TaskExecutionPolicyTest {
    public static void main(String[] args) {
        java.util.Calendar clock = java.util.Calendar.getInstance();
        clock.set(2026, 9, 18, 10, 30, 0);
        clock.set(java.util.Calendar.MILLISECOND, 0);
        long now = clock.getTimeInMillis();
        assert TaskExecutionPolicy.nextTrigger(631, now) == now + 60_000;
        assert TaskExecutionPolicy.nextTrigger(630, now) > now + 23 * 3600_000L;
        assert TaskExecutionPolicy.nextTrigger(629, now) > now;
        assert TaskExecutionPolicy.nextTrigger(631, now + 30_000) == now + 60_000;
        boolean rejected = false;
        try { TaskExecutionPolicy.nextTrigger(1440, now); } catch (IllegalArgumentException expected) { rejected = true; }
        assert rejected;
        assert !TaskExecutionPolicy.canExecute(false, 0, 100);
        assert !TaskExecutionPolicy.canExecute(false, 101, 100);
        assert !TaskExecutionPolicy.canExecute(true, 99, 100);
        assert !TaskExecutionPolicy.canExecute(true, 100, 100);
        assert TaskExecutionPolicy.canExecute(true, 101, 100);
        assert TaskExecutionPolicy.canExecute(true, 0, 100);
    }
}
