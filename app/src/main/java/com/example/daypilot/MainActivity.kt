package com.example.daypilot

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import androidx.activity.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.daypilot.databinding.ActivityMainBinding
import com.google.firebase.FirebaseApp
import androidx.navigation.fragment.findNavController
import com.example.daypilot.data.TaskRepo
import com.example.daypilot.ui.home.HomeViewModel
import com.example.daypilot.ui.home.HomeViewModelFactory


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

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
}


