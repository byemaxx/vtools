package com.omarea.scene_mode

import android.content.Context
import com.omarea.data.EventType
import com.omarea.data.IEventReceiver

class TriggerIEventMonitor(private val context: Context, override val isAsync: Boolean = false) : IEventReceiver {
    private val triggerListConfig = context.getSharedPreferences("scene_trigger_list", Context.MODE_PRIVATE)

    override fun onReceive(eventType: EventType, data: HashMap<String, Any>?) {
        val triggers = triggerListConfig.all.filter { (_, value) ->
            (value as? String)?.split(",")?.contains(eventType.name) == true
        }.keys
        if (triggers.isEmpty()) return
        val work = androidx.work.OneTimeWorkRequestBuilder<com.omarea.vtools.workers.TriggerWorker>()
            .setInputData(androidx.work.workDataOf("event" to eventType.name, "triggers" to triggers.toTypedArray()))
            .build()
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "scene-trigger-actions", androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE, work)
    }

    override fun onSubscribe() {

    }

    override fun onUnsubscribe() {

    }

    override fun eventFilter(eventType: EventType): Boolean {
        when (eventType) {
            EventType.SCREEN_ON,
            EventType.SCREEN_OFF,
            EventType.BOOT_COMPLETED,
            EventType.BATTERY_LOW,
            EventType.POWER_CONNECTED,
            EventType.POWER_DISCONNECTED -> {
                return true
            }
            else -> return false
        }
    }
}

