package com.example.daypilot.ui.notifications

import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.daypilot.databinding.FragmentNotificationsBinding
import com.example.daypilot.ui.notes.NotesViewModel
import com.example.daypilot.ui.notes.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.*

class NotificationsFragment : Fragment() {
    companion object {
        private const val TAG = "NotificationsFrag"
    }

    private val repeatToggles = MutableList(7) { false }

    private var selectedStartHour = -1
    private var selectedStartMinute = -1
    private var selectedEndHour = -1
    private var selectedEndMinute = -1

    private var descriptionIncoming: String? = null
    private var dateIncoming: String? = null
    private var timeIncoming: String? = null
    private var weekdayIncoming: String? = null

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root = binding.root

        val args = arguments ?: Bundle()
        if (args.isEmpty.not()) {
            Log.d(TAG, "Bundle keys ${args.keySet()}")
            args.keySet().forEach { key ->
                Log.d(TAG, "-- $key = ${args.getString(key)}")
            }
            descriptionIncoming = args.getString("description")
            dateIncoming = args.getString("date")
            timeIncoming = args.getString("time")
            weekdayIncoming = args.getString("weekday")
            Log.d(
                TAG,
                "Parsed desc=$descriptionIncoming, date=$dateIncoming, time=$timeIncoming, weekday=$weekdayIncoming"
            )
        } else {
            Log.d(TAG, "No arguments passed  creating a brand-new notification")
        }

        setupRepeatButtons()
        setupTimePicker()

        // Populate UI with incoming values
        descriptionIncoming?.let {
            binding.editTaskName.setText(it)
            binding.noteBody.setText(it)
        }
        dateIncoming?.let {
            binding.taskDate.setText(it)
        }
        timeIncoming?.let {
            binding.startTimeTextView.text = it
        }

        arguments?.getString("taskId")?.let { loadExistingTask(it) }

        binding.saveButton.setOnClickListener {
            if (descriptionIncoming != null) {
                createTaskInDayPilot(
                    description = descriptionIncoming!!,
                    dateStr = dateIncoming,
                    timeStr = timeIncoming,
                    weekday = weekdayIncoming
                )
            } else {
                arguments?.getString("taskId")?.let { taskId ->
                    val newTitle = binding.editTaskName.text.toString()
                    val updatedDesc = binding.noteBody.text.toString()
                    updateTaskInFirebase(taskId, newTitle, updatedDesc)
                }
            }
        }

        return root
    }

    private fun createTaskInDayPilot(
        description: String,
        dateStr: String?,
        timeStr: String?,
        weekday: String?
    ) {
        Log.d(TAG, "createTaskInDayPilot() desc=$description, date=$dateStr")
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val tasksRef = FirebaseDatabase.getInstance()
            .getReference("/users/$uid/Tasks")

        val newTaskId = tasksRef.push().key ?: UUID.randomUUID().toString()
        val combinedDate = buildString {
            dateStr?.let { append(it) }
            timeStr?.let { append(" $it") }
            weekday?.let { append(" ($it)") }
        }

        val task = Task(
            id = newTaskId,
            title = description,
            description = description,
            date = combinedDate
        )

        tasksRef.child(newTaskId)
            .setValue(task)
            .addOnSuccessListener {
                Log.d(TAG, "Task Created: $newTaskId")
                Toast.makeText(requireContext(), "Task Saved!", Toast.LENGTH_SHORT).show()

                // Update Calendar after save
                val notesViewModel = ViewModelProvider(requireActivity()).get(NotesViewModel::class.java)
                notesViewModel.addTask(task)

                findNavController().popBackStack()
            }
            .addOnFailureListener {
                Log.e(TAG, "Error creating task", it)
                Toast.makeText(requireContext(), "Failed to save task", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadExistingTask(taskId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance()
            .getReference("/users/$uid/Tasks")

        ref.orderByChild("id").equalTo(taskId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.firstOrNull()?.getValue(Task::class.java)?.let { task ->
                        binding.editTaskName.setText(task.title)
                        binding.noteBody.setText(task.description)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "loadExistingTask:onCancelled", error.toException())
                }
            })
    }

    private fun updateTaskInFirebase(taskId: String, newTitle: String, newDesc: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance()
            .getReference("/users/$uid/Tasks")

        ref.orderByChild("id").equalTo(taskId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.firstOrNull()?.ref?.updateChildren(
                        mapOf(
                            "title" to newTitle,
                            "description" to newDesc
                        )
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "updateTaskInFirebase:onCancelled", error.toException())
                }
            })
    }

    private fun setupRepeatButtons() {
        val buttons = listOf(
            binding.repeatSunday, binding.repeatMonday, binding.repeatTuesday,
            binding.repeatWednesday, binding.repeatThursday,
            binding.repeatFriday, binding.repeatSaturday
        )
        buttons.forEachIndexed { i, btn ->
            btn.isSelected = repeatToggles[i]
            btn.setOnClickListener {
                repeatToggles[i] = !repeatToggles[i]
                btn.isSelected = repeatToggles[i]
            }
        }
    }

    private fun setupTimePicker() {
        binding.startTimeTextView.setOnClickListener {
            showTimePicker(selectedStartHour, selectedStartMinute, binding.startTimeTextView)
        }
        binding.endTimeTextView.setOnClickListener {
            showTimePicker(selectedEndHour, selectedEndMinute, binding.endTimeTextView)
        }
    }

    private fun showTimePicker(initHour: Int, initMin: Int, textView: TextView) {
        val h = if (initHour >= 0) initHour else 0
        val m = if (initMin >= 0) initMin else 0
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                when (textView) {
                    binding.startTimeTextView -> {
                        selectedStartHour = hour
                        selectedStartMinute = minute
                    }
                    binding.endTimeTextView -> {
                        selectedEndHour = hour
                        selectedEndMinute = minute
                    }
                }
                textView.text = formatTime(hour, minute)
            },
            h, m, false
        ).show()
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val h12 = if (hour % 12 == 0) 12 else hour % 12
        return String.format("%02d:%02d %s", h12, minute, amPm)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
