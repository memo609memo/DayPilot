package com.example.daypilot.ui.notes


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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import com.applandeo.materialcalendarview.listeners.OnDayLongClickListener
import com.example.daypilot.R
import com.example.daypilot.databinding.FragmentNotesBinding
import java.util.Calendar


class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!
    private lateinit var notesViewModel: NotesViewModel
    private  lateinit var adapter: TaskAdapter


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
                val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_notifications_black_24dp)
                val clickedCalendar = eventDay.calendar

                Log.d("CalendarDebug", "Long clicked on: ${clickedCalendar.time}")

                if (drawable!= null){
                    val event = EventDay(clickedCalendar,drawable)
                    notesViewModel.addEvent(event)
                }
                else{
                    Toast.makeText(requireContext(),"Could not load Icon", Toast.LENGTH_SHORT).show()
                }

            }
        })

        val observer = object : Observer<String>{
            override fun onChanged(date: String){
                Toast.makeText(requireContext(), "Selected: $date", Toast.LENGTH_SHORT).show()
            }
        }

       notesViewModel.selectedDate.observe(viewLifecycleOwner,observer)

        val observerEventDay = object: Observer<List<EventDay>>{
            override fun onChanged(events: List<EventDay>){
                if(events!=null){
                    binding.calendarView.setEvents(events)
                }
            }
        }
        notesViewModel.eventList.observe(viewLifecycleOwner, observerEventDay)








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
