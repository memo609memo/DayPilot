package com.example.daypilot.ui.notes

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class NotesViewModel : ViewModel() {
   private  val  _selectedDate = MutableLiveData<String>()
   val selectedDate: LiveData<String> = _selectedDate
   private val taskMap = mutableMapOf<String, MutableList<Task>>()


   private val _tasksForSelectedDate = MutableLiveData<List<Task>>()

   private var currentSelectedDate: String = ""

   val tasksForSelectedDate: LiveData<List<Task>>get()=_tasksForSelectedDate

   private val uid = FirebaseAuth.getInstance().currentUser?.uid
   private val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/tasks")
   fun selectedDate(dateString: String)
   {
      _selectedDate.value = dateString
   }




   fun addTask(task: Task) {
      ref.orderByChild("id").equalTo(task.id)
         .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
               // If task with same ID already exists, don't add it again
               if (!snapshot.exists()) {
                  ref.push().setValue(task).addOnSuccessListener {
                     if (currentSelectedDate == task.date) {
                        getTasksForDate(task.date)
                     }
                  }.addOnFailureListener {
                     Log.e("Firebase", "Failed to add task: ${it.message}")
                  }
               } else {
                  Log.d("Firebase", "Task with id ${task.id} already exists. Skipping add.")
               }
            }

            override fun onCancelled(error: DatabaseError) {
               Log.e("Firebase", "addTask check failed: ${error.message}")
            }
         })
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
      val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/tasks")

      ref.orderByChild("date").equalTo(date)
         .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
               val taskMapById = mutableMapOf<String, Task>()
               for (taskSnapshot in snapshot.children) {
                  val task = taskSnapshot.getValue(Task::class.java)
                  task?.let { taskMapById[it.id] = it }
               }
               val taskList = taskMapById.values.toList()
               if (taskList.isEmpty()) {
                  taskMap.remove(date)
               } else {
                  taskMap[date] = taskList.toMutableList()
               }

               _tasksForSelectedDate.value = taskList

            }

            override fun onCancelled(error: DatabaseError) {
               Log.e("Error", error.toString())
            }

         })


   }

   fun deleteTask(task: Task) {
      ref.orderByChild("id").equalTo(task.id)
         .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
               for (child in snapshot.children) {
                  child.ref.removeValue()
               }
               getTasksForDate(task.date)
            }

            override fun onCancelled(error: DatabaseError) {
               Log.e("Firebase", "deleteTask failed: ${error.message}")
            }
         })
   }



   fun updateTask(updatedTask: Task) {
      ref.orderByChild("id").equalTo(updatedTask.id)
         .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
               for (child in snapshot.children) {
                  child.ref.setValue(updatedTask)
               }
               getTasksForDate(updatedTask.date)
            }

            override fun onCancelled(error: DatabaseError) {
               Log.e("Firebase", "updateTask failed: ${error.message}")
            }
         })
   }

   fun preloadAllTasks(onLoaded: () -> Unit) {
      val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
      val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/tasks")

      ref.addListenerForSingleValueEvent(object : ValueEventListener {
         override fun onDataChange(snapshot: DataSnapshot) {
            taskMap.clear()
            for (taskSnapshot in snapshot.children) {
               val task = taskSnapshot.getValue(Task::class.java)
               task?.let {
                  val list = taskMap.getOrPut(it.date) { mutableListOf() }
                  list.add(it)
               }
            }
            onLoaded()
         }

         override fun onCancelled(error: DatabaseError) {
            Log.e("Firebase", "Failed to preload tasks: ${error.message}")
         }
      })
   }



}