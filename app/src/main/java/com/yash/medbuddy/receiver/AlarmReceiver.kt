package com.yash.medbuddy.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yash.medbuddy.utils.NotificationHelper
import java.util.*

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val name = intent.getStringExtra("medName") ?: "Medicine"
        val time = intent.getStringExtra("medTime") ?: return

        // 🔔 Notification
        NotificationHelper.showNotification(context, name)

        // 🔁 Repeat next day
        val parts = time.split(":")
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
        calendar.set(Calendar.MINUTE, parts[1].toInt())
        calendar.set(Calendar.SECOND, 0)
        calendar.add(Calendar.DAY_OF_MONTH, 1)

        val newIntent = Intent(context, AlarmReceiver::class.java)
        newIntent.putExtra("medName", name)
        newIntent.putExtra("medTime", time)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (name + time).hashCode(),
            newIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
}