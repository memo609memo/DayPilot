package com.example.daypilot.ui.login

import android.app.AlertDialog
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.daypilot.R
import android.widget.Button
import android.widget.EditText
import com.example.daypilot.MainActivity
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    companion object {
        fun newInstance() = LoginFragment()
    }

    private lateinit var viewModel: LoginViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        val viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)

        val emailText = view.findViewById<EditText>(R.id.emailLoginEditText)
        val passwordText = view.findViewById<EditText>(R.id.passwordLoginEditText)

        view.findViewById<Button>(R.id.loginButton).setOnClickListener {
            val email = emailText.text.toString().trim()
            val password = passwordText.text.toString().trim()

            viewModel.loginUser(email, password)
        }

        view.findViewById<Button>(R.id.goRegisterButton).setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        viewModel.loginSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                val intent = Intent(context, MainActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
        }

        viewModel.loginError.observe(viewLifecycleOwner) { loginError ->
            Toast.makeText(requireContext(), loginError, Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.forgotPasswordButton).setOnClickListener {
            showForgotPasswordDialog()
        }

    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Forgot Password")

        val input = EditText(context)
        input.hint = "Enter your email"
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton("Done") { dialog, _ ->

            val email = input.text.toString().trim()
            if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Success: Sent password reset to email", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed: Invalid Email", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Invalid Email", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }


        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel()}

        val dialog = builder.create()
        dialog.show()

    }


}