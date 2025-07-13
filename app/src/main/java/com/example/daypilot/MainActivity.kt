package com.example.daypilot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.icu.util.TimeZone
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity.NOTIFICATION_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.Navigation
import androidx.navigation.Navigation.findNavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.daypilot.databinding.ActivityMainBinding
import com.example.daypilot.ui.settings.TaskNotificationManager
import com.example.daypilot.ui.settings.UserSettings
import com.example.daypilot.ui.settings.applyDarkMode
import com.example.daypilot.ui.settings.channelID
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannel()

        TaskNotificationManager.initFirebaseTaskListener(this)

        val navView: BottomNavigationView = binding.navView

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        val userRef = FirebaseDatabase.getInstance().getReference("users/$uid")

        val ref = FirebaseDatabase.getInstance().getReference("users/$uid/userSettings")

        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val settings = snapshot.getValue(UserSettings::class.java)
                settings?. let {
                    applyDarkMode(it.darkModeOn)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

        val timeZone = TimeZone.getDefault().id

        val dbTimeZone = mapOf("timeZone" to timeZone)

        userRef.updateChildren(dbTimeZone)

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notes, R.id.taskViewFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.settingsFragment -> {
                    binding.navView.visibility = View.GONE
                } R.id.navigation_notifications -> {
                binding.navView.visibility = View.GONE
                }else -> {
                    navView.visibility = View.VISIBLE
                }
            }
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
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


