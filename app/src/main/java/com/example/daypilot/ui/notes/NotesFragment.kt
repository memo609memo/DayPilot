package com.example.daypilot.ui.notes

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private lateinit var notesViewModel: NotesViewModel
    private lateinit var adapter: TaskAdapter

    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    private val ref = FirebaseDatabase.getInstance().getReference("users/$uid/Tasks")

    private var selectedLocalDate: LocalDate = LocalDate.now()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        notesViewModel = ViewModelProvider(this)[NotesViewModel::class.java]
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        val root = binding.root

        setupRecyclerView()
        setupCalendar()

        notesViewModel.tasksForSelectedDate.observe(viewLifecycleOwner) { tasks ->
            adapter.submitList(tasks)
            binding.monthCalendarView.notifyCalendarChanged()
        }

        notesViewModel.loadAllTasksOnceFromFirebase {
            Log.d("DebugCheck", "Finished loading tasks from Firebase")
            binding.monthCalendarView.notifyCalendarChanged()
        }

        binding.btnAddTask.setOnClickListener {
            notesViewModel.selectedDate.value?.let { selected ->
                showAddTaskDialog(selected)
            } ?: Toast.makeText(requireContext(), "Please select a date first", Toast.LENGTH_SHORT).show()
        }

        return root
    }

    private fun setupRecyclerView() {
        adapter = TaskAdapter { clickedTask ->
            val bundle = Bundle().apply { putString("taskId", clickedTask.id) }
            findNavController().navigate(R.id.action_navigation_notes_to_notifications, bundle)
        }
        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTasks.adapter = adapter

        ItemTouchHelper(SwipeToActionCallback(
            requireContext(),
            adapter,
            onEdit = { position -> showEditTaskDialog(adapter.currentList[position]) },
            onDelete = { position ->
                val task = adapter.currentList[position]
                notesViewModel.deleteTask(task)
                notesViewModel.getTasksForDate(task.date)
                binding.monthCalendarView.notifyCalendarChanged()
            }
        )).attachToRecyclerView(binding.recyclerViewTasks)
    }

    private fun setupCalendar() {
        val currentDate = LocalDate.now()
        val currentMonth = YearMonth.now()
        val daysOfWeek = daysOfWeek()
        val titleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

        val monthView = binding.monthCalendarView
        monthView.dayViewResource = R.layout.calendar_month_layout
        binding.textViewMonthTitle.text = titleFormatter.format(currentMonth)

        monthView.monthScrollListener = { month ->
            binding.textViewMonthTitle.text = titleFormatter.format(month.yearMonth)
        }

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
                            isSelected -> ContextCompat.getColor(requireContext(), android.R.color.white)
                            isToday -> ContextCompat.getColor(requireContext(), R.color.purple_500)
                            else -> ContextCompat.getColor(requireContext(), android.R.color.black)
                        }
                    )
                    container.dayNumberText.setTypeface(null, if (isSelected || isToday) Typeface.BOLD else Typeface.NORMAL)
                    container.selectedBackground.visibility = if (isSelected) View.VISIBLE else View.GONE
                    container.eventDot.visibility = if (hasTasks) View.VISIBLE else View.GONE

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
                    container.dayNumberText.setTextColor(ContextCompat.getColor(requireContext(), R.color.disabled_day))
                    container.dayNumberText.setTypeface(null, Typeface.NORMAL)
                    container.selectedBackground.visibility = View.GONE
                    container.eventDot.visibility = View.GONE
                    container.view.setOnClickListener(null)
                }
            }
        }

        monthView.setup(currentMonth.minusMonths(12), currentMonth.plusMonths(12), daysOfWeek.first())
        monthView.scrollToDate(currentDate)

        binding.buttonPreviousMonth.setOnClickListener {
            monthView.findFirstVisibleMonth()?.yearMonth?.minusMonths(1)?.let(monthView::scrollToMonth)
        }
        binding.buttonNextMonth.setOnClickListener {
            monthView.findFirstVisibleMonth()?.yearMonth?.plusMonths(1)?.let(monthView::scrollToMonth)
        }
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
                    val task = Task(System.currentTimeMillis().toString(), title, description, date)
                    notesViewModel.addTask(task)
                    notesViewModel.getTasksForDate(date)
                    ref.push().setValue(task).addOnFailureListener {
                        Toast.makeText(requireContext(), "Could not add task to database", Toast.LENGTH_SHORT).show()
                    }
                    binding.monthCalendarView.notifyCalendarChanged()
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
                    binding.monthCalendarView.notifyCalendarChanged()
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
