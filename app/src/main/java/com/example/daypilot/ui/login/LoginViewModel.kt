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
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
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