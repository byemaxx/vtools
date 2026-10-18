package com.omarea.vtools.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.omarea.scene_mode.SceneTaskIntentService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DelayedTimingTaskWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString("taskId") ?: return@withContext Result.failure()
        try {
            SceneTaskIntentService.executeTask(applicationContext, id)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.e("SceneTask", "Delayed task failed", error)
            Result.failure()
        }
    }
}
