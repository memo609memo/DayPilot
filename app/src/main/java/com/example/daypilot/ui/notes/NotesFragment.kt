package com.example.daypilot.ui.notes


import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import com.applandeo.materialcalendarview.listeners.OnDayLongClickListener
import com.example.daypilot.R
import com.example.daypilot.databinding.FragmentNotesBinding
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!
    private lateinit var notesViewModel: NotesViewModel
    private  lateinit var adapter: TaskAdapter

    private var selectedLocalDate: LocalDate? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        notesViewModel = ViewModelProvider(this).get(NotesViewModel::class.java)
        _binding = FragmentNotesBinding.inflate(inflater,container,false)


        val root : View = binding.root

        adapter = TaskAdapter()
        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTasks.adapter = adapter

        notesViewModel.tasksForSelectedDate.observe(viewLifecycleOwner, object : Observer<List<Task>> {
            override fun onChanged(tasks: List<Task>) {
                adapter.submitList(tasks)
            }
        })


        val currentDate = LocalDate.now()
        val currentMonth = YearMonth.now()
        val startDate = currentMonth.minusMonths(12).atDay(1)
        val endDate = currentMonth.plusMonths(12).atEndOfMonth()
        val daysOfWeek = daysOfWeek()
        val weekView = binding.weekCalendarView

        weekView.dayViewResource = R.layout.calendar_day_layout
        weekView.setup(startDate, endDate, daysOfWeek.first())
        weekView.scrollToDate(currentDate)

        var selectedDate = currentDate
         var eventDatesSet: Set<LocalDate> = emptySet()

        binding.headerDateText.text = formatHeaderDate(currentDate)

        weekView.dayBinder = object : com.kizitonwose.calendar.view.WeekDayBinder<WeekDayViewContainer> {
            override fun create(view: View) = WeekDayViewContainer(view)

            override fun bind(container: WeekDayViewContainer, data: com.kizitonwose.calendar.core.WeekDay) {
                container.day = data
                val date = data.date
                val currentMonth = selectedDate.month
                val today = LocalDate.now()

                val isSelected = date == selectedDate
                val isCurrentMonth = date.month == currentMonth
                val isToday = date == today
                val hasTasks = notesViewModel.hasTasksForDate(date.toString())

                // Set texts
                container.dayNumberText.text = date.dayOfMonth.toString()
                container.dayOfWeekText.text = date.dayOfWeek.name.take(3).replaceFirstChar { it.uppercase() }

                // Selection background
                container.selectedBackground.visibility = if (isSelected) View.VISIBLE else View.GONE

                // Day number text color
                when {
                    isSelected -> container.dayNumberText.setTextColor(resources.getColor(android.R.color.white))
                    !isCurrentMonth -> container.dayNumberText.setTextColor(Color.GRAY)
                    isToday -> {
                        container.dayNumberText.setTextColor(resources.getColor(R.color.purple_500))
                        container.dayNumberText.setTypeface(null, Typeface.BOLD)
                    }
                    else -> {
                        container.dayNumberText.setTextColor(resources.getColor(android.R.color.black))
                        container.dayNumberText.setTypeface(null, Typeface.NORMAL)
                    }
                }

                // Day of week text color (lighter for non-current month)
                container.dayOfWeekText.setTextColor(
                    if (isCurrentMonth) resources.getColor(android.R.color.darker_gray)
                    else Color.LTGRAY
                )

                // Show or hide event dot
                container.eventDot.visibility = if (hasTasks) View.VISIBLE else View.GONE

                // Click listener
                container.view.setOnClickListener {
                    if (selectedDate != date) {
                        val oldDate = selectedDate
                        selectedDate = date
                        weekView.notifyDateChanged(oldDate)
                        weekView.notifyDateChanged(selectedDate)

                        selectedLocalDate = date
                        val formatted = date.toString()
                        notesViewModel.selectedDate(formatted)
                        notesViewModel.getTasksForDate(formatted)

                        binding.headerDateText.text = formatHeaderDate(date)
                    }
                }
            }
        }

        binding.calendarView.setOnDayClickListener(object : OnDayClickListener{
            override  fun  onDayClick(eventDay: EventDay){
                val formattedDate = formatCalendar(eventDay.calendar)
                notesViewModel.selectedDate(formattedDate)
                notesViewModel.getTasksForDate(formattedDate)
                //showAddTaskDialog(formattedDate)
            }
        })

        binding.btnAddTask.setOnClickListener{
            val selected = notesViewModel.selectedDate.value

            if(selected!= null){
                showAddTaskDialog(selected)
            }else Toast.makeText(context, "Please select a date first", Toast.LENGTH_SHORT).show()
        }

        binding.calendarView.setOnDayLongClickListener(object : OnDayLongClickListener{
            override fun onDayLongClick(eventDay: EventDay){
                val formattedDate = formatCalendar(eventDay.calendar)

                showAddTaskDialog(formattedDate)
            }
        })
        val observer = object : Observer<String>{
            override fun onChanged(date: String){
                Toast.makeText(requireContext(), "Selected: $date", Toast.LENGTH_SHORT).show()
            }
        }
       notesViewModel.selectedDate.observe(viewLifecycleOwner,observer)


        binding.switchCalendar.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.calendarView.visibility = View.GONE
                binding.weekCalendarView.visibility = View.VISIBLE
                binding.headerDateText.visibility = View.VISIBLE
            } else {
                binding.calendarView.visibility = View.VISIBLE
                binding.weekCalendarView.visibility = View.GONE
                binding.headerDateText.visibility = View.GONE
            }
        }

        binding.btnAddTask.setOnClickListener {
            val selected = notesViewModel.selectedDate.value
            if (selected != null) {
                showAddTaskDialog(selected)
            } else {
                Toast.makeText(context, "Please select a date first", Toast.LENGTH_SHORT).show()
            }
        }



      return  root


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





    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }



}

private fun formatCalendar(calendar: Calendar): String{
    val  formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return formatter.format(calendar.time)
}

private fun formatHeaderDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())
    return date.format(formatter)
}