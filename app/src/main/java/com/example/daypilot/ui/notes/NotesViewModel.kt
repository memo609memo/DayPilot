package com.example.daypilot.ui.notes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.applandeo.materialcalendarview.EventDay
import java.util.Collections.emptyList
import android.util.Log
class NotesViewModel : ViewModel() {
   private  val  _selectedDate = MutableLiveData<String>()
   val selectedDate: LiveData<String> = _selectedDate
   private val taskMap = mutableMapOf<String, MutableList<Task>>()


   private val _eventList = MutableLiveData<List<EventDay>>(emptyList())
   private val _tasksForSelectedDate = MutableLiveData<List<Task>>()

   private var currentSelectedDate: String = ""

   val tasksForSelectedDate: LiveData<List<Task>>get()=_tasksForSelectedDate
   val eventList: LiveData<List<EventDay>> = _eventList
   fun selectedDate(dateString: String)
   {
      _selectedDate.value = dateString
   }

   fun addEvent(event:EventDay){

      val current = _eventList.value?.toMutableList() ?: mutableListOf()
      current.add(event)
      _eventList.value = current
   }


   fun addTask(task: Task){
      val dateTasks = taskMap.getOrPut(task.date){ mutableListOf()}
      dateTasks.add(task)

      Log.d("TaskSaveDebug", "Task added: ${task.title} on ${task.date}")

      if(currentSelectedDate ==task.date){
         _tasksForSelectedDate.value = dateTasks.toList()
      }
   }

   fun getTasksForDate(date: String){
      currentSelectedDate = date
      _tasksForSelectedDate.value = taskMap[date]?.toList() ?: emptyList()
   }
}