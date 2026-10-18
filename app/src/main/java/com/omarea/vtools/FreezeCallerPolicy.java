package com.omarea.vtools;

import java.util.Arrays;

final class FreezeCallerPolicy {
    static boolean allows(String target, String source, String[] callers, String[] trusted, boolean optedIn) {
        return target != null && target.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)*")
                && source != null && callers != null && Arrays.asList(callers).contains(source)
                && (optedIn || Arrays.asList(trusted).contains(source));
    }
}
