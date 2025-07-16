package com.example.daypilot.ui.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.daypilot.ui.notes.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val ref = FirebaseDatabase.getInstance().getReference("users/$uid")

            ref.child("userSettings").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(settingSnapshot: DataSnapshot) {
                    val notificationsOn = settingSnapshot.child("notificationsOn").getValue(Boolean::class.java) ?: false
                    if (!notificationsOn) return

                    ref.child("tasks").addListenerForSingleValueEvent(object : ValueEventListener {
                        @RequiresApi(Build.VERSION_CODES.S)
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (taskSnapshot in snapshot.children) {
                                val task = taskSnapshot.getValue(Task::class.java)
                                val taskId = taskSnapshot.key ?: continue

                                if (task != null && !task.isCompleted) {
                                    TaskNotificationManager.scheduleTaskNotification(
                                        context, taskId, task.title, task.description, task.date, task.startTime
                                    )
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }
}