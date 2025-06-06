package com.example.daypilot.ui.notifications

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.daypilot.databinding.FragmentNotificationsBinding
import android.app.TimePickerDialog
import android.graphics.Color.alpha
import androidx.navigation.fragment.findNavController
import com.example.daypilot.R
import java.util.*

class NotificationsFragment : Fragment() {

    private val repeatToggles = MutableList(7) { false }
    private var selectedStartHour: Int = -1
    private var selectedStartMinute: Int = -1
    private var selectedEndHour: Int = -1
    private var selectedEndMinute: Int = -1

    private var _binding: FragmentNotificationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel = ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRepeatButtons()
        setupTimePicker()

        binding.settingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_notifications_to_settingsFragment)
        }

        return root
    }



    private fun setupRepeatButtons() {
        val buttons = listOf(
            binding.repeatSunday,
            binding.repeatMonday,
            binding.repeatTuesday,
            binding.repeatWednesday,
            binding.repeatThursday,
            binding.repeatFriday,
            binding.repeatSaturday
        )

        buttons.forEachIndexed { index, button ->
            button.isSelected = repeatToggles[index]


            button.setOnClickListener {
                repeatToggles[index] = !repeatToggles[index]
                button.isSelected = repeatToggles[index]
            }
        }
    }

    private fun setupTimePicker() {
        binding.startTimeTextView.setOnClickListener {
            showTimePickerDialog(selectedStartHour, selectedStartMinute, binding.startTimeTextView)

        }

        binding.endTimeTextView.setOnClickListener {
            showTimePickerDialog(selectedEndHour, selectedEndMinute, binding.endTimeTextView)
        }
    }

    private fun showTimePickerDialog(initialHour: Int, initialMinute: Int, textView: TextView){
        val hourToDisplay = if (initialHour != -1) initialHour else 0;
        val minuteToDisplay = if (initialMinute != -1) initialMinute else 0;

        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour: Int, selectedMinute: Int ->
                when(textView) {
                    binding.startTimeTextView -> {
                        selectedStartHour = selectedHour
                        selectedStartMinute = selectedMinute
                    }
                    binding.endTimeTextView -> {
                        selectedEndHour = selectedHour
                        selectedEndMinute = selectedMinute
                    }
                }
                updateTimeTextView(textView, selectedHour, selectedMinute)
            },
            hourToDisplay,
            minuteToDisplay,
            false
        )
        timePickerDialog.show()
    }

    private fun updateTimeTextView(textView: TextView, selectedHour: Int, selectedMinute: Int) {
        if (selectedHour != -1 && selectedMinute != -1) {
            val formattedTime = formatTime(selectedHour, selectedMinute)
            textView.text = formattedTime
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM";
        val displayHour = if(hour == 0 || hour == 12) 12 else hour % 12
        return String.format("%02d:%02d %s", displayHour, minute, amPm)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}