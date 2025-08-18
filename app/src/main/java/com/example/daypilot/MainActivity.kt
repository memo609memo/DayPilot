package com.example.daypilot

import android.app.Activity
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.icu.util.TimeZone
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.speech.RecognizerIntent
import android.view.View
import androidx.activity.viewModels
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
import androidx.navigation.fragment.findNavController
import com.example.daypilot.data.TaskRepo
import com.example.daypilot.ui.home.HomeViewModel
import com.example.daypilot.ui.home.HomeViewModelFactory

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val taskRepo = TaskRepo()
    private val homeViewModelFactory = HomeViewModelFactory(taskRepo)
    private val homeViewModel: HomeViewModel by viewModels { homeViewModelFactory }


    companion object {
        const val REQ_SPEECH = 1234
    }

    // must be public, take a View
    fun onMicIconClick(view: View) {
        // android speech intent directly
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

            // making sure our result data is a string
            val matches: ArrayList<String>? =
                data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

            // spoken text is going to our spoken text, the built in translator spits a few
            val spokenTextAbu = matches?.firstOrNull() ?: return

            // store it in  ViewModel
            homeViewModel.onNewSpeechText(spokenTextAbu)
            // send to python
            homeViewModel.sendTextToAi(spokenTextAbu)
        }
    }
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
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_notifications,
                R.id.navigation_dashboard,
                R.id.navigation_notes
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

