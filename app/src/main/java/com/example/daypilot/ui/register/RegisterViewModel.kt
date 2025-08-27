package com.example.daypilot.ui.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import com.google.firebase.database.FirebaseDatabase

class RegisterViewModel : ViewModel() {

    //Variables/////////////////////////////////////////////////////

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _registerSuccess = MutableLiveData<Boolean>()
    private val _registerError = MutableLiveData<String>()

    val registerSuccess: LiveData<Boolean> get() = _registerSuccess
    val registerError: LiveData<String> get() = _registerError

    ////////////////////////////////////////////////////////////////

    fun registerUser(email: String, password: String) {


        //attempt to create a user with an email and password
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {


                val user = auth.currentUser

                val uid = FirebaseAuth.getInstance().currentUser?.uid
                val userEmail = FirebaseAuth.getInstance().currentUser?.email

                val userRef = FirebaseDatabase.getInstance().getReference("users/$uid")

                val userSettings = mapOf(
                    "darkModeOn" to false,
                    "notificationsOn" to false,
                    "receiptsOn" to false
                )

                val userData = mapOf(
                    "email" to userEmail,
                    "userSettings" to userSettings,
                    "tasks" to null
                )

                userRef.setValue(userData)

                //send the email verification to user so they can verify account creation
                user?.sendEmailVerification()
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            _registerSuccess.value = true

                        } else {
                            _registerError.value = "Email verification failed"
                        }
                    }
            } else {
                Log.e("Register", "createUserWithEmail:failure", task.exception)
                _registerSuccess.value = false
                _registerError.value = task.exception.toString() ?: "Unknown error"
            }
        }

    }


}