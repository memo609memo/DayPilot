package com.example.daypilot.ui.TasksView

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.daypilot.ui.notes.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TasksViewViewModel : ViewModel() {

    private val _allTasks = MutableLiveData<List<Task>>()
    val allTasks: LiveData<List<Task>> get() = _allTasks



    fun loadAllTasks() {

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("users/$uid/Tasks")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = mutableListOf<Task>()
                for (child in snapshot.children) {
                    val task = child.getValue(Task::class.java)
                    if (task?.date.isNullOrEmpty()) {
                        task?.let { tasks.add(it) }
                    }
                }
                _allTasks.value = tasks
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase Error", error.message)
            }
        })
    }

    fun addTask(task: Task) {

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("users/$uid/Tasks")

        ref.orderByChild("id").equalTo(task.id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // If task with same ID already exists, don't add it again
                    if (!snapshot.exists()) {
                        ref.push().setValue(task).addOnSuccessListener {
                            loadAllTasks()
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


    fun deleteTask(task: Task) {

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("users/$uid/Tasks")

        ref.orderByChild("id").equalTo(task.id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        child.ref.removeValue()
                    }
                    loadAllTasks()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "deleteTask failed: ${error.message}")
                }
            })
    }


    fun updateTask(updatedTask: Task) {

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("users/$uid/Tasks")

        ref.orderByChild("id").equalTo(updatedTask.id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        child.ref.setValue(updatedTask)
                    }
                    loadAllTasks()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "updateTask failed: ${error.message}")
                }
            })
    }

}