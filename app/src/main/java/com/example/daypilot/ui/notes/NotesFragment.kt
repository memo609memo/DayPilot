package com.example.daypilot.ui.notes

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.daypilot.R
import com.example.daypilot.data.TaskRepo
import com.example.daypilot.databinding.FragmentNotesBinding
import com.example.daypilot.ui.floatingbutton.FloatingButton
import com.example.daypilot.ui.home.HomeViewModel
import com.example.daypilot.ui.home.HomeViewModelFactory
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthDayBinder
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar
import java.util.Locale

class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private inline fun <T> withBinding(block: (FragmentNotesBinding) -> T): T? {
        val b = _binding ?: return null
        if (!isAdded) return null
        if (!viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return null
        return block(b)
    }

    private lateinit var notesViewModel: NotesViewModel
    private lateinit var hourBlockAdapter: HourBlockAdapter
    private var selectedLocalDate: LocalDate = LocalDate.now()

    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    private val ref = FirebaseDatabase.getInstance().getReference("users/$uid/tasks")

    // floating mic and ai response set up
    private val taskRepo = TaskRepo()
    private val homeVMFactory = HomeViewModelFactory(taskRepo)
    private val homeViewModel: HomeViewModel by activityViewModels { homeVMFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        notesViewModel = ViewModelProvider(this)[NotesViewModel::class.java]
        val isDark = requireContext().isDarkMode()

        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        val root = binding.root

        // floating mic overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(requireContext())) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        } else {
            FloatingButton(requireActivity())
        }

        homeViewModel.aiResponse.observe(viewLifecycleOwner) { reply ->
            if (reply?.success == true) {
                val bundle = bundleOf(
                    "description" to reply.description,
                    "date" to reply.date,
                    "time" to reply.time,
                    "weekday" to reply.weekday
                )
                findNavController().navigate(R.id.navigation_notifications, bundle)

                // clear response ao it not always consuming the data and filling in the page n stuck on task page
                homeViewModel.clearAiResponse()
            }
        }


        setupRecycler()
        setupCalendars(isDark)

        notesViewModel.tasksForSelectedDate.observe(viewLifecycleOwner) { tasks ->
            withBinding { b ->
                b.textViewNoTasks.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
                val hourBlocks = notesViewModel.buildHourBlocksFromTasks(tasks)
                hourBlockAdapter.showEmptyMessage = tasks.isNotEmpty()

                hourBlockAdapter.submitList(hourBlocks) {
                    withBinding { bb ->
                        val first = hourBlockAdapter.getFirstTaskPosition()
                        if (first != RecyclerView.NO_POSITION) {
                            bb.recyclerViewTasks.post {
                                withBinding { bbb ->
                                    bbb.recyclerViewTasks.smoothScrollToPosition(first)
                                }
                            }
                        }
                    }
                }
            }
        }

        notesViewModel.preloadAllTasks {
            withBinding { b ->
                b.monthCalendarView.notifyCalendarChanged()
                b.weekCalendarView.notifyCalendarChanged()
            }
        }

        binding.btnAddTask.setOnClickListener {
            notesViewModel.selectedDate.value?.let { selected ->
                showAddTaskDialog(selected, isDark)
            } ?: Toast.makeText(requireContext(), "Please select a date first", Toast.LENGTH_SHORT).show()
        }

        val initiallySelectedDate = selectedLocalDate.toString()
        notesViewModel.selectedDate(initiallySelectedDate)
        notesViewModel.getTasksForDate(initiallySelectedDate)

        return root
    }

    private fun setupRecycler() {
        val dragListener = object : TaskDragListener {
            override fun onTaskMoved(task: Task, fromHour: Int, toHour: Int) {
                notesViewModel.rescheduleTask(task, fromHour, toHour)
            }
        }

        hourBlockAdapter = HourBlockAdapter(
            onEdit = { task -> showEditTaskDialog(task) },
            onDelete = { task ->
                notesViewModel.deleteTask(task)
                notesViewModel.getTasksForDate(task.date)
                val d = LocalDate.parse(task.date)
                withBinding { b ->
                    b.monthCalendarView.notifyDateChanged(d)
                    b.weekCalendarView.notifyDateChanged(d)
                }
            },
            onComplete = { task ->
                notesViewModel.markTaskAsCompleted(task)
                notesViewModel.getTasksForDate(task.date)
                val d = LocalDate.parse(task.date)
                withBinding { b ->
                    b.monthCalendarView.notifyDateChanged(d)
                    b.weekCalendarView.notifyDateChanged(d)
                }
            },
            onTaskClick = { task ->
                val bundle = Bundle().apply { putString("taskId", task.id) }
                findNavController().navigate(R.id.action_navigation_notes_to_notifications, bundle)
            },
            dragListener = dragListener,
            dragHelper = null
        )

        val dragHelper = TaskDragAndDropHelper(hourBlockAdapter, dragListener)
        hourBlockAdapter.dragHelper = dragHelper

        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTasks.adapter = hourBlockAdapter
        ItemTouchHelper(dragHelper).attachToRecyclerView(binding.recyclerViewTasks)
        binding.root.setOnDragListener(TaskAutoScrollDragListener(binding.recyclerViewTasks))
    }

    private fun setupCalendars(isDark: Boolean) {
        val currentDate = LocalDate.now()
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(12)
        val endMonth = currentMonth.plusMonths(12)
        val daysOfWeek = daysOfWeek()
        val titleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

        val monthView = binding.monthCalendarView
        monthView.dayViewResource = R.layout.calendar_month_layout

        monthView.monthScrollListener = { month ->
            withBinding { b -> b.textViewMonthTitle.text = titleFormatter.format(month.yearMonth) }
        }
        binding.textViewMonthTitle.text = titleFormatter.format(currentMonth)

        monthView.dayBinder = object : MonthDayBinder<MonthDayViewContainer> {
            override fun create(view: View) = MonthDayViewContainer(view)
            override fun bind(container: MonthDayViewContainer, day: CalendarDay) {
                val date = day.date
                val isSelected = date == selectedLocalDate
                val isToday = date == currentDate
                val hasTasks = notesViewModel.hasTasksForDate(date.toString())

                container.day = day
                container.dayNumberText.text = date.dayOfMonth.toString()

                if (day.position == DayPosition.MonthDate) {
                    container.dayNumberText.setTextColor(
                        when {
                            !isDark && isSelected -> resources.getColor(android.R.color.white, null)
                            isDark && isSelected -> resources.getColor(android.R.color.black, null)
                            !isDark && isToday -> resources.getColor(R.color.today, null)
                            isDark && isToday -> resources.getColor(R.color.dark_today, null)
                            else -> {
                                if (!isDark) resources.getColor(android.R.color.black, null)
                                else resources.getColor(android.R.color.white, null)
                            }
                        }
                    )
                    container.dayNumberText.setTypeface(
                        null,
                        if (isSelected || isToday) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
                    )
                    container.selectedBackground.visibility = if (isSelected) View.VISIBLE else View.GONE
                    container.eventDot.visibility = if (hasTasks) View.VISIBLE else View.GONE

                    container.view.isEnabled = true
                    container.view.setOnClickListener {
                        if (selectedLocalDate != date) {
                            val old = selectedLocalDate
                            selectedLocalDate = date
                            monthView.notifyDateChanged(old)
                            monthView.notifyDateChanged(date)
                            notesViewModel.selectedDate(date.toString())
                            notesViewModel.getTasksForDate(date.toString())
                        }
                    }
                } else {
                    val disabledColor = if (!isDark) R.color.disabled_day else R.color.dark_disabled_day
                    container.dayNumberText.setTextColor(ContextCompat.getColor(requireContext(), disabledColor))
                    container.dayNumberText.setTypeface(null, android.graphics.Typeface.NORMAL)
                    container.selectedBackground.visibility = View.GONE
                    container.eventDot.visibility = View.GONE
                    container.view.isEnabled = false
                    container.view.setOnClickListener(null)
                }
            }
        }
        monthView.setup(startMonth, endMonth, daysOfWeek.first())
        monthView.scrollToDate(currentDate)

        val weekView = binding.weekCalendarView
        weekView.dayViewResource = R.layout.calendar_day_layout
        weekView.setup(currentDate.minusWeeks(12), currentDate.plusWeeks(12), daysOfWeek.first())
        weekView.scrollToDate(currentDate)

        var selectedWeekDate = currentDate
        weekView.dayBinder = object : com.kizitonwose.calendar.view.WeekDayBinder<WeekDayViewContainer> {
            override fun create(view: View) = WeekDayViewContainer(view)
            override fun bind(container: WeekDayViewContainer, data: com.kizitonwose.calendar.core.WeekDay) {
                val date = data.date
                val isSelected = date == selectedWeekDate
                val isToday = date == currentDate
                val hasTasks = notesViewModel.hasTasksForDate(date.toString())

                container.dayNumberText.text = date.dayOfMonth.toString()
                container.dayOfWeekText.text = date.dayOfWeek.name.take(3).replaceFirstChar { it.uppercase() }
                container.selectedBackground.visibility = if (isSelected) View.VISIBLE else View.GONE
                container.eventDot.visibility = if (hasTasks) View.VISIBLE else View.GONE

                container.dayNumberText.setTextColor(
                    when {
                        !isDark && isSelected -> resources.getColor(android.R.color.white, null)
                        isDark && isSelected -> resources.getColor(android.R.color.black, null)
                        !isDark && isToday -> resources.getColor(R.color.today, null)
                        isDark && isToday -> resources.getColor(R.color.dark_today, null)
                        else -> {
                            if (!isDark) resources.getColor(android.R.color.black, null)
                            else resources.getColor(android.R.color.white, null)
                        }
                    }
                )

                container.view.setOnClickListener {
                    if (selectedWeekDate != date) {
                        val old = selectedWeekDate
                        selectedWeekDate = date
                        weekView.notifyDateChanged(old)
                        weekView.notifyDateChanged(selectedWeekDate)

                        selectedLocalDate = date
                        notesViewModel.selectedDate(date.toString())
                        notesViewModel.getTasksForDate(date.toString())
                        withBinding { b -> b.headerDateText.text = formatHeaderDate(date) }
                    }
                }
            }
        }

        binding.headerDateText.text = formatHeaderDate(currentDate)

        binding.switchCalendar.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.monthCalendarView.visibility = View.GONE
                binding.weekCalendarView.visibility = View.VISIBLE
                binding.monthWeekdayLabels.visibility = View.GONE
                binding.headerDateText.visibility = View.VISIBLE
                binding.monthNavigation.visibility = View.GONE
                binding.textViewToggleLabel.text = "Weekly View:"
            } else {
                binding.monthCalendarView.visibility = View.VISIBLE
                binding.weekCalendarView.visibility = View.GONE
                binding.monthWeekdayLabels.visibility = View.VISIBLE
                binding.headerDateText.visibility = View.GONE
                binding.monthNavigation.visibility = View.VISIBLE
                binding.textViewToggleLabel.text = "Monthly View:"
            }
        }

        binding.monthCalendarView.visibility = View.VISIBLE
        binding.weekCalendarView.visibility = View.GONE
        binding.headerDateText.visibility = View.GONE

        binding.buttonPreviousMonth.setOnClickListener {
            monthView.findFirstVisibleMonth()?.yearMonth?.minusMonths(1)?.let(monthView::scrollToMonth)
        }
        binding.buttonNextMonth.setOnClickListener {
            monthView.findFirstVisibleMonth()?.yearMonth?.plusMonths(1)?.let(monthView::scrollToMonth)
        }
    }

    private fun showAddTaskDialog(date: String, isDark: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val titleInputLayout = dialogView.findViewById<TextInputLayout>(R.id.titleInputLayout)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.editTextTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.editTextDescription)
        val startTimeLayout = dialogView.findViewById<TextInputLayout>(R.id.startTimeLayout)
        val endTimeLayout = dialogView.findViewById<TextInputLayout>(R.id.endTimeLayout)
        val startTimeInput = dialogView.findViewById<TextInputEditText>(R.id.editTextStartTime)
        val endTimeInput = dialogView.findViewById<TextInputEditText>(R.id.editTextEndTime)
        val prioritySpinner = dialogView.findViewById<Spinner>(R.id.spinnerPriority)

        val priorityList = listOf("Select Priority (Optional)") + Priority.values().map { it.name }
        val priorityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, priorityList)
        prioritySpinner.adapter = priorityAdapter

        startTimeInput.setOnClickListener {
            showTimePicker(isStartTime = true) { times ->
                startTimeInput.setText(times.first)
                times.second?.let { endTimeInput.setText(it) }
            }
        }
        endTimeInput.setOnClickListener {
            showTimePicker(isStartTime = false) { times ->
                endTimeInput.setText(times.first)
            }
        }

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setTitle("Add Task")
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            if (!isDark) {
                saveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color))
                cancelButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color))
            } else {
                saveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_text_color))
                cancelButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_text_color))
            }

            saveButton.setOnClickListener {
                val title = titleInput.text.toString()
                val description = descriptionInput.text.toString()
                val startTime = startTimeInput.text.toString()
                val endTime = endTimeInput.text.toString()

                val selectedPosition = prioritySpinner.selectedItemPosition
                val selectedPriority =
                    if (selectedPosition == 0) Priority.DEFAULT else Priority.values()[selectedPosition - 1]

                var isValid = true

                if (title.isBlank()) {
                    titleInputLayout.error = "Title is required"
                    titleInputLayout.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.shake))
                    isValid = false
                } else {
                    titleInputLayout.error = null
                }

                val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
                if (startTime.isNotBlank() && endTime.isNotBlank()) {
                    try {
                        val today = LocalDate.now()
                        val start = LocalDateTime.of(today, LocalTime.parse(startTime.uppercase(), formatter))
                        var end = LocalDateTime.of(today, LocalTime.parse(endTime.uppercase(), formatter))
                        if (end.isBefore(start)) end = end.plusDays(1)
                        val durationMinutes = Duration.between(start, end).toMinutes()
                        if (durationMinutes <= 0 || durationMinutes > 8 * 60) {
                            startTimeLayout.error = "Start must be before end"
                            endTimeLayout.error = "End must be after start (max 8h)"
                            val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
                            startTimeLayout.startAnimation(shake)
                            endTimeLayout.startAnimation(shake)
                            isValid = false
                        } else {
                            startTimeLayout.error = null
                            endTimeLayout.error = null
                        }
                    } catch (_: DateTimeParseException) {
                        startTimeLayout.error = "Invalid format"
                        endTimeLayout.error = "Invalid format"
                        val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
                        startTimeLayout.startAnimation(shake)
                        endTimeLayout.startAnimation(shake)
                        isValid = false
                    }
                } else {
                    startTimeLayout.error = null
                    endTimeLayout.error = null
                }

                if (!isValid) return@setOnClickListener

                val task = Task(
                    id = System.currentTimeMillis().toString(),
                    title = title,
                    description = description,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    isCompleted = false,
                    priority = selectedPriority
                )
                notesViewModel.addTask(task)

                val taskDate = LocalDate.parse(task.date)
                withBinding { b ->
                    b.monthCalendarView.notifyDateChanged(taskDate)
                    b.weekCalendarView.notifyDateChanged(taskDate)
                }

                notesViewModel.getTasksForDate(date)

                ref.push().setValue(task).addOnFailureListener {
                    Toast.makeText(requireContext(), "Could not add task to database", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showTimePicker(isStartTime: Boolean, onTimeSelected: (Pair<String, String?>) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, selectedHour)
                set(Calendar.MINUTE, selectedMinute)
            }
            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val formattedStart = formatter.format(cal.time)

            if (isStartTime) {
                cal.add(Calendar.HOUR_OF_DAY, 1)
                val formattedEnd = formatter.format(cal.time)
                onTimeSelected(Pair(formattedStart, formattedEnd))
            } else {
                onTimeSelected(Pair(formattedStart, null))
            }
        }, hour, minute, false).show()
    }

    private fun showEditTaskDialog(task: Task) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val titleInputLayout = dialogView.findViewById<TextInputLayout>(R.id.titleInputLayout)
        val titleInput = dialogView.findViewById<EditText>(R.id.editTextTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.editTextDescription)
        val startTimeLayout = dialogView.findViewById<TextInputLayout>(R.id.startTimeLayout)
        val endTimeLayout = dialogView.findViewById<TextInputLayout>(R.id.endTimeLayout)
        val startTimeInput = dialogView.findViewById<TextInputEditText>(R.id.editTextStartTime)
        val endTimeInput = dialogView.findViewById<TextInputEditText>(R.id.editTextEndTime)
        val prioritySpinner = dialogView.findViewById<Spinner>(R.id.spinnerPriority)

        val priorityList = listOf("Select Priority (Optional)") + Priority.values().map { it.name }
        val priorityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, priorityList)
        prioritySpinner.adapter = priorityAdapter

        titleInput.setText(task.title)
        descriptionInput.setText(task.description)
        startTimeInput.setText(task.startTime)
        endTimeInput.setText(task.endTime)

        val currentPriorityIndex = task.priority?.let { Priority.values().indexOf(it) + 1 } ?: 0
        prioritySpinner.setSelection(currentPriorityIndex)

        startTimeInput.setOnClickListener {
            showTimePicker(isStartTime = true) { times ->
                startTimeInput.setText(times.first)
                times.second?.let { endTimeInput.setText(it) }
            }
        }
        endTimeInput.setOnClickListener {
            showTimePicker(isStartTime = false) { times ->
                endTimeInput.setText(times.first)
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Task")
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val title = titleInput.text.toString()
                val description = descriptionInput.text.toString()
                val startTime = startTimeInput.text.toString()
                val endTime = endTimeInput.text.toString()

                val selectedPosition = prioritySpinner.selectedItemPosition
                val selectedPriority =
                    if (selectedPosition == 0) Priority.DEFAULT else Priority.values()[selectedPosition - 1]

                var isValid = true
                if (title.isBlank()) {
                    titleInputLayout.error = "Title is required"
                    titleInputLayout.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.shake))
                    isValid = false
                } else {
                    titleInputLayout.error = null
                }

                val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
                if (startTime.isNotBlank() && endTime.isNotBlank()) {
                    try {
                        val today = LocalDate.now()
                        val start = LocalDateTime.of(today, LocalTime.parse(startTime.uppercase(), formatter))
                        var end = LocalDateTime.of(today, LocalTime.parse(endTime.uppercase(), formatter))
                        if (end.isBefore(start)) end = end.plusDays(1)
                        val durationMinutes = Duration.between(start, end).toMinutes()
                        if (durationMinutes <= 0 || durationMinutes > 8 * 60) {
                            startTimeLayout.error = "Start must be before end"
                            endTimeLayout.error = "End must be after start (max 8h)"
                            val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
                            startTimeLayout.startAnimation(shake)
                            endTimeLayout.startAnimation(shake)
                            isValid = false
                        } else {
                            startTimeLayout.error = null
                            endTimeLayout.error = null
                        }
                    } catch (_: DateTimeParseException) {
                        startTimeLayout.error = "Invalid time"
                        endTimeLayout.error = "Invalid time"
                        val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
                        startTimeLayout.startAnimation(shake)
                        endTimeLayout.startAnimation(shake)
                        isValid = false
                    }
                }

                if (!isValid) return@setOnClickListener

                val updatedTask = task.copy(
                    title = title,
                    description = description,
                    startTime = startTime,
                    endTime = endTime,
                    priority = selectedPriority
                )
                notesViewModel.updateTask(updatedTask)

                val query = ref.orderByChild("id").equalTo(task.id)
                query.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (child in snapshot.children) child.ref.setValue(updatedTask)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(requireContext(), "Update failed: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })

                dialog.dismiss()
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private fun formatHeaderDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())
    return date.format(formatter)
}
