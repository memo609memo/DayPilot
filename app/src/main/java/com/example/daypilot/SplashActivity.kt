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



}

