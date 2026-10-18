package com.omarea.utils;

public class AndroidPackageNameTest {
    public static void main(String[] args) {
        assert AndroidPackageName.isValid("android");
        assert AndroidPackageName.isValid("com.example.app_1");
        for (String input : new String[]{null, "", "com.app;reboot", "com.app\nreboot", "$(id)", "--user 0", "a/b", "a..b", "a.", ".a"}) {
            assert !AndroidPackageName.isValid(input) : input;
        }
        System.out.println("Root package-name boundary checks passed");
    }
}
