package com.example.daypilot.ui.notes


import android.app.TimePickerDialog
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.daypilot.R
import com.example.daypilot.databinding.FragmentNotesBinding
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
    private lateinit var notesViewModel: NotesViewModel
    private  lateinit var adapter: TaskAdapter
    private lateinit var hourBlockAdapter: HourBlockAdapter
    private var selectedLocalDate: LocalDate = LocalDate.now()

    //Michael: Adding this for realtime DB
    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    val ref = FirebaseDatabase.getInstance().getReference("users/$uid/tasks")



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        notesViewModel = ViewModelProvider(this).get(NotesViewModel::class.java)
        val isDark = requireContext().isDarkMode()

        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        val root = binding.root


        val dragListener = object : TaskDragListener{
            override fun onTaskMoved(task: Task, fromHour: Int, toHour: Int) {
                notesViewModel.rescheduleTask(task, fromHour, toHour)
            }
        }

        hourBlockAdapter = HourBlockAdapter(
            onEdit = { task -> showEditTaskDialog(task) },
            onDelete = { task ->
                notesViewModel.deleteTask(task)
                notesViewModel.getTasksForDate(task.date)
                val date = LocalDate.parse(task.date)
                binding.monthCalendarView.notifyDateChanged(date)
                binding.weekCalendarView.notifyDateChanged(date)
            },
            onComplete = { task -> 
                notesViewModel.markTaskAsCompleted(task)
                notesViewModel.getTasksForDate(task.date)
                val date = LocalDate.parse(task.date)
                binding.monthCalendarView.notifyDateChanged(date)
                binding.weekCalendarView.notifyDateChanged(date)
            },
            onTaskClick = { task ->
                val bundle = Bundle().apply {
                    putString("taskId", task.id)
                }
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
        val autoScrollDragListener = TaskAutoScrollDragListener(binding.recyclerViewTasks)
        binding.root.setOnDragListener(autoScrollDragListener)



        // Attach swipe callback
        /*val swipeCallback = SwipeToActionCallback(
             requireContext(),
             adapter,
             onEdit = { position ->
                 val task = adapter.currentList[position]
                 showEditTaskDialog(task)
             },
             onDelete = { position ->
                 val task = adapter.currentList[position]
                 notesViewModel.deleteTask(task)

                 //Reload tasks and refresh the red dot for that date
                 notesViewModel.getTasksForDate(task.date)
                 val date = LocalDate.parse(task.date)
                 binding.monthCalendarView.notifyDateChanged(date)
                 binding.weekCalendarView.notifyDateChanged(date)
             }
         )*/
        //ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerViewTasks)

        // Observe tasks for selected date to update RecyclerView
        notesViewModel.tasksForSelectedDate.observe(viewLifecycleOwner) { tasks ->
            //adapter.submitList(tasks)
            binding.textViewNoTasks.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
            val hourBlocks = notesViewModel.buildHourBlocksFromTasks(tasks)
            hourBlockAdapter.showEmptyMessage = tasks.isNotEmpty()


            hourBlockAdapter.submitList(hourBlocks){
                val firstTaskPosition = hourBlockAdapter.getFirstTaskPosition()
                if(firstTaskPosition != RecyclerView.NO_POSITION){
                    binding.recyclerViewTasks.post {
                        binding.recyclerViewTasks.smoothScrollToPosition(firstTaskPosition)
                    }
                }
            }
        }

        notesViewModel.preloadAllTasks{
            binding.monthCalendarView.notifyCalendarChanged()
            binding.weekCalendarView.notifyCalendarChanged()
        }

        // Setup initial dates
        val currentDate = LocalDate.now()
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(12)
        val endMonth = currentMonth.plusMonths(12)
        val daysOfWeek = daysOfWeek()

        //  Setup Month CalendarView
        val monthView = binding.monthCalendarView
        val titleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")


        monthView.dayViewResource = R.layout.calendar_month_layout

        monthView.monthScrollListener = { month ->
            binding.textViewMonthTitle.text = titleFormatter.format(month.yearMonth)
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
                    // Normal styling for current month
                    container.dayNumberText.setTextColor(
                        when {
                            !isDark && isSelected -> resources.getColor(android.R.color.white, null)
                            isDark && isSelected -> resources.getColor(android.R.color.black, null)
                            !isDark && isToday -> resources.getColor(R.color.today, null)
                            isDark && isToday -> resources.getColor(R.color.dark_today, null)
                            else -> {
                                if (!isDark) {
                                    resources.getColor(android.R.color.black, null)
                                }
                                else {
                                    resources.getColor(android.R.color.white, null)
                                }
                            }
                        }
                    )
                    container.dayNumberText.setTypeface(null, if (isSelected || isToday) Typeface.BOLD else Typeface.NORMAL)

                    container.selectedBackground.visibility = if (isSelected) View.VISIBLE else View.GONE
                    container.eventDot.visibility = if (hasTasks) View.VISIBLE else View.GONE

                    container.view.isEnabled = true
                    container.view.setOnClickListener {
                        if (selectedLocalDate != date) {
                            val oldDate = selectedLocalDate
                            selectedLocalDate = date
                            monthView.notifyDateChanged(oldDate)
                            monthView.notifyDateChanged(date)

                            notesViewModel.selectedDate(date.toString())
                            notesViewModel.getTasksForDate(date.toString())
                        }
                    }
                } else {
                    // Disabled styling for days outside current month
                    if (!isDark) {
                        container.dayNumberText.setTextColor(ContextCompat.getColor(requireContext(), R.color.disabled_day))
                    }
                    else {
                        container.dayNumberText.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_disabled_day))
                    }
                    container.dayNumberText.setTypeface(null, Typeface.NORMAL)

                    container.selectedBackground.visibility = View.GONE
                    container.eventDot.visibility = View.GONE

                    container.view.isEnabled = false
                    container.view.setOnClickListener(null)
                }
            }
        }
        monthView.setup(startMonth, endMonth, daysOfWeek.first())
        monthView.scrollToDate(currentDate)

        binding.buttonPreviousMonth.setOnClickListener {
            val previousMonth = monthView.findFirstVisibleMonth()?.yearMonth?.minusMonths(1)
            if (previousMonth != null) {
                monthView.scrollToMonth(previousMonth)
            }
        }

        binding.buttonNextMonth.setOnClickListener {
            val nextMonth = monthView.findFirstVisibleMonth()?.yearMonth?.plusMonths(1)
            if (nextMonth != null) {
                monthView.scrollToMonth(nextMonth)
            }
        }

        // Setup Week CalendarView
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
                            if (!isDark) {
                                resources.getColor(android.R.color.black, null)
                            }
                            else {
                                resources.getColor(android.R.color.white, null)
                            }
                        }
                    }
                )

                container.view.setOnClickListener {
                    if (selectedWeekDate != date) {
                        val oldDate = selectedWeekDate
                        selectedWeekDate = date
                        weekView.notifyDateChanged(oldDate)
                        weekView.notifyDateChanged(selectedWeekDate)

                        selectedLocalDate = date
                        notesViewModel.selectedDate(date.toString())
                        notesViewModel.getTasksForDate(date.toString())
                        binding.headerDateText.text = formatHeaderDate(date)
                    }
                }
            }
        }
        binding.headerDateText.text = formatHeaderDate(currentDate)

        // Observe selected date changes and show toast
        notesViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            //Toast.makeText(requireContext(), "Selected: $date", Toast.LENGTH_SHORT).show()
        }

        // Switch calendar visibility
        binding.switchCalendar.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Switch to week view
                binding.monthCalendarView.visibility = View.GONE
                binding.weekCalendarView.visibility = View.VISIBLE
                binding.monthWeekdayLabels.visibility = View.GONE
                binding.headerDateText.visibility = View.VISIBLE
                binding.monthNavigation.visibility = View.GONE
                binding.textViewToggleLabel.text ="Weekly View:"
            } else {
                // Switch to month view
                binding.monthCalendarView.visibility = View.VISIBLE
                binding.weekCalendarView.visibility = View.GONE
                binding.monthWeekdayLabels.visibility = View.VISIBLE
                binding.headerDateText.visibility = View.GONE
                binding.monthNavigation.visibility = View.VISIBLE
                binding.textViewToggleLabel.text ="Monthly View:"
            }
        }

        // Set initial visibility
        binding.monthCalendarView.visibility = View.VISIBLE
        binding.weekCalendarView.visibility = View.GONE
        binding.headerDateText.visibility = View.GONE

        // Add task button
        binding.btnAddTask.setOnClickListener {
            val selected = notesViewModel.selectedDate.value
            if (selected != null) {
                showAddTaskDialog(selected)
            } else {
                Toast.makeText(requireContext(), "Please select a date first", Toast.LENGTH_SHORT).show()
            }
        }
        val initiallySelectedDate = selectedLocalDate.toString()
        notesViewModel.selectedDate(initiallySelectedDate)
        notesViewModel.getTasksForDate(initiallySelectedDate)
        return root
    }


    private fun showAddTaskDialog(date: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val titleInputLayout = dialogView.findViewById<TextInputLayout>(R.id.titleInputLayout)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.editTextTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.editTextDescription)
        val startTimeLayout = dialogView.findViewById<TextInputLayout>(R.id.startTimeLayout)
        val endTimeLayout = dialogView.findViewById<TextInputLayout>(R.id.endTimeLayout)
        val startTimeInput = dialogView.findViewById<TextInputEditText>(R.id.editTextStartTime)
        val endTimeInput = dialogView.findViewById<TextInputEditText>(R.id.editTextEndTime)
        val prioritySpinner = dialogView.findViewById<Spinner>(R.id.spinnerPriority)

        //Populate Spinner
        val priorityList = listOf("Select Priority (Optional)") + Priority.values().map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, priorityList)
            prioritySpinner.adapter = adapter

        // Set up TimePickers
        startTimeInput.setOnClickListener {
            showTimePicker(isStartTime = true) { times ->
                startTimeInput.setText(times.first)
                times.second?.let { endTimeInput.setText(it) }
            }
        }
        endTimeInput.setOnClickListener {
            showTimePicker(isStartTime = false) { times ->
                endTimeInput.setText(times.first) // only end time
            }
        }

       val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setTitle("Add Task")
            .setView(dialogView)
            .setPositiveButton("Save", null)
           .setNegativeButton("Cancel", null)
           .create()



        dialog.setOnShowListener{
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            val isDark = requireContext().isDarkMode()
            if (!isDark) {
                saveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color))
                cancelButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color))
            }
            else {
                saveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_text_color))
                cancelButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_text_color))
            }

            saveButton.setOnClickListener{
                val title = titleInput.text.toString()
                val description = descriptionInput.text.toString()
                val startTime = startTimeInput.text.toString()
                val endTime = endTimeInput.text.toString()

                val selectedPosition = prioritySpinner.selectedItemPosition
                val selectedPriority = if (selectedPosition == 0) {
                    Priority.DEFAULT // default if none selected
                } else {
                    Priority.values()[selectedPosition - 1]
                }

                var isValid = true



                //validation(edge cases)

                if (title.isBlank()) {
                    titleInputLayout.error = "Title is required"
                    val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
                    titleInputLayout.startAnimation(shake)
                    isValid = false
                }else{
                    titleInputLayout.error = null
                }

                // Validate time
                val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
                if (startTime.isNotBlank() && endTime.isNotBlank()) {
                    try {

                        val today = LocalDate.now()
                        val start = LocalDateTime.of(today, LocalTime.parse(startTime.uppercase(), formatter))
                        var end = LocalDateTime.of(today, LocalTime.parse(endTime.uppercase(), formatter))

                        // If end is before start, assume next day
                        if (end.isBefore(start)) {
                            end = end.plusDays(1)
                        }

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
                    } catch (e: DateTimeParseException) {
                        startTimeLayout.error = "Invalid format"
                        endTimeLayout.error = "Invalid format"
                        val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
                        startTimeLayout.startAnimation(shake)
                        endTimeLayout.startAnimation(shake)

                        isValid = false
                    }
                } else {
                    // Clear errors if empty
                    startTimeLayout.error = null
                    endTimeLayout.error = null
                }

                // Only proceed if all fields are valid
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
                binding.monthCalendarView.notifyDateChanged(taskDate)
                binding.weekCalendarView.notifyDateChanged(taskDate)

                notesViewModel.getTasksForDate(date)




                /*ref.push().setValue(task).addOnFailureListener{
                    Toast.makeText(requireContext(),"Could not add task to database",Toast.LENGTH_SHORT).show()
                }*/
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
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, selectedHour)
            cal.set(Calendar.MINUTE, selectedMinute)

            // Format to 12-hour with AM/PM
            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val formattedTime = formatter.format(cal.time)

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

            titleInput.setText(task.title)
            descriptionInput.setText(task.description)
            startTimeInput.setText(task.startTime)
            endTimeInput.setText(task.endTime)



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

                    var isValid = true

                    if (title.isBlank()) {
                        titleInputLayout.error = "Title is required"
                        val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
                        titleInputLayout.startAnimation(shake)
                        isValid = false
                    } else {
                        titleInputLayout.error = null
                    }

                    val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
                    if (startTime.isNotBlank() && endTime.isNotBlank()) {
                        try {
                            val start = LocalTime.parse(startTime, formatter)
                            val end = LocalTime.parse(endTime, formatter)

                            if (start >= end) {
                                startTimeLayout.error = "Start must be before end"
                                endTimeLayout.error = "End must be after start"
                                val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
                                startTimeLayout.startAnimation(shake)
                                endTimeLayout.startAnimation(shake)
                                isValid = false
                            } else {
                                startTimeLayout.error = null
                                endTimeLayout.error = null
                            }
                        } catch (e: DateTimeParseException) {
                            startTimeLayout.error = "Invalid time"
                            endTimeLayout.error = "Invalid time"
                            val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
                            startTimeLayout.startAnimation(shake)
                            endTimeLayout.startAnimation(shake)
                            isValid = false
                        }
                    }

                    if (!isValid) return@setOnClickListener

                    // Create updated task with same ID
                    val updatedTask = task.copy(
                        title = title,
                        description = description,
                        startTime = startTime,
                        endTime = endTime
                    )

                    notesViewModel.updateTask(updatedTask)

                    // Optionally update in Firebase too
                    val query = ref.orderByChild("id").equalTo(task.id)
                    query.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (child in snapshot.children) {
                                child.ref.setValue(updatedTask)
                            }
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


