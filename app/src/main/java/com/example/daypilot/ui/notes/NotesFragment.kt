package com.example.daypilot.ui.notes


import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import com.example.daypilot.databinding.FragmentNotesBinding
import java.util.Calendar

class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!
    private lateinit var notesViewModel: NotesViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        notesViewModel = ViewModelProvider(this).get(NotesViewModel::class.java)
        _binding = FragmentNotesBinding.inflate(inflater,container,false)
        val root : View = binding.root


        binding.calendarView.setOnDayClickListener(object : OnDayClickListener{
            override  fun  onDayClick(eventDay: EventDay){
                val clickedCalendar = eventDay.calendar
                //Save date to viewModel
                val formattedDate = formatCalendar(clickedCalendar)
                notesViewModel.selectedDate(formattedDate)

                val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.logo1)

                if (drawable!= null){
                    val event = EventDay(clickedCalendar,drawable)
                    val eventList = ArrayList<EventDay>()
                    eventList.add(event)
                    binding.calendarView.setEvents(eventList)
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




    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }



}

private fun formatCalendar(calendar: Calendar): String{
    val  formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return formatter.format(calendar.time)
}