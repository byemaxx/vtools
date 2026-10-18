package com.omarea.scene_mode;

public class TriggerExecutionPolicyTest {
    public static void main(String[] args) {
        assert !TriggerExecutionPolicy.canExecute(false, true, false, 0, 1439, 500);
        assert !TriggerExecutionPolicy.canExecute(true, false, false, 0, 1439, 500);
        assert TriggerExecutionPolicy.canExecute(true, true, false, 0, 0, 500);
        assert TriggerExecutionPolicy.canExecute(true, true, true, 420, 540, 420);
        assert TriggerExecutionPolicy.canExecute(true, true, true, 420, 540, 540);
        assert !TriggerExecutionPolicy.canExecute(true, true, true, 420, 540, 541);
        assert TriggerExecutionPolicy.canExecute(true, true, true, 1380, 420, 0);
        assert TriggerExecutionPolicy.canExecute(true, true, true, 1380, 420, 1439);
        assert !TriggerExecutionPolicy.canExecute(true, true, true, 1380, 420, 500);
        assert !TriggerExecutionPolicy.canExecute(true, true, true, 420, 420, 420);
        assert !TriggerExecutionPolicy.canExecute(true, true, true, -1, 420, 0);
        assert !TriggerExecutionPolicy.canExecute(true, true, true, 0, 1440, 0);
        System.out.println("Trigger enabled/event/time-window checks passed");
    }
}
