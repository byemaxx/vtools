package com.omarea.utils;

public final class AndroidPackageName {
    public static boolean isValid(String value) {
        return value != null && value.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)*");
    }
}
