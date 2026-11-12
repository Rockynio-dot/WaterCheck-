package app.watercheck.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.watercheck.Hydration
import app.watercheck.R
import app.watercheck.WaterApp
import app.watercheck.data.DrinkType
import app.watercheck.data.Settings
import app.watercheck.ui.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

object Reminders {
    private const val CHANNEL = "reminders"
    private const val NOTIFICATION_ID = 1
    const val ACTION_REMIND = "app.watercheck.REMIND"
    const val ACTION_ADD = "app.watercheck.ADD"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, "Připomínky pití", NotificationManager.IMPORTANCE_HIGH)
        channel.description = "Pravidelné připomenutí, že je čas se napít"
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Replaces the pending alarm with the next reminder, or cancels it when reminders are off. */
    fun schedule(context: Context, settings: Settings) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = alarmIntent(context)
        alarms.cancel(intent)
        if (!settings.reminders) return
        val next = Hydration.nextReminder(
            LocalDateTime.now(), settings.intervalMin, settings.wakeMin, settings.sleepMin
        )
        val at = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
    }

    fun cancelNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun canNotify(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun notify(context: Context, totalMl: Int, settings: Settings) {
        if (!canNotify(context)) return
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val add = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_ADD),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val drink = DrinkType.of(settings.drinkType)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_drop)
            .setContentTitle("Čas se napít 💧")
            .setContentText("Dnes vypito $totalMl z ${settings.goalMl} ml")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "+ ${settings.cupMl} ml ${drink.label.lowercase()}", add)
            .build()
        @Suppress("MissingPermission") // checked by canNotify above
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

/** Fires on each alarm and on the "+ ml" notification action. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as WaterApp
        val pending = goAsync()
        app.scope.launch {
            try {
                val settings = app.settings.flow.first()
                when (intent.action) {
                    Reminders.ACTION_ADD ->
                        app.addDrink(settings.cupMl, DrinkType.of(settings.drinkType))

                    Reminders.ACTION_REMIND -> {
                        val total = app.todayTotalMl()
                        val now = LocalDateTime.now()
                        val awake = Hydration.isAwake(
                            now.hour * 60 + now.minute, settings.wakeMin, settings.sleepMin
                        )
                        if (awake && total < settings.goalMl) Reminders.notify(app, total, settings)
                        Reminders.schedule(app, settings)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarms are lost on reboot, app update and clock changes, so they are set again here. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as WaterApp
        val pending = goAsync()
        app.scope.launch {
            try {
                Reminders.schedule(app, app.settings.flow.first())
            } finally {
                pending.finish()
            }
        }
    }
}
