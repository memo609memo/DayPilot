package com.example.daypilot.ui.home

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.daypilot.databinding.FragmentHomeBinding
import android.provider.Settings
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import com.example.daypilot.data.TaskRepo
import com.example.daypilot.ui.floatingbutton.FloatingButton


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val taskRepo = TaskRepo()
    private val homeViewModelFactory = HomeViewModelFactory(taskRepo)
    private val homeViewModel: HomeViewModel by viewModels { homeViewModelFactory }

    // This is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }


        //mic button
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(requireContext())) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(intent)
        } else {
            FloatingButton(requireActivity())
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}