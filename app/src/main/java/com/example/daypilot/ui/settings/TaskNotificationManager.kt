package com.example.daypilot.ui.settings

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.daypilot.ui.notes.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

//Object that manages scheduling and canceling task notifications
object TaskNotificationManager {

    fun initFirebaseTaskListener(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val dbRef = FirebaseDatabase.getInstance().getReference("users/$uid")

        dbRef.child("userSettings").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(settingsSnapshot: DataSnapshot) {
                val notificationsOn = settingsSnapshot.child("notificationsOn").getValue(Boolean::class.java) ?: false
                if (!notificationsOn) return

                val tasksRef = dbRef.child("tasks")
                tasksRef.addChildEventListener(object : ChildEventListener {
                    @RequiresApi(Build.VERSION_CODES.S)
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        handleTaskSnapshot(context, snapshot)
                    }

                    @RequiresApi(Build.VERSION_CODES.S)
                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                        Log.d("Received Task Change", "Received Task Change")
                        handleTaskSnapshot(context, snapshot)
                    }

                    override fun onChildRemoved(snapshot: DataSnapshot) {
                        cancelScheduledNotification(context, snapshot.key ?: return)
                    }

                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun handleTaskSnapshot(context: Context, snapshot: DataSnapshot) {
        val task = snapshot.getValue(Task::class.java) ?: return
        val taskId = snapshot.key ?: return

        if (task.isCompleted) {
            cancelScheduledNotification(context, taskId)
        } else {
            scheduleTaskNotification(context, taskId, task.title, task.description, task.date, task.startTime)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun scheduleTaskNotification(context: Context, taskId: String, title: String, description: String, date: String, startTime: String) {
        Log.d("Debugging Log", "Notification function triggered")
        if (date == "" || startTime == "") return
        val taskTimeMillis = parseTaskDateTime(date, startTime)
        val notifyTime = taskTimeMillis - 15 * 60 * 1000

        if (notifyTime < System.currentTimeMillis()) return

        val notification = "$title is starting in 15 minutes at $startTime."

        Log.d("Debugging Log", notification)

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(titleExtra, title)
            putExtra(messageExtra, notification)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notifyTime, pendingIntent)
            Log.d("Debugging Log", "AlarmManager Fired")
        }
    }

    fun cancelScheduledNotification(context: Context, taskId: String) {
        Log.d("Debugging Log", "Cancel Notification function triggered")
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        pendingIntent?.let {
            alarmManager.cancel(it)
        }

    }

    private fun parseTaskDateTime(date: String, time: String): Long {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.US)
        val localDateTime = LocalDateTime.parse("$date $time", formatter)
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}