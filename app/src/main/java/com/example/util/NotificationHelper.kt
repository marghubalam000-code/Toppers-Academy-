package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log

class NotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "toppers_attendance_notifications"
        const val CHANNEL_NAME = "Attendance Alerts & Parent Notifications"
        const val CHANNEL_DESC = "Real-time notifications sent to parents for absent/present status"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendParentAlertNotification(studentName: String, rollNumber: String, isPresent: Boolean, date: String) {
        val statusText = if (isPresent) "PRESENT" else "ABSENT"
        val message = "Toppers Academy Alert: Parent of Roll No. $rollNumber ($studentName) notified that student is marked $statusText for $date."
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // System default icon
            .setContentTitle("Toppers Academy Alert")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        getLogoBitmap()?.let { builder.setLargeIcon(it) }
        
        try {
            with(NotificationManagerCompat.from(context)) {
                // Unique notification ID based on student name and status hash
                val notificationId = (studentName.hashCode() + statusText.hashCode() + date.hashCode()).hashCode()
                // Check if we have permission to post notifications (handled in UI, but catch if fails)
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Permission for notifications is missing: ${e.message}")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to send notification: ${e.message}", e)
        }
    }

    fun sendStudentAttendanceNotification(studentName: String, status: String, date: String, subject: String) {
        val message = "Hi $studentName, your attendance for $subject on $date has been marked as: $status."
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔔 Attendance Update")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        getLogoBitmap()?.let { builder.setLargeIcon(it) }
        
        try {
            with(NotificationManagerCompat.from(context)) {
                val notificationId = (studentName.hashCode() + status.hashCode() + date.hashCode() + subject.hashCode()).hashCode()
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Permission for notifications is missing: ${e.message}")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to send student attendance notification: ${e.message}", e)
        }
    }

    fun sendDailySummaryNotification(presentCount: Int, totalCount: Int, date: String) {
        val title = "Toppers Academy Daily Summary"
        val message = if (totalCount > 0) {
            "Today summary: $presentCount students are marked PRESENT out of $totalCount submitted records for $date."
        } else {
            "No attendance logs were submitted today ($date). Remember to mark and sync class records."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        getLogoBitmap()?.let { builder.setLargeIcon(it) }
        
        try {
            with(NotificationManagerCompat.from(context)) {
                val notificationId = 9999
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Permission for notifications is missing: ${e.message}")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to send summary notification: ${e.message}", e)
        }
    }

    fun sendOtpNotification(email: String, otp: String) {
        val title = "🔑 Admin Login OTP"
        val message = "Your Toppers Academy login verification code for $email is: $otp"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        getLogoBitmap()?.let { builder.setLargeIcon(it) }
        
        try {
            with(NotificationManagerCompat.from(context)) {
                val notificationId = 11111
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Permission for notifications is missing: ${e.message}")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to send OTP notification: ${e.message}", e)
        }
    }

    fun sendExamScheduleNotification(title: String, details: String) {
        val message = "📅 Exam Scheduled: $title\n$details"
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📅 New Exam Scheduled")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        getLogoBitmap()?.let { builder.setLargeIcon(it) }
        
        try {
            with(NotificationManagerCompat.from(context)) {
                val notificationId = (title.hashCode() + details.hashCode()).hashCode()
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Permission for notifications is missing: ${e.message}")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to send exam notification: ${e.message}", e)
        }
    }

    fun sendAnnouncementNotification(title: String, text: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📢 Announcement: $title")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        getLogoBitmap()?.let { builder.setLargeIcon(it) }
        
        try {
            with(NotificationManagerCompat.from(context)) {
                val notificationId = (title.hashCode() + text.hashCode()).hashCode()
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Permission for notifications is missing: ${e.message}")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to send announcement notification: ${e.message}", e)
        }
    }

    fun sendLeaveNotification(studentName: String, action: String, status: String) {
        val title = "🍁 Leave Request Update"
        val message = "Hi $studentName, your leave request has been $action. Status: $status."
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        getLogoBitmap()?.let { builder.setLargeIcon(it) }
        
        try {
            with(NotificationManagerCompat.from(context)) {
                val notificationId = (studentName.hashCode() + action.hashCode() + status.hashCode() + System.currentTimeMillis().hashCode()).hashCode()
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Permission for notifications is missing: ${e.message}")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to send leave notification: ${e.message}", e)
        }
    }

    fun sendFeeReminderNotification(studentName: String, amount: Int) {
        val title = "⚠️ Toppers Academy: Fee Payment Reminder"
        val message = "Dear $studentName, you have a pending fee balance of ₹${String.format("%,d", amount)}. Please complete the payment at your earliest convenience."
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        getLogoBitmap()?.let { builder.setLargeIcon(it) }
        
        try {
            with(NotificationManagerCompat.from(context)) {
                val notificationId = (studentName.hashCode() + amount + System.currentTimeMillis().hashCode()).hashCode()
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Permission for notifications is missing: ${e.message}")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to send fee reminder notification: ${e.message}", e)
        }
    }

    fun sendAdminPaymentNotification(studentName: String, amount: Double, title: String) {
        val message = "💸 Student $studentName has submitted a payment of ₹$amount for '$title'. Please review and verify the transaction."
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💸 New Fee Payment Submitted")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        getLogoBitmap()?.let { builder.setLargeIcon(it) }

        try {
            with(NotificationManagerCompat.from(context)) {
                val notificationId = (studentName.hashCode() + amount.hashCode() + title.hashCode() + System.currentTimeMillis().hashCode()).hashCode()
                notify(notificationId, builder.build())
            }
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to send admin payment notification: ${e.message}", e)
        }
    }

    fun sendStudentFeeIssuedNotification(title: String, amount: Double, dueDate: String) {
        val message = "🔔 A new fee of ₹$amount for '$title' has been issued. Due date: $dueDate. Please pay using UPI or Scanner."
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔔 New Fee Issued / शुल्क जारी")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        getLogoBitmap()?.let { builder.setLargeIcon(it) }

        try {
            with(NotificationManagerCompat.from(context)) {
                val notificationId = (title.hashCode() + amount.hashCode() + dueDate.hashCode() + System.currentTimeMillis().hashCode()).hashCode()
                notify(notificationId, builder.build())
            }
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to send student fee issued notification: ${e.message}", e)
        }
    }

    fun sendStudentFeeStatusNotification(title: String, status: String, rejectionReason: String) {
        val statusEmoji = when (status) {
            "Paid" -> "✅ APPROVED / स्वीकृत"
            "Rejected" -> "❌ REJECTED / अस्वीकृत"
            "Pending" -> "⏳ PENDING / लंबित"
            else -> status
        }
        val reasonText = if (status == "Rejected" && rejectionReason.isNotEmpty()) "\nReason: $rejectionReason" else ""
        val message = "Your fee payment for '$title' status is: $statusEmoji.$reasonText"
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💰 Fee Payment Status Update")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        getLogoBitmap()?.let { builder.setLargeIcon(it) }

        try {
            with(NotificationManagerCompat.from(context)) {
                val notificationId = (title.hashCode() + status.hashCode() + System.currentTimeMillis().hashCode()).hashCode()
                notify(notificationId, builder.build())
            }
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to send student fee status notification: ${e.message}", e)
        }
    }

    private fun getLogoBitmap(): android.graphics.Bitmap? {
        return try {
            val file = java.io.File(context.filesDir, "custom_app_logo.png")
            if (file.exists()) {
                android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            } else {
                android.graphics.BitmapFactory.decodeResource(context.resources, com.example.R.drawable.ic_app_icon_foreground)
            }
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to decode custom logo file: ${e.message}")
            null
        }
    }
}
