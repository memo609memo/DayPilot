package com.example.daypilot.ui.home

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
        val root = binding.root


        val tv: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) { tv.text = it }

        //  permission redirection for overlay intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(requireContext())
        ) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        } else {
            FloatingButton(requireActivity())
        }

        // listening for the ai the redirects it to notification
        homeViewModel.aiResponse.observe(viewLifecycleOwner) { reply ->
            if (reply?.success == true) {
                val bundle = bundleOf(
                    "description" to reply.description,
                    "date" to reply.date,
                    "time" to reply.time,
                    "weekday" to reply.weekday
                )
                findNavController().navigate(R.id.navigation_notifications, bundle)

                // clears response so we go back to calendar and are not fill it out indefinitely
                homeViewModel.clearAiResponse()
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
