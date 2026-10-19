package com.omarea.utils;

public final class MiniMonitorPosition {
    private MiniMonitorPosition() {}

    public static int clampOffset(int offset, int screenPosition, int size, int minimum, int maximum) {
        int last = Math.max(minimum, maximum - size);
        int target = Math.max(minimum, Math.min(screenPosition, last));
        return offset + target - screenPosition;
    }
}
