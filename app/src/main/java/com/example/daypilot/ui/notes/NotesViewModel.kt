package com.example.daypilot.ui.notes

import android.icu.util.Calendar
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


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

      val list = taskMap.getOrPut(task.date) { mutableListOf() }
      list.add(task)


      if (currentSelectedDate == task.date) {
         _tasksForSelectedDate.value = list.toList()
      }
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
                  task?.let {Log.d("TaskCheck", "Task: ${it.title}, Completed: ${it.isCompleted}")
                     taskMapById[it.id] = it }
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
   fun buildHourBlocksFromTasks(tasks: List<Task>): List<HourBlock> {
      val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())

      val blocks = mutableMapOf<Int, MutableList<Task>>()

      for (task in tasks) {
         if (task.startTime.isNotBlank() && task.endTime.isNotBlank()) {
            try {
               val start = sdf.parse(task.startTime)
               val startHour = start?.hours ?: continue
               blocks.getOrPut(startHour) { mutableListOf() }.add(task)
            } catch (e: Exception) {
               blocks.getOrPut(-1) { mutableListOf() }.add(task)
            }
         } else {
            blocks.getOrPut(-1) { mutableListOf() }.add(task)
         }
      }

      return (0..23).map { hour ->
         HourBlock(hour, blocks[hour] ?: mutableListOf())
      }.toMutableList().apply {
         if (blocks.containsKey(-1)) {
            add(0, HourBlock(-1, blocks[-1]!!))
         }
      }
   }

   fun markTaskAsCompleted(task: Task){
      val query = ref.orderByChild("id").equalTo(task.id)

      query.addListenerForSingleValueEvent(object : ValueEventListener {
         override fun onDataChange(snapshot: DataSnapshot) {
            for (childSnapshot in snapshot.children) {
               val updatedTask = task.copy(isCompleted = true)
               childSnapshot.ref.setValue(updatedTask)
               getTasksForDate(task.date)
            }
         }

         override fun onCancelled(error: DatabaseError) {
            Log.e("Firebase", "Failed to mark task as completed", error.toException())
         }
      })


   }

   fun rescheduleTask(task: Task, fromHour: Int, toHour: Int) {
      val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
      try {
         val originalStart = task.startTime?.takeIf { it.isNotBlank() }?.let { formatter.parse(it) }
         val originalEnd = task.endTime?.takeIf { it.isNotBlank() }?.let { formatter.parse(it) }

         val calendar = Calendar.getInstance()
         val newStart: Date
         val newEnd: Date

         if (originalStart != null && originalEnd != null) {

            val durationMillis = originalEnd.time - originalStart.time

            calendar.time = originalStart
            val originalMinutes = calendar.get(Calendar.MINUTE)

            calendar.set(Calendar.HOUR_OF_DAY, toHour)
            calendar.set(Calendar.MINUTE, originalMinutes)
            newStart = calendar.time
            newEnd = Date(newStart.time + durationMillis)

         } else {
            calendar.set(Calendar.HOUR_OF_DAY, toHour)
            calendar.set(Calendar.MINUTE, 0)
            newStart = calendar.time

            calendar.set(Calendar.HOUR_OF_DAY, toHour + 1)
            newEnd = calendar.time
         }

         val newStartTime = formatter.format(newStart)
         val newEndTime = formatter.format(newEnd)

         val updatedTask = task.copy(startTime = newStartTime, endTime = newEndTime)
         updateTask(updatedTask)

         // Update in Firebase
         val query = ref.orderByChild("id").equalTo(task.id)
         query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
               for (child in snapshot.children) {
                  child.ref.setValue(updatedTask)
               }
            }

            override fun onCancelled(error: DatabaseError) {
               Log.e("Firebase", "Update failed: ${error.message}")
            }
         })

         getTasksForDate(task.date)

      } catch (e: Exception) {
         Log.e("Reschedule", "Error updating task time", e)
      }
   }
   }



