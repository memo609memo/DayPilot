package com.example.daypilot.ui.notes

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Collections.emptyList


class NotesViewModel : ViewModel() {
   private  val  _selectedDate = MutableLiveData<String>()
   val selectedDate: LiveData<String> = _selectedDate
   private val taskMap = mutableMapOf<String, MutableList<Task>>()


   private val _tasksForSelectedDate = MutableLiveData<List<Task>>()

   private var currentSelectedDate: String = ""

   val tasksForSelectedDate: LiveData<List<Task>>get()=_tasksForSelectedDate
   fun selectedDate(dateString: String)
   {
      _selectedDate.value = dateString
   }




   fun addTask(task: Task) {
      val dateTasks = taskMap.getOrPut(task.date) { mutableListOf() }
      dateTasks.add(task)

      Log.d("TaskSaveDebug", "Task added: ${task.title} on ${task.date}")

      if (currentSelectedDate == task.date) {
         _tasksForSelectedDate.value = dateTasks.toList()
      }
   }
   fun hasTasksForDate(date: String): Boolean {
      return taskMap[date]?.isNotEmpty() == true
   }


   fun getTasksForDate(date: String){
      currentSelectedDate = date
      _selectedDate.value = date
      _tasksForSelectedDate.value = taskMap[date]?.toList() ?: emptyList()
   }
}