package com.omarea.utils

import android.content.SharedPreferences
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import com.omarea.Scene
import com.omarea.data.GlobalStatus
import com.omarea.store.SpfConfig
import java.util.Locale

object BatteryPower {
    private var lastVoltageRetry = -5000L

    @Synchronized
    private fun recoverVoltage() {
        if (GlobalStatus.batteryVoltage > 0 && GlobalStatus.batteryVoltage.isFinite()) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastVoltageRetry < 5000) return
        lastVoltageRetry = now
        val snapshot = Scene.context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val voltage = (snapshot?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000.0
        if (voltage > 0) GlobalStatus.batteryVoltage = voltage
    }

    fun currentMa(raw: Long?, preferences: SharedPreferences): Double =
        BatteryPowerMath.currentMa(raw ?: Long.MIN_VALUE, preferences.getInt(
            SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT, SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT_DEFAULT))

    fun format(raw: Long?, preferences: SharedPreferences): String = format(
        raw, preferences.getInt(SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT,
            SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT_DEFAULT),
        preferences.getFloat(SpfConfig.GLOBAL_SPF_POWER_FACTOR, 1f))

    fun format(raw: Long?, divisor: Int, factor: Float): String {
        recoverVoltage()
        val watts = BatteryPowerMath.watts(
            BatteryPowerMath.currentMa(raw ?: Long.MIN_VALUE, divisor), GlobalStatus.batteryVoltage, factor)
        return if (watts.isFinite()) String.format(Locale.getDefault(), "%+.2fW", watts) else "--W"
    }
}
