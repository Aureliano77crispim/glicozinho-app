package br.com.glicozinho.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import org.json.JSONObject
import java.util.Calendar

/** Agenda avisos flexíveis ou pontuais a pedido da pessoa usuária. Alarmes flexíveis podem atrasar por economia de bateria. */
object ReminderScheduler {
    private const val PREFS = "glicozinho_reminders_v1"
    private const val KEY_ITEMS = "items"
    private const val ACTION_FIRE = "br.com.glicozinho.app.ACTION_FIRE_REMINDER"

    data class Reminder(
        val id: String,
        val triggerAtMillis: Long,
        val daily: Boolean,
        val exact: Boolean = false
    )

    fun schedule(context: Context, reminder: Reminder): String {
        if (reminder.id.isBlank() || reminder.id.length > 100 ||
            reminder.triggerAtMillis <= System.currentTimeMillis()) return "invalid"

        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (reminder.exact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            return "exact_permission_required"
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val items = readItems(prefs)
        val scheduledCalendar = Calendar.getInstance().apply { timeInMillis = reminder.triggerAtMillis }
        items.put(reminder.id, JSONObject().apply {
            // Não guardar títulos potencialmente sensíveis em SharedPreferences.
            put("id", reminder.id)
            put("triggerAtMillis", reminder.triggerAtMillis)
            put("hourOfDay", scheduledCalendar.get(Calendar.HOUR_OF_DAY))
            put("minute", scheduledCalendar.get(Calendar.MINUTE))
            put("year", scheduledCalendar.get(Calendar.YEAR))
            put("month", scheduledCalendar.get(Calendar.MONTH) + 1)
            put("dayOfMonth", scheduledCalendar.get(Calendar.DAY_OF_MONTH))
            put("daily", reminder.daily)
            put("exact", reminder.exact)
        })
        prefs.edit().putString(KEY_ITEMS, items.toString()).apply()

        val pending = pendingIntent(context, reminder)
        return try {
            // Lembretes comuns usam alarme inexato: não exigem acesso especial a alarmes exatos.
            if (reminder.exact) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAtMillis, pending)
                } else {
                    manager.setExact(AlarmManager.RTC_WAKEUP, reminder.triggerAtMillis, pending)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAtMillis, pending)
            } else {
                manager.set(AlarmManager.RTC_WAKEUP, reminder.triggerAtMillis, pending)
            }
            prefs.edit().putString(KEY_ITEMS, items.toString()).apply()
            if (reminder.exact) "scheduled_exact" else "scheduled_approximate"
        } catch (_: SecurityException) {
            items.remove(reminder.id)
            prefs.edit().putString(KEY_ITEMS, items.toString()).apply()
            "schedule_denied"
        } catch (_: Exception) {
            items.remove(reminder.id)
            prefs.edit().putString(KEY_ITEMS, items.toString()).apply()
            "schedule_failed"
        }
    }

    fun cancel(context: Context, id: String): Boolean {
        if (id.isBlank() || id.length > 100) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val items = readItems(prefs)
        items.remove(id)
        prefs.edit().putString(KEY_ITEMS, items.toString()).apply()
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pendingIntent(context, Reminder(id, 0L, false)))
        return true
    }


    fun isScheduled(context: Context, id: String): Boolean {
        if (id.isBlank() || id.length > 100) return false
        val item = readItems(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)).optJSONObject(id) ?: return false
        if (item.optBoolean("exact", false) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!manager.canScheduleExactAlarms()) return false
        }
        return true
    }

    fun isScheduledAt(context: Context, id: String, triggerAtMillis: Long, exact: Boolean): Boolean {
        if (!isScheduled(context, id)) return false
        val item = readItems(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)).optJSONObject(id) ?: return false
        val savedTrigger = item.optLong("triggerAtMillis", 0L)
        return item.optBoolean("exact", false) == exact && kotlin.math.abs(savedTrigger - triggerAtMillis) <= 60_000L
    }

    fun cancelAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val items = readItems(prefs)
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val iterator = items.keys()
        val ids = mutableListOf<String>()
        while (iterator.hasNext()) ids.add(iterator.next())
        ids.forEach { id -> manager.cancel(pendingIntent(context, Reminder(id, 0L, false))) }
        prefs.edit().clear().apply()
    }

    fun onFired(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val items = readItems(prefs)
        val item = items.optJSONObject(id) ?: return
        if (!item.optBoolean("daily", false)) {
            items.remove(id)
            prefs.edit().putString(KEY_ITEMS, items.toString()).apply()
            return
        }
        val next = nextDaily(item.optInt("hourOfDay", 9), item.optInt("minute", 0))
        item.put("triggerAtMillis", next)
        items.put(id, item)
        prefs.edit().putString(KEY_ITEMS, items.toString()).apply()
        schedule(context, Reminder(id, next, true, item.optBoolean("exact", false)))
    }

    fun rescheduleAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val items = readItems(prefs)
        val ids = mutableListOf<String>()
        val iterator = items.keys()
        while (iterator.hasNext()) ids.add(iterator.next())
        val toSchedule = mutableListOf<Reminder>()
        for (id in ids) {
            val item = items.optJSONObject(id) ?: continue
            val whenMillis = item.optLong("triggerAtMillis", 0L)
            val daily = item.optBoolean("daily", false)
            val exact = item.optBoolean("exact", false)
            val trigger = if (daily) {
                nextDaily(item.optInt("hourOfDay", 9), item.optInt("minute", 0))
            } else if (item.has("year") && item.has("month") && item.has("dayOfMonth")) {
                Calendar.getInstance().apply {
                    set(Calendar.YEAR, item.optInt("year"))
                    set(Calendar.MONTH, item.optInt("month", 1) - 1)
                    set(Calendar.DAY_OF_MONTH, item.optInt("dayOfMonth"))
                    set(Calendar.HOUR_OF_DAY, item.optInt("hourOfDay", 9))
                    set(Calendar.MINUTE, item.optInt("minute", 0))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            } else whenMillis
            if (trigger > System.currentTimeMillis()) {
                item.put("triggerAtMillis", trigger)
                toSchedule.add(Reminder(id, trigger, daily, exact))
            } else {
                items.remove(id)
            }
        }
        prefs.edit().putString(KEY_ITEMS, items.toString()).apply()
        toSchedule.forEach { schedule(context, it) }
    }

    private fun nextDaily(hourOfDay: Int, minute: Int): Long {
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis
    }

    private fun readItems(prefs: android.content.SharedPreferences): JSONObject = try {
        JSONObject(prefs.getString(KEY_ITEMS, "{}") ?: "{}")
    } catch (_: Exception) { JSONObject() }

    private fun pendingIntent(context: Context, reminder: Reminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_FIRE
            data = Uri.parse("glicozinho://reminder/${Uri.encode(reminder.id)}")
            putExtra("reminder_id", reminder.id)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getBroadcast(context, reminder.id.hashCode(), intent, flags)
    }
}
