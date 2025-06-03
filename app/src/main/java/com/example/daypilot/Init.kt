package com.example.daypilot

import android.app.Application
import com.google.firebase.FirebaseApp

class Init : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)


    }

}