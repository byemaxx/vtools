package com.omarea.utils;

public class MiniMonitorPositionTest {
    public static void main(String[] args) {
        assert MiniMonitorPosition.clampOffset(0, 300, 400, 0, 1000) == 0;
        assert MiniMonitorPosition.clampOffset(-400, -100, 400, 0, 1000) == -300;
        assert MiniMonitorPosition.clampOffset(400, 700, 400, 0, 1000) == 300;
        assert MiniMonitorPosition.clampOffset(0, 0, 40, 160, 3104) == 160;
        assert MiniMonitorPosition.clampOffset(3100, 3100, 40, 160, 3104) == 3064;
        assert MiniMonitorPosition.clampOffset(0, -100, 1200, 0, 1000) == 100;
        assert MiniMonitorPosition.clampOffset(300, 500, 700, 20, 720) == -180;
        System.out.println("Mini bar edge, inset, rotation and oversized-window checks passed");
    }
}
