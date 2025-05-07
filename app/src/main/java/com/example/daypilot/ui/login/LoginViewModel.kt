package com.example.daypilot.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth

class LoginViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> get() = _loginSuccess

    private val _loginError = MutableLiveData<String>()
    val loginError: LiveData<String> get() = _loginError

    fun loginUser(email: String, password: String) {
        if(email.isEmpty() || password.isEmpty()) {
            _loginSuccess.value = false
            _loginError.value = "Please fill out all fields"
            return
        }


        auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if(task.isSuccessful) {
                val user = auth.currentUser
                if(user != null && user.isEmailVerified) {
                    _loginSuccess.value = true
                } else {
                    _loginSuccess.value = false
                    _loginError.value = "Email is not verified"
                }
            } else {
                _loginError.value = task.exception?.message ?: "ERROR: Maybe check credentials?"
            }
        }
    }

}