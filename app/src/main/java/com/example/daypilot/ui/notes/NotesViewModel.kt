package com.example.daypilot.ui.notes

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.applandeo.materialcalendarview.EventDay
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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

      //Michael: Adding functionality for the tasks to get loaded from firebase instead
      //_tasksForSelectedDate.value = taskMap[date]?.toList() ?: emptyList()

      val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
      val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/Tasks")

      ref.orderByChild("date").equalTo(date)
         .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
               val taskList = mutableListOf<Task>()
               for(taskSnapshot in snapshot.children){
                  val task = taskSnapshot.getValue(Task::class.java)
                  task?.let { taskList.add(it) }
               }

               _tasksForSelectedDate.value = taskList

            }

            override fun onCancelled(error: DatabaseError) {
               Log.e("Error", error.toString())
            }

         })


   }

   fun deleteTask(task: Task) {
      taskMap[task.date]?.remove(task)
      if (currentSelectedDate == task.date) {
         _tasksForSelectedDate.value = taskMap[task.date]?.toList() ?: emptyList()
      }
   }

   fun updateTask(updatedTask: Task) {
      val tasksForDate = taskMap[updatedTask.date]
      if (tasksForDate != null) {
         val index = tasksForDate.indexOfFirst { it.id == updatedTask.id }
         if (index != -1) {
            tasksForDate[index] = updatedTask
            if (currentSelectedDate == updatedTask.date) {
               _tasksForSelectedDate.value = tasksForDate.toList()
            }
         }
      }
   }


}