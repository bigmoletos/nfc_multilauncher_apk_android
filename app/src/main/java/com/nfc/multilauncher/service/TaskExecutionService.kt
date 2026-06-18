package com.nfc.multilauncher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.nfc.multilauncher.R
import com.nfc.multilauncher.data.model.Task
import com.nfc.multilauncher.domain.ActionResult
import com.nfc.multilauncher.domain.ExecutionProgress
import com.nfc.multilauncher.domain.TaskExecutor
import com.nfc.multilauncher.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TaskExecutionService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val gson = Gson()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskJson = intent?.getStringExtra(EXTRA_TASK_JSON) ?: run {
            stopSelf()
            return START_NOT_STICKY
        }

        val task = runCatching { gson.fromJson(taskJson, Task::class.java) }.getOrNull() ?: run {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Démarrage : ${task.name}", 0, 1))

        scope.launch {
            val executor = TaskExecutor(applicationContext)
            executor.executeSequentially(task) { progress ->
                val notif = buildProgressNotification(task.name, progress)
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, notif)
            }
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification("Tâche terminée : ${task.name}", 1, 1))
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun buildProgressNotification(taskName: String, progress: ExecutionProgress): Notification {
        val actionLabel = when {
            progress.currentAction.packageName != null -> progress.currentAction.packageName
            progress.currentAction.url != null -> "URL"
            else -> progress.currentAction.type.name
        }
        val text = "Action ${progress.currentIndex + 1}/${progress.total} : $actionLabel"
        return buildNotification(text, progress.currentIndex + 1, progress.total)
    }

    private fun buildNotification(text: String, progress: Int, max: Int): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NFC Multi-App Launcher")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(max, progress, false)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Exécution de tâches NFC",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifications pendant l'exécution des tâches NFC"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "nfc_task_execution"
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_TASK_JSON = "extra_task_json"

        fun startFor(context: Context, task: Task) {
            val gson = Gson()
            val intent = Intent(context, TaskExecutionService::class.java).apply {
                putExtra(EXTRA_TASK_JSON, gson.toJson(task))
            }
            context.startForegroundService(intent)
        }
    }
}
