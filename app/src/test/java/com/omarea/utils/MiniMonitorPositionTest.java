package com.omarea.utils;

public class MiniMonitorPositionTest {
    public static void main(String[] args) {
        assert MiniMonitorPosition.clampOffset(0, 300, 400, 0, 1000) == 0;
        assert MiniMonitorPosition.clampOffset(-400, -100, 400, 0, 1000) == -300;
        assert MiniMonitorPosition.clampOffset(400, 700, 400, 0, 1000) == 300;
        assert MiniMonitorPosition.clampOffset(0, 0, 40, 160, 3104) == 160;
        // Landscape immersive mode: hidden status bar must allow the top edge.
        assert MiniMonitorPosition.clampOffset(-200, -40, 71, 0, 1440) == -160;
        assert MiniMonitorPosition.clampOffset(0, 0, 71, 0, 1440) == 0;
        assert MiniMonitorPosition.clampOffset(3100, 3100, 40, 160, 3104) == 3064;
        assert MiniMonitorPosition.clampOffset(0, -100, 1200, 0, 1000) == 100;
        assert MiniMonitorPosition.clampOffset(300, 500, 700, 20, 720) == -180;
        System.out.println("Mini bar edge, inset, rotation and oversized-window checks passed");
    }
}
