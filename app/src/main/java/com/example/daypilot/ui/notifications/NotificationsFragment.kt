package com.example.daypilot.ui.notifications

import android.app.DatePickerDialog
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.util.Calendar
import java.util.UUID

class NotificationsFragment : Fragment() {

    companion object { private const val TAG = "NotificationsFrag" }

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val repeatToggles = MutableList(7) { false }

    private var selectedStartHour = -1
    private var selectedStartMinute = -1
    private var selectedEndHour = -1
    private var selectedEndMinute = -1

    private var descriptionIncoming: String? = null
    private var dateIncoming: String? = null
    private var timeIncoming: String? = null
    private var weekdayIncoming: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root = binding.root

        arguments?.let { args ->
            descriptionIncoming = args.getString("description")
            dateIncoming = args.getString("date")
            timeIncoming = args.getString("time")
            weekdayIncoming = args.getString("weekday")
            Log.d(TAG, "Args desc=$descriptionIncoming date=$dateIncoming time=$timeIncoming weekday=$weekdayIncoming")
        }

        descriptionIncoming?.let {
            binding.editTaskName.setText(it)
            binding.noteBody.setText(it)
        }
        dateIncoming?.let { binding.taskDate.setText(it) }
        timeIncoming?.let { binding.startTimeTextView.text = it }

        // Edit mode? Load existing task
        val taskId = arguments?.getString("taskId")
        if (taskId != null) loadTask(taskId)

        setupRepeatButtons()
        setupTimePicker()

        binding.taskDate.setOnClickListener { showDatePickerDialog() }

        binding.saveButton.setOnClickListener {
            val title = binding.editTaskName.text.toString().trim()
            val description = binding.noteBody.text.toString().trim()
            val date = binding.taskDate.text.toString().trim()
            val startTime = binding.startTimeTextView.text.toString().trim()
            val endTime = binding.endTimeTextView.text.toString().trim()

            if (title.isEmpty()) {
                binding.editTaskName.error = "Please enter title"
                return@setOnClickListener
            }

            if (startTime.isNotEmpty() && endTime.isNotEmpty()
                && selectedStartHour >= 0 && selectedEndHour >= 0
            ) {
                val startMin = selectedStartHour * 60 + selectedStartMinute
                val endMin = selectedEndHour * 60 + selectedEndMinute
                if (endMin <= startMin) {
                    Toast.makeText(requireContext(), "End time must be later than start time", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            if (taskId == null) {
                // CREATE
                createTask(title, description, date, startTime, endTime)
            } else {
                // UPDATE
                updateTask(taskId, title, description, date, startTime, endTime)
            }
        }

        return root
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
        binding.repeatTextView.setOnClickListener {
            val names = arrayOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")
            val checked = repeatToggles.toBooleanArray()
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Repeat On")
                .setMultiChoiceItems(names, checked) { _, idx, isChecked ->
                    repeatToggles[idx] = isChecked
                }
                .setPositiveButton("OK") { _, _ -> setupRepeatButtons() }
                .setNegativeButton("Cancel", null)
                .show()
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
        val h = if (initHour >= 0) initHour else 9
        val m = if (initMin >= 0) initMin else 0
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                when (textView) {
                    binding.startTimeTextView -> { selectedStartHour = hour; selectedStartMinute = minute }
                    binding.endTimeTextView -> { selectedEndHour = hour; selectedEndMinute = minute }
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

    private fun showDatePickerDialog() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val selected = LocalDate.of(y, m + 1, d)
                binding.taskDate.setText(selected.toString())
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun createTask(title: String, description: String, date: String, start: String, end: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/tasks") // <- lowercase
        val id = ref.push().key ?: UUID.randomUUID().toString()

        val task = Task(
            id = id,
            title = title,
            description = description,
            date = date,
            startTime = start,
            endTime = end,
            isCompleted = false
        )

        ref.child(id).setValue(task)
            .addOnSuccessListener {
                // Save repeats as auxiliary field (doesn't require Task to have 'repeats')
                ref.child(id).child("repeats").setValue(repeatToggles)
                // Refresh calendar
                ViewModelProvider(requireActivity())[NotesViewModel::class.java].addTask(task)
                Toast.makeText(requireContext(), "Task saved", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener {
                Log.e(TAG, "createTask failed", it)
                Toast.makeText(requireContext(), "Failed to save task", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTask(taskId: String, title: String, description: String, date: String, start: String, end: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/tasks")

        ref.orderByChild("id").equalTo(taskId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val updates = mapOf(
                        "title" to title,
                        "description" to description,
                        "date" to date,
                        "startTime" to start,
                        "endTime" to end,
                        "repeats" to repeatToggles
                    )
                    snapshot.children.firstOrNull()?.ref?.updateChildren(updates)
                        ?.addOnSuccessListener {
                            Toast.makeText(requireContext(), "Task updated", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                        ?.addOnFailureListener {
                            Log.e(TAG, "updateTask failed", it)
                            Toast.makeText(requireContext(), "Failed to update task", Toast.LENGTH_SHORT).show()
                        }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "updateTask:onCancelled", error.toException())
                }
            })
    }

    private fun loadTask(taskId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/tasks")

        ref.orderByChild("id").equalTo(taskId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.firstOrNull()?.getValue(Task::class.java)?.let { t ->
                        binding.editTaskName.setText(t.title)
                        binding.noteBody.setText(t.description)
                        binding.taskDate.setText(t.date)
                        binding.startTimeTextView.text = t.startTime
                        binding.endTimeTextView.text = t.endTime
                        // Try to read repeats if present
                        snapshot.children.firstOrNull()?.child("repeats")?.let { node ->
                            val list = (0..6).map { idx -> node.child(idx.toString()).getValue(Boolean::class.java) == true }
                            list.forEachIndexed { i, v -> repeatToggles[i] = v }
                            setupRepeatButtons()
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "loadTask:onCancelled", error.toException())
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
