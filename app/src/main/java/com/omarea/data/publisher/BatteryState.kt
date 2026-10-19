package com.omarea.data.publisher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.omarea.data.EventBus
import com.omarea.data.EventType
import com.omarea.data.GlobalStatus
import com.omarea.data.customer.BatteryReceiver

class BatteryState(private val applicationContext: Context) : BroadcastReceiver() {

    // 最后的电量百分比（用于判断是否有电量变化）
    private var lastCapacity = 0

    // 最后的充电状态（用于判断是否有状态）
    private var lastStatus = BatteryManager.BATTERY_STATUS_UNKNOWN

    // bms
    private var batteryManager: BatteryManager? = null

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == null) {
            return
        }

        val pendingResult = goAsync()
        try {
            // Power/boot/low-battery events need not contain a battery snapshot.
            // Read the sticky snapshot instead of replacing valid readings with defaults.
            val snapshot = if (action == Intent.ACTION_BATTERY_CHANGED) intent else
                applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: intent
            val status = snapshot.getIntExtra(BatteryManager.EXTRA_STATUS, GlobalStatus.batteryStatus)
            var capacity = snapshot.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val temp = snapshot.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            val voltage = snapshot.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000.0
            if (capacity == -1) {
                if (batteryManager == null) {
                    batteryManager = context.applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                }
                capacity = batteryManager!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            }

            GlobalStatus.batteryStatus = status
            if (capacity in 0..100) GlobalStatus.batteryCapacity = capacity
            if (voltage > 0) GlobalStatus.batteryVoltage = voltage
            GlobalStatus.batteryCycleCount = snapshot.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, GlobalStatus.batteryCycleCount)
            if (temp != Int.MIN_VALUE) GlobalStatus.setBatteryTemperature(temp / 10.0)

            // 判断是否在充电
            // val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            // val onCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC || chargePlug == BatteryManager.BATTERY_PLUGGED_USB || chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS

            if (status != BatteryManager.BATTERY_STATUS_UNKNOWN && status != lastStatus) {
                lastStatus = status
                if (status.equals(BatteryManager.BATTERY_STATUS_CHARGING)) {
                    EventBus.publish(EventType.POWER_CONNECTED)
                } else if (status.equals(BatteryManager.BATTERY_STATUS_FULL)) {
                    EventBus.publish(EventType.BATTERY_FULL)
                }
            }

            if (action == Intent.ACTION_BATTERY_LOW) {
                EventBus.publish(EventType.BATTERY_LOW)
            } else if (action == Intent.ACTION_BATTERY_CHANGED) {
                EventBus.publish(EventType.BATTERY_CHANGED)
            } else if (action == Intent.ACTION_POWER_DISCONNECTED) {
                EventBus.publish(EventType.POWER_DISCONNECTED)
            } else if (action == Intent.ACTION_POWER_CONNECTED) {
                EventBus.publish(EventType.POWER_CONNECTED)
            }
            if (capacity in 0..100 && lastCapacity != capacity) {
                lastCapacity = capacity
                EventBus.publish(EventType.BATTERY_CAPACITY_CHANGED)
            }
        } catch (ex: Exception) {
        } finally {
            pendingResult.finish()
        }
    }

    fun registerReceiver() {
        val batteryChangedReciver = this
        applicationContext.run {
            //启动完成
            registerReceiver(batteryChangedReciver, IntentFilter(Intent.ACTION_BOOT_COMPLETED))
            //电源连接
            registerReceiver(batteryChangedReciver, IntentFilter(Intent.ACTION_POWER_CONNECTED))
            //电源断开
            registerReceiver(batteryChangedReciver, IntentFilter(Intent.ACTION_POWER_DISCONNECTED))
            //电量变化
            registerReceiver(batteryChangedReciver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            //电量不足
            registerReceiver(batteryChangedReciver, IntentFilter(Intent.ACTION_BATTERY_LOW))
        }
        // 充电控制模块
        EventBus.subscribe(BatteryReceiver(applicationContext))
    }
}
