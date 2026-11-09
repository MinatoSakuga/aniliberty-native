package top.aniliberty.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import top.aniliberty.app.AniLibertyApplication

/**
 * Цепочка будильников: раз в 15 минут будит приложение и запускает мгновенную
 * проверку новых серий. WorkManager-периодика остаётся подстраховкой, но его
 * окно может «плыть» в дрёме — будильник приходит раньше и уведомления не
 * запаздывают.
 */
object AlarmScheduler {
    private const val REQ = 2101
    private const val PERIOD_MS = 15 * 60 * 1000L

    fun schedule(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(context)
        val at = System.currentTimeMillis() + PERIOD_MS
        try {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(context))
    }

    private fun pending(context: Context): PendingIntent =
        PendingIntent.getBroadcast(context, REQ, Intent(context, CheckReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

class CheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext as? AniLibertyApplication ?: return
        if (!app.container.store.settings.value.notifications) return
        AlarmScheduler.schedule(context)
        EpisodeNotifications.checkNow(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val app = context.applicationContext as? AniLibertyApplication ?: return
        if (!app.container.store.settings.value.notifications) return
        AlarmScheduler.schedule(context)
        EpisodeNotifications.checkNow(context)
    }
}
