package com.example.daypilot.ui.register

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.daypilot.R
import com.example.daypilot.ui.login.LoginViewModel
import android.widget.Button
import android.widget.EditText
import com.example.daypilot.MainActivity

class RegisterFragment : Fragment() {

    companion object {
        fun newInstance() = RegisterFragment()
    }

    private lateinit var viewModel: RegisterViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = ViewModelProvider(this).get(RegisterViewModel::class.java)

        //todo: register hardcoding limits for signing up (char count, same password, username exists, etc)

        val emailText = view.findViewById<EditText>(R.id.emailRegisterEditText)
        val passwordText = view.findViewById<EditText>(R.id.passwordRegisterEditText)
        val confirmPasswordText = view.findViewById<EditText>(R.id.passwordConfirmRegisterEditText)

        view.findViewById<Button>(R.id.registerButton).setOnClickListener {

            val email = emailText.text.toString()
            val password = passwordText.text.toString()

            viewModel.registerUser(email, password)
        }


        viewModel.registerSuccess.observe(viewLifecycleOwner, { success ->
            if (success) {
                Toast.makeText(activity, "Registered Successfully", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
            } else {
                Toast.makeText(activity, "Registration Failed", Toast.LENGTH_SHORT).show()
            }
        })




        view.findViewById<Button>(R.id.goLoginButton).setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }


    }

}