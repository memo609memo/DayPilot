package com.example.daypilot.ui.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import android.util.Log

class RegisterViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _registerSuccess = MutableLiveData<Boolean>()
    val registerSuccess: LiveData<Boolean> get() = _registerSuccess

    private val _registerError = MutableLiveData<String>()
    val registerError: LiveData<String> get() = _registerError

    fun registerUser(email: String, password: String) {

        //todo: implement registering of users firebase

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {


                val user = auth.currentUser

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