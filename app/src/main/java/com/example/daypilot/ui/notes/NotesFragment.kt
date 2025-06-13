package com.example.daypilot.ui.notes


import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.daypilot.R
import com.example.daypilot.databinding.FragmentNotesBinding
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthDayBinder
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!
    private lateinit var notesViewModel: NotesViewModel
    private  lateinit var adapter: TaskAdapter

    private var selectedLocalDate: LocalDate = LocalDate.now()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        notesViewModel = ViewModelProvider(this).get(NotesViewModel::class.java)
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        val root = binding.root

        // Setup RecyclerView and adapter for tasks
        adapter = TaskAdapter()
        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTasks.adapter = adapter

        // Attach swipe callback
        val swipeCallback = SwipeToActionCallback(
            requireContext(),
            adapter,
            onEdit = { position ->
                val task = adapter.currentList[position]
                showEditTaskDialog(task)
            },
            onDelete = { position ->
                val task = adapter.currentList[position]
                notesViewModel.deleteTask(task)
                notesViewModel.getTasksForDate(task.date)
            }
        )
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerViewTasks)

        // Observe tasks for selected date to update RecyclerView
        notesViewModel.tasksForSelectedDate.observe(viewLifecycleOwner) { tasks ->
            adapter.submitList(tasks)
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
                            isSelected -> ContextCompat.getColor(requireContext(), android.R.color.white)
                            isToday -> ContextCompat.getColor(requireContext(), R.color.purple_500)
                            else -> ContextCompat.getColor(requireContext(), android.R.color.black)
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
                    container.dayNumberText.setTextColor(ContextCompat.getColor(requireContext(), R.color.disabled_day))
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
                        isSelected -> resources.getColor(android.R.color.white, null)
                        isToday -> resources.getColor(R.color.purple_500, null)
                        else -> resources.getColor(android.R.color.black, null)
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
            Toast.makeText(requireContext(), "Selected: $date", Toast.LENGTH_SHORT).show()
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
            } else {
                // Switch to month view
                binding.monthCalendarView.visibility = View.VISIBLE
                binding.weekCalendarView.visibility = View.GONE
                binding.monthWeekdayLabels.visibility = View.VISIBLE
                binding.headerDateText.visibility = View.GONE
                binding.monthNavigation.visibility = View.VISIBLE
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

        return root
    }


    private fun showAddTaskDialog(date: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.editTextTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.editTextDescription)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Task")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleInput.text.toString()
                val description = descriptionInput.text.toString()
                if (title.isNotBlank()) {
                    val task = Task(title = title, description = description, date = date)

                    notesViewModel.addTask(task)

                    notesViewModel.getTasksForDate(date)
                } else {
                    Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditTaskDialog(task: Task) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.editTextTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.editTextDescription)

        titleInput.setText(task.title)
        descriptionInput.setText(task.description)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Task")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val updatedTitle = titleInput.text.toString()
                val updatedDescription = descriptionInput.text.toString()
                if (updatedTitle.isNotBlank()) {
                    val updatedTask = task.copy(title = updatedTitle, description = updatedDescription)
                    notesViewModel.updateTask(updatedTask)
                    notesViewModel.getTasksForDate(updatedTask.date)
                } else {
                    Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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