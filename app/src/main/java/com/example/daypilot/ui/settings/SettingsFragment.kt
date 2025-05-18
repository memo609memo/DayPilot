package com.example.daypilot.ui.settings

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import com.sendgrid.*
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.example.daypilot.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

val appEmail = "app.daypilot@gmail.com"
val userEmail = FirebaseAuth.getInstance().currentUser?.email
val API_Key = "removed"

class SettingsFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val navController = findNavController()
            navController.setGraph(R.navigation.auth_navigation)
        }

        view.findViewById<Button>(R.id.btnDeleteAccount).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete") { dialog, _ ->
                    dialog.dismiss()
                    FirebaseAuth.getInstance().currentUser?.delete()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()

            val navController = findNavController()
            navController.setGraph(R.navigation.auth_navigation)
        }

        view.findViewById<Button>(R.id.btnReportProblem).setOnClickListener {
            val input = EditText(context)
            input.hint = "Please describe the problem."
            input.inputType = InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE


            AlertDialog.Builder(requireContext())
                .setTitle("Report a Problem")
                .setView(input)
                .setPositiveButton("Submit") { dialog, _ ->
                    val userInput = input.text.toString().trim()
                    if (userInput.isNotEmpty()) {
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
                .show()

        }
    }
}

fun sendEmailToApp(problem: String) {
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
            Log.d("SendGrid", "Sending JSON: $requestBody")

            val output = conn.outputStream
            output.write(requestBody.toByteArray(Charsets.UTF_8))
            output.flush()
            output.close()

        } catch (e: Exception) {
            Log.e("SendGrid", "Failed to send email: ${e.message}", e)
        }
    }.start()
}

fun sendEmailToUser(problem: String) {
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






