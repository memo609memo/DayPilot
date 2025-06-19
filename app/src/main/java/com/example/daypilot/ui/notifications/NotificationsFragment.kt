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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*
import com.example.daypilot.ui.notes.Task
import android.util.Log

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
        val taskId = arguments?.getString("taskId")

        setupRepeatButtons()
        setupTimePicker()
        taskId?.let { getTaskDataFromFirebase(it)}

        //save button section
        binding.saveButton.setOnClickListener {
            val newTitle = binding.editTaskName.text.toString()
            val updatedDescription = binding.noteBody.text.toString()
            if (taskId != null) {
                updateTaskInFirebase(taskId, newTitle, updatedDescription)
            }
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

    private fun getTaskDataFromFirebase(taskId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/Tasks")

        ref.orderByChild("id").equalTo(taskId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for(taskSnapshot in snapshot.children) {
                        val task = taskSnapshot.getValue(Task::class.java)
                        if (task != null) {
                            binding.editTaskName.setText(task.title)
                            binding.noteBody.setText(task.description)
                            break
                        }
                    }
                   }


                override fun onCancelled(error: DatabaseError) {
                    Log.e("Error", error.message)
                }
            })
    }



    //Don't forget to add in Time Start + Time End
    //Also need to add Repeat section when that is done
    //Luis will also need to add the logic for saving the date in here!
    private fun updateTaskInFirebase(taskId: String, newTitle: String, newDescription: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/Tasks")

        ref.orderByChild("id").equalTo(taskId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for(taskSnapshot in snapshot.children) {
                        val updates = mapOf<String, Any>(
                            "title" to newTitle,
                            "description" to newDescription
                        )
                        taskSnapshot.ref.updateChildren(updates)
                        break
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Error", error.message)
                }
            })
    }
}