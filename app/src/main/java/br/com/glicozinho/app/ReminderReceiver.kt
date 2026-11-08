package br.com.glicozinho.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("reminder_id") ?: return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "glicozinho_routine_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Lembretes de rotina", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Lembretes escolhidos pela pessoa usuária no Glicozinho"
                }
            )
        }
        val openApp = PendingIntent.getActivity(
            context, 7001,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }.setSmallIcon(R.drawable.ic_stat_glico)
            .setContentTitle("Lembrete do Glicozinho")
            .setContentText(if (id.startsWith("appointment:")) "Você tem um compromisso agendado. Abra o Glicozinho para conferir." else "Você tem um lembrete. Abra o Glicozinho para conferir.")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Notification.Builder(context, channelId)
                } else {
                    @Suppress("DEPRECATION")
                    Notification.Builder(context)
                }.setSmallIcon(R.drawable.ic_stat_glico)
                    .setContentTitle("Lembrete do Glicozinho")
                    .setContentText("Abra o aplicativo para conferir seu compromisso ou lembrete.")
                    .build()
            )
            .build()

        // Sem permissão de notificação, o Android pode rejeitar a publicação; o alarme não é clínico.
        try { manager.notify(id.hashCode(), notification) } catch (_: SecurityException) { }
        ReminderScheduler.onFired(context, id)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.TIMEZONE_CHANGED" ||
            intent.action == "android.intent.action.TIME_SET" ||
            intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            ReminderScheduler.rescheduleAll(context)
        }
    }
}
