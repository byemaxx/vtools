package com.omarea.library.shell

import com.omarea.common.shell.KernelProrp
import com.omarea.data.GlobalStatus
import com.omarea.permissions.CheckRootStatus

class BatteryDetails {
    private var lastRead = 0L
    private var cached = ""
    fun read(): String {
        if (System.currentTimeMillis() - lastRead < 60000) return cached
        lastRead = System.currentTimeMillis()
        val values = if (CheckRootStatus.lastCheckResult) KernelProrp.getProp("/sys/class/power_supply/battery/uevent")
            .lineSequence().mapNotNull { line ->
                val parts = line.split('=', limit = 2)
                if(parts.size == 2) parts[0] to parts[1].trim().toLongOrNull() else null
            }.toMap() else emptyMap()
        val cycle = values["POWER_SUPPLY_CYCLE_COUNT"]?.takeIf { it >= 0 } ?: GlobalStatus.batteryCycleCount.toLong().takeIf { it >= 0 }
        fun capacity(key: String): String = values[key]?.takeIf { it > 0 }?.let { "%.0f mAh".format(it / 1000.0) } ?: "--"
        cached = "Charge cycles: ${cycle ?: "--"}\nReported full capacity: ${capacity("POWER_SUPPLY_CHARGE_FULL")}\n" +
            "Reported design capacity: ${capacity("POWER_SUPPLY_CHARGE_FULL_DESIGN")}\n" +
            "Reported values; cell/pack capacity units may differ. No health estimate."
        return cached
    }
}
