package com.omarea.vtools;

public class FreezeCallerPolicyTest {
    public static void main(String[] args) {
        String[] trusted = {"com.android.settings"};
        String[] caller = {"com.example.other"};
        assert !FreezeCallerPolicy.allows("com.example.app", trusted[0], caller, trusted, false);
        assert !FreezeCallerPolicy.allows("com.example.app", trusted[0], caller, trusted, true);
        assert !FreezeCallerPolicy.allows("com.example.app", caller[0], caller, trusted, false);
        assert FreezeCallerPolicy.allows("com.example.app", caller[0], caller, trusted, true);
        assert FreezeCallerPolicy.allows("com.example.app", trusted[0], trusted, trusted, false);
        assert !FreezeCallerPolicy.allows("com.example.app;reboot", trusted[0], trusted, trusted, false);
        assert !FreezeCallerPolicy.allows("", trusted[0], trusted, trusted, false);
        assert !FreezeCallerPolicy.allows("com.example.app", trusted[0], null, trusted, true);
    }
}
