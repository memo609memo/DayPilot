package com.example.daypilot


import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent

import android.os.Build

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService

import com.example.daypilot.ui.settings.NotificationReceiver
import com.example.daypilot.ui.settings.TaskNotificationManager
import com.example.daypilot.ui.settings.channelID
import com.google.firebase.auth.FirebaseAuth



class SplashActivity : AppCompatActivity() {

    private val splashDelay: Long = 2500


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        createNotificationChannel()

        TaskNotificationManager.initFirebaseTaskListener(this)


        Handler(Looper.getMainLooper()).postDelayed({

            //persistent login
            val user = FirebaseAuth.getInstance().currentUser
            val intent = if (user != null) {
                Intent(this@SplashActivity, MainActivity::class.java)
            } else {
                Intent(this@SplashActivity, AuthActivity::class.java)
            }

            startActivity(intent)
            finish()
        }, splashDelay)


    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notify Channel"
            val desc = "Task Notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelID, name, importance)
            channel.description = desc

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("Debugging", "Notification Channel Created")
        }
    }

}

