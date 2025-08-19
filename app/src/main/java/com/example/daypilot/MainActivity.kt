package com.example.daypilot

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.icu.util.TimeZone
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.daypilot.data.TaskRepo
import com.example.daypilot.databinding.ActivityMainBinding
import com.example.daypilot.ui.home.HomeViewModel
import com.example.daypilot.ui.home.HomeViewModelFactory
import com.example.daypilot.ui.settings.TaskNotificationManager
import com.example.daypilot.ui.settings.UserSettings
import com.example.daypilot.ui.settings.applyDarkMode
import com.example.daypilot.ui.settings.channelID
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.ArrayList

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // here we set for the mic from homeview speech to ai-model to notification
    private val taskRepo = TaskRepo()
    private val homeViewModelFactory = HomeViewModelFactory(taskRepo)
    private val homeViewModel: HomeViewModel by viewModels { homeViewModelFactory }

    companion object {
        const val REQ_SPEECH = 1234
    }

    fun onMicIconClick(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now…")
        }
        startActivityForResult(intent, REQ_SPEECH)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SPEECH && resultCode == Activity.RESULT_OK) {
            val matches: ArrayList<String>? =
                data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull() ?: return
            // updates live data
            homeViewModel.onNewSpeechText(spoken)
            homeViewModel.sendTextToAi(spoken)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannel()
        TaskNotificationManager.initFirebaseTaskListener(this)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_notes,
                R.id.taskViewFragment,
                R.id.settingsFragment
            )
        )

        // setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


        //  Firebase  settings n timezone
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("users/$uid")
            val settingsRef = FirebaseDatabase.getInstance().getReference("users/$uid/userSettings")

            // Apply dark mode from DB once at launch
            settingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.getValue(UserSettings::class.java)?.let { applyDarkMode(it.darkModeOn) }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.w("MainActivity", "Load settings cancelled: ${error.message}")
                }
            })

            //  device timezone for server logic
            val timeZone = TimeZone.getDefault().id
            userRef.updateChildren(mapOf("timeZone" to timeZone))
        } else {
            Log.w("MainActivity", "No Firebase user; skipping settings/timezone update.")
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
            val channel = NotificationChannel(channelID, name, importance).apply {
                description = desc
            }
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
            Log.d("MainActivity", "Notification Channel Created")
        }
    }
}
