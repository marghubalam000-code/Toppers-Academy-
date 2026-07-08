package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DailySummaryReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DAILY_SUMMARY = "com.example.ACTION_DAILY_SUMMARY"
        private const val PREFS_NAME = "toppers_summary_prefs"
        const val KEY_ENABLED = "daily_summary_enabled"
        const val KEY_HOUR = "daily_summary_hour"
        const val KEY_MINUTE = "daily_summary_minute"

        fun scheduleDailySummary(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean(KEY_ENABLED, true) // Enabled by default
            val hour = prefs.getInt(KEY_HOUR, 16) // 4 PM default
            val minute = prefs.getInt(KEY_MINUTE, 0) // 0 minute default

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, DailySummaryReceiver::class.java).apply {
                action = ACTION_DAILY_SUMMARY
            }

            // Create PendingIntent
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (!isEnabled) {
                alarmManager.cancel(pendingIntent)
                Log.d("DailySummaryReceiver", "Daily summary disabled, alarm cancelled.")
                return
            }

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If the time has already passed today, schedule for tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            try {
                // Set the alarm (using non-exact for compatibility without SCHEDULE_EXACT_ALARM permission)
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d("DailySummaryReceiver", "Daily summary scheduled successfully for ${calendar.time}")
            } catch (e: Exception) {
                Log.e("DailySummaryReceiver", "Failed to schedule daily summary: ${e.message}", e)
            }
        }

        fun cancelDailySummary(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, DailySummaryReceiver::class.java).apply {
                action = ACTION_DAILY_SUMMARY
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d("DailySummaryReceiver", "Daily summary cancelled.")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("DailySummaryReceiver", "Received intent action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            // Reschedule upon device reboot
            scheduleDailySummary(context)
        } else if (action == ACTION_DAILY_SUMMARY) {
            val pendingResult = goAsync()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    db.attendanceDao().getAttendanceForDate(todayDate).take(1).collect { records ->
                        val totalRecords = records.size
                        val presentCount = records.count { it.status.equals("present", ignoreCase = true) }

                        val notificationHelper = NotificationHelper(context)
                        notificationHelper.sendDailySummaryNotification(presentCount, totalRecords, todayDate)

                        // Reschedule for tomorrow
                        scheduleDailySummary(context)
                    }
                } catch (e: Exception) {
                    Log.e("DailySummaryReceiver", "Error processing daily summary", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
