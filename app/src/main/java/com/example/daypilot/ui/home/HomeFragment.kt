package com.example.daypilot.ui.home

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.daypilot.R
import com.example.daypilot.data.TaskRepo
import com.example.daypilot.databinding.FragmentHomeBinding
import com.example.daypilot.ui.floatingbutton.FloatingButton

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val taskRepo = TaskRepo()
    private val homeViewModelFactory = HomeViewModelFactory(taskRepo)
    private val homeViewModel: HomeViewModel by activityViewModels { homeViewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.button.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_settingsFragment)
        }

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(requireContext())
        ) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        } else {
            FloatingButton(requireActivity())
        }

        homeViewModel.aiResponse.observe(viewLifecycleOwner) { reply ->
            if (reply.success) {   // Changed to check for success == true
                val description = reply.description
                val dateStr = reply.date
                val timeStr = reply.time
                val weekday = reply.weekday

                val bundle = bundleOf(
                    "description" to description,
                    "date" to dateStr,
                    "time" to timeStr,
                    "weekday" to weekday
                )

                findNavController().navigate(
                    R.id.navigation_notifications,
                    bundle
                )
            } else {
                Log.d("HomeFragment", "AI response was not successful, skipping navigation.")
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
