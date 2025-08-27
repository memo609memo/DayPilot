package com.example.daypilot.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.fragment.findNavController
import com.example.daypilot.R
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.example.daypilot.BuildConfig
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.daypilot.SplashActivity
import com.example.daypilot.ui.notes.isDarkMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

//App email used for reporting user problems
val appEmail = "app.daypilot@gmail.com"

//API key for SendGrid
val API_Key = BuildConfig.SENDGRID_API_KEY

//User settings object
var settings = UserSettings()



class SettingsFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Gets current user ID and DB ref for user settings
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("users/$uid/userSettings")



        //Logout Button
        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            cancelScheduledNotifications(requireContext()) //cancels notifications
            FirebaseAuth.getInstance().signOut() //signs user out
            val intent = Intent(requireContext(), SplashActivity::class.java)
            AppCompatDelegate.setDefaultNightMode((AppCompatDelegate.MODE_NIGHT_NO)) //resets night mode setting
            startActivity(intent)
            requireActivity().finish()
        }

        //Loads user settings from Firebase
        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userSettings = snapshot.getValue(UserSettings::class.java)

                userSettings?. let {
                    settings.darkModeOn = it.darkModeOn
                    settings.receiptsOn = it.receiptsOn
                    settings.notificationsOn = it.notificationsOn

                    //Updates switches with settings from Firebase
                    view.findViewById<SwitchCompat>(R.id.switchDarkMode).isChecked = settings.darkModeOn
                    view.findViewById<SwitchCompat>(R.id.switchReceipts).isChecked = settings.receiptsOn
                    view.findViewById<SwitchCompat>(R.id.switchNotifications).isChecked = settings.notificationsOn
                }


            }

            override fun onCancelled(error: DatabaseError) {

            }
        })




        //Delete account button
        view.findViewById<Button>(R.id.btnDeleteAccount).setOnClickListener {

            //Prompts user for confirmation
            val input = EditText(requireContext())
            input.hint = "Type DELETE to delete your account."
            input.inputType = InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE

            val alertDialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle("Delete Account? This cannot be undone.")
                .setView(input)
                .setPositiveButton("Confirm", null)
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()

            //Sets AlertDialog style based on day/night modes
            alertDialog.setOnShowListener {
                val positive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                val negative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

                val isDark = requireContext().isDarkMode()
                if (!isDark) {
                    positive.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.text_color
                        )
                    )
                    negative.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.text_color
                        )
                    )
                } else {
                    positive.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.dark_text_color
                        )
                    )
                    negative.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.dark_text_color
                        )
                    )
                }
            }


            alertDialog.show()

            //Confirms user typed "DELETE" and handles account deletion from Firebase and returns them to the login screen
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val userInput = input.text.toString().trim()
                if (userInput == "DELETE") {
                    alertDialog.dismiss()
                    cancelScheduledNotifications(requireContext())
                    FirebaseAuth.getInstance().currentUser?.delete()
                    val intent = Intent(requireContext(), SplashActivity::class.java)
                    AppCompatDelegate.setDefaultNightMode((AppCompatDelegate.MODE_NIGHT_NO))
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    Toast.makeText(requireContext(), "Please type DELETE to confirm the deletion of your account.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        //Report problem button
        view.findViewById<Button>(R.id.btnReportProblem).setOnClickListener {

            //Prompts user for problem description
            val input = EditText(context).apply {
                hint = "Please describe the problem."
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                isSingleLine = false
                minLines = 5
                maxLines = 10
                gravity = Gravity.BOTTOM or Gravity.START
                setHorizontallyScrolling(false)
            }

            val reportDialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle("Report a Problem")
                .setView(input)
                .setPositiveButton("Submit") { dialog, _ ->
                    val userInput = input.text.toString().trim()
                    if (userInput.isNotEmpty()) {
                        //Sends email to user and app email addresses
                        sendEmailToApp(userInput)
                        sendEmailToUser(userInput)
                        dialog.dismiss()
                        Toast.makeText(context, "Problem successfully submitted", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(context, "The text box cannot be empty.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()

            //Sets AlertDialog style based on day/night modes
            reportDialog.setOnShowListener {
                val positive = reportDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                val negative = reportDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

                val isDark = requireContext().isDarkMode()
                if (!isDark) {
                    positive.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.text_color
                        )
                    )
                    negative.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.text_color
                        )
                    )
                } else {
                    positive.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.dark_text_color
                        )
                    )
                    negative.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.dark_text_color
                        )
                    )
                }
            }
            reportDialog.show()
        }


        //Dark mode switch
        view.findViewById<SwitchCompat>(R.id.switchDarkMode).setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                settings.darkModeOn = false
                ref.setValue(settings)
                applyDarkMode(settings.darkModeOn)
            }
            else {
                settings.darkModeOn = true
                ref.setValue(settings)
                applyDarkMode(settings.darkModeOn)
            }
        }

        //Notifications switch
        view.findViewById<SwitchCompat>(R.id.switchNotifications).setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                settings.notificationsOn = false
                ref.setValue(settings)
                cancelScheduledNotifications(requireContext())
            }
            else {
                settings.notificationsOn = true
                ref.setValue(settings)
                checkNotificationPermissions(requireContext())
            }
        }

        //Email receipts switch
        view.findViewById<SwitchCompat>(R.id.switchReceipts).setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                settings.receiptsOn = false
                ref.setValue(settings)
            }
            else {
                settings.receiptsOn = true
                ref.setValue(settings)
            }
        }
    }
}

fun sendEmailToApp(problem: String) {

    val userEmail = FirebaseAuth.getInstance().currentUser?.email

    //Builds JSON request
    val json = JSONObject().apply {
        put("personalizations", JSONArray().apply {
            put(JSONObject().apply {
                put("to", JSONArray().apply {
                    put(JSONObject().apply {
                        put("email", appEmail)
                    })
                })
                put("subject", "A user has submitted a problem.")
            })
        })
        put("from", JSONObject().apply {
            put("email", appEmail)
        })
        put("content", JSONArray().apply {
            put(JSONObject().apply {
                put("type", "text/plain")
                put("value", "User's email: $userEmail\n\nUser's message:\n$problem")
            })
        })
    }
    Thread {
        try {
            val url = URL("https://api.sendgrid.com/v3/mail/send")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $API_Key")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val requestBody = json.toString()


            val output = conn.outputStream
            output.write(requestBody.toByteArray(Charsets.UTF_8))
            output.flush()
            output.close()

            val responseCode = conn.responseCode
            val responseMessage = conn.responseMessage
            Log.d("SendGrid", "Response Code: $responseCode, Message: $responseMessage")

        } catch (e: Exception) {
            Log.e("SendGrid", "Failed to send email: ${e.message}", e)
        }
    }.start()
}

fun sendEmailToUser(problem: String) {

    val userEmail = FirebaseAuth.getInstance().currentUser?.email

    //Builds JSON request
    val json = JSONObject().apply {
        put("personalizations", JSONArray().apply {
            put(JSONObject().apply {
                put("to", JSONArray().apply {
                    put(JSONObject().apply {
                        put("email", userEmail)
                    })
                })
                put("subject", "You submitted a problem to the DayPilot team.")
            })
        })
        put("from", JSONObject().apply {
            put("email", appEmail)
        })
        put("content", JSONArray().apply {
            put(JSONObject().apply {
                put("type", "text/plain")
                put("value", "Thank you for submitting a problem to the DayPilot team. We appreciate you working with us to identify any issues with the application.\n\nYour message:\n$problem")
            })
        })
    }
    val thread = Thread {
        try {
            val url = URL("https://api.sendgrid.com/v3/mail/send")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $API_Key")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val output = conn.outputStream
            output.write(json.toString().toByteArray())
            output.flush()
            output.close()

        } catch (e: Exception) {
            Log.e("SendGridUser", "Failed to send email: ${e.message}", e)
        }
    }
    thread.start()
}

fun checkNotificationPermissions(context: Context) : Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val isEnabled = notificationManager.areNotificationsEnabled()

        if (!isEnabled) {
            //Sends user to allow notifications if not enabled
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            context.startActivity(intent)

            return false

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
            }
        }
    } else {
        val areEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

        if (!areEnabled) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            context.startActivity(intent)

            return false
        }
    }

    return true
}


//Cancels all scheduled notifications
fun cancelScheduledNotifications(context: Context) {
    Log.d("Debugging Log", "Cancel triggered")
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val ref = FirebaseDatabase.getInstance().getReference("users/$uid/tasks")
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    ref.addListenerForSingleValueEvent(object : ValueEventListener {

        override fun onDataChange(snapshot: DataSnapshot) {
            Log.d("Debugging Log", "Event trigger")

            for (taskSnapshot in snapshot.children) {
                val taskId = taskSnapshot.key ?: continue

                Log.d("Debugging Log", taskId)

                val intent = Intent(context, NotificationReceiver::class.java)

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskId.hashCode(),
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )

                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                }
                Log.d("Debugging Log", "Alarm canceled")
            }

            NotificationManagerCompat.from(context).cancelAll()

            ref.removeEventListener(this)
        }

        override fun onCancelled(error: DatabaseError) {
            Log.d("Debugging Log", error.message)
        }
    })
}



