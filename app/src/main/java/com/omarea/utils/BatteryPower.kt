package com.omarea.utils

import android.content.SharedPreferences
import com.omarea.data.GlobalStatus
import com.omarea.store.SpfConfig
import java.util.Locale

object BatteryPower {
    fun currentMa(raw: Long?, preferences: SharedPreferences): Double =
        BatteryPowerMath.currentMa(raw ?: Long.MIN_VALUE, preferences.getInt(
            SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT, SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT_DEFAULT))

    fun format(raw: Long?, preferences: SharedPreferences): String = format(
        raw, preferences.getInt(SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT,
            SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT_DEFAULT),
        preferences.getFloat(SpfConfig.GLOBAL_SPF_POWER_FACTOR, 1f))

    fun format(raw: Long?, divisor: Int, factor: Float): String {
        val watts = BatteryPowerMath.watts(
            BatteryPowerMath.currentMa(raw ?: Long.MIN_VALUE, divisor), GlobalStatus.batteryVoltage, factor)
        return if (watts.isFinite()) String.format(Locale.getDefault(), "%+.2fW", watts) else "--W"
    }
}
