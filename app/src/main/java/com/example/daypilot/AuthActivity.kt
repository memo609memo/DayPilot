package com.example.daypilot

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.FirebaseApp

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)


        val navHostFragment = supportFragmentManager.findFragmentById(R.id.auth_fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

    }


}