package com.omarea.vtools.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.omarea.data.EventType
import com.omarea.library.calculator.GetUpTime
import com.omarea.scene_mode.TaskActionsExecutor
import com.omarea.scene_mode.TriggerExecutionPolicy
import com.omarea.store.TriggerStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TriggerWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val event = try {
            EventType.valueOf(inputData.getString("event") ?: return@withContext Result.failure())
        } catch (_: IllegalArgumentException) {
            return@withContext Result.failure()
        }
        val ids = inputData.getStringArray("triggers") ?: return@withContext Result.failure()
        val storage = TriggerStorage(applicationContext)
        var succeeded = true
        for (id in ids.distinct()) {
            try {
                val trigger = storage.load(id) ?: continue
                if (!TriggerExecutionPolicy.canExecute(trigger.enabled, trigger.events?.contains(event) == true,
                        trigger.timeLimited, trigger.timeStart, trigger.timeEnd, GetUpTime(0).currentTime)) continue
                TaskActionsExecutor(trigger.taskActions, trigger.customTaskActions, applicationContext).run()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("SceneTrigger", "Trigger execution failed: $id", error)
                succeeded = false
            }
        }
        if (succeeded) Result.success() else Result.failure()
    }
}
