package com.example.daypilot.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth
import com.example.daypilot.ui.settings.UserSettings
import com.example.daypilot.ui.settings.applyDarkMode
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

import com.google.firebase.database.database

class LoginViewModel : ViewModel() {

    //viewmodel for the login screen which includes the login logic and sends to state to the UI

    //Variables///////////////////////////////////////////////////////////////////////////////

    //firebase authentication instance
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    //private mutable live data is used locally by the viewmodel
    private val _loginSuccess = MutableLiveData<Boolean>()
    private val _loginError = MutableLiveData<String>()

    //public live data to observe whether the login functionality works successfully or gives an error to send to the UI
    val loginSuccess: LiveData<Boolean> get() = _loginSuccess
    val loginError: LiveData<String> get() = _loginError
    /////////////////////////////////////////////////////////////////////////////////////////


    //This is the login logic
    fun loginUser(email: String, password: String) {

        //user didn't fill all information out
        if(email.isEmpty() || password.isEmpty()) {
            _loginSuccess.value = false
            _loginError.value = "Please fill out all fields"
            return
        }

        //firebase call to login
        auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if(task.isSuccessful) { //user successfully puts correct login information in
                val user = auth.currentUser
                if(user != null && user.isEmailVerified) { //user successfully logs in and their email is successful
                    _loginSuccess.value = true


                } else { //email is not verified error
                    _loginSuccess.value = false
                    _loginError.value = "Email is not verified"
                }
            } else { //any other reason the user cannot login
                _loginError.value = task.exception?.message ?: "ERROR: Maybe check credentials?"
            }
        }
    }

}