package com.example.daypilot.ui.notes

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NotesViewModel : ViewModel() {
   private val _selectedDate = MutableLiveData<String>()
   val selectedDate: LiveData<String> = _selectedDate

   private val taskMap = mutableMapOf<String, MutableList<Task>>()
   private val _tasksForSelectedDate = MutableLiveData<List<Task>>()

   private var currentSelectedDate: String = ""

   val tasksForSelectedDate: LiveData<List<Task>> get() = _tasksForSelectedDate

   fun selectedDate(dateString: String) {
      _selectedDate.value = dateString
   }

   // need to make edit to fit recycler view so list shows
   fun addTask(task: Task) {
      val normalizedDateForList = task.date.take(10)
      val dateTasks = taskMap.getOrPut(normalizedDateForList) { mutableListOf() }
      dateTasks.add(task)
      if (currentSelectedDate == normalizedDateForList) {
         _tasksForSelectedDate.value = dateTasks.toList()
      }
   }


   fun hasTasksForDate(date: String): Boolean {
      return taskMap[date]?.isNotEmpty() == true
   }

   fun getTasksForDate(date: String) {
      currentSelectedDate = date
      _selectedDate.value = date
      _tasksForSelectedDate.value = taskMap[date]?.toList() ?: emptyList()
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

   //load task if on firebase
   fun loadAllTasksOnceFromFirebase(onComplete: (() -> Unit)? = null) {    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
      val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/Tasks")

      ref.addListenerForSingleValueEvent(object : ValueEventListener {
         override fun onDataChange(snapshot: DataSnapshot) {
            Log.d("DebugCheck", "Firebase data snapshot has ${snapshot.childrenCount} tasks")
            for (taskSnapshot in snapshot.children) {
               val task = taskSnapshot.getValue(Task::class.java)
               task?.let {
                  addTask(it)
                  Log.d("DebugCheck", "Loaded task: ${it.title} on ${it.date}")
               }
            }
            onComplete?.invoke()
         }

         override fun onCancelled(error: DatabaseError) {
            Log.e("DebugCheck", "Error loading tasks: ${error.message}")
            onComplete?.invoke()
         }
      })
   }
}
