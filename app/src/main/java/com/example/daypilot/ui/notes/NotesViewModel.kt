package com.example.daypilot.ui.notes

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NotesViewModel : ViewModel() {

    private val _selectedDate = MutableLiveData<String>()
    val selectedDate: LiveData<String> = _selectedDate

    private val _tasksForSelectedDate = MutableLiveData<List<Task>>()
    val tasksForSelectedDate: LiveData<List<Task>> get() = _tasksForSelectedDate

    private val taskMap = mutableMapOf<String, MutableList<Task>>()
    private var currentSelectedDate: String = ""

    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    private val ref: DatabaseReference = FirebaseDatabase.getInstance()
        .getReference("/users/$uid/tasks")

    fun selectedDate(dateString: String) {
        _selectedDate.value = dateString
    }

    fun hasTasksForDate(date: String): Boolean = taskMap[date]?.isNotEmpty() == true

    fun addTask(task: Task) {
        // writes on firebase then updates the task
        ref.orderByChild("id").equalTo(task.id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        ref.push().setValue(task).addOnSuccessListener {
                            val list = taskMap.getOrPut(task.date) { mutableListOf() }
                            list.add(task)
                            if (currentSelectedDate == task.date) {
                                _tasksForSelectedDate.value = list.toList()
                            }
                        }.addOnFailureListener {
                            Log.e("Firebase", "Failed to add task: ${it.message}")
                        }
                    } else {
                        Log.d("Firebase", "Task id ${task.id} already exists. Skipping.")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "addTask check failed: ${error.message}")
                }
            })
    }

    fun getTasksForDate(date: String) {
        currentSelectedDate = date
        _selectedDate.value = date

        ref.orderByChild("date").equalTo(date)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val byId = mutableMapOf<String, Task>()
                    for (child in snapshot.children) {
                        child.getValue(Task::class.java)?.let { byId[it.id] = it }
                    }
                    val list = byId.values.toList()
                    if (list.isEmpty()) taskMap.remove(date) else taskMap[date] = list.toMutableList()
                    _tasksForSelectedDate.value = list
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "getTasksForDate failed: ${error.message}")
                }
            })
    }

    fun deleteTask(task: Task) {
        ref.orderByChild("id").equalTo(task.id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) child.ref.removeValue()
                    taskMap[task.date]?.removeAll { it.id == task.id }
                    if (currentSelectedDate == task.date) {
                        _tasksForSelectedDate.value = taskMap[task.date]?.toList().orEmpty()
                    }
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
                    for (child in snapshot.children) child.ref.setValue(updatedTask)
                    taskMap[updatedTask.date]?.let { list ->
                        val idx = list.indexOfFirst { it.id == updatedTask.id }
                        if (idx != -1) list[idx] = updatedTask
                    }
                    if (currentSelectedDate == updatedTask.date) {
                        _tasksForSelectedDate.value = taskMap[updatedTask.date]?.toList().orEmpty()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "updateTask failed: ${error.message}")
                }
            })
    }

    fun markTaskAsCompleted(task: Task) {
        val done = task.copy(isCompleted = true)
        updateTask(done)
    }

    fun preloadAllTasks(onLoaded: () -> Unit) {
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                taskMap.clear()
                for (child in snapshot.children) {
                    child.getValue(Task::class.java)?.let { t ->
                        taskMap.getOrPut(t.date) { mutableListOf() }.add(t)
                    }
                }
                if (currentSelectedDate.isNotBlank()) {
                    _tasksForSelectedDate.value = taskMap[currentSelectedDate]?.toList().orEmpty()
                }
                onLoaded()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "preloadAllTasks failed: ${error.message}")
                onLoaded()
            }
        })
    }

    fun buildHourBlocksFromTasks(tasks: List<Task>): List<HourBlock> {
        val fmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val blocks = mutableMapOf<Int, MutableList<Task>>()

        for (t in tasks) {
            val start = t.startTime
            val end = t.endTime
            if (!start.isNullOrBlank() && !end.isNullOrBlank()) {
                try {
                    val cal = Calendar.getInstance().apply { time = fmt.parse(start)!! }
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    blocks.getOrPut(hour) { mutableListOf() }.add(t)
                } catch (_: Exception) {
                    blocks.getOrPut(-1) { mutableListOf() }.add(t)
                }
            } else {
                blocks.getOrPut(-1) { mutableListOf() }.add(t)
            }
        }

        return (0..23).map { h -> HourBlock(h, blocks[h] ?: mutableListOf()) }
            .toMutableList().apply {
                if (blocks.containsKey(-1)) add(0, HourBlock(-1, blocks[-1]!!))
            }
    }

    fun rescheduleTask(task: Task, fromHour: Int, toHour: Int) {
        val fmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
        try {
            val cal = Calendar.getInstance()
            val originalStart = task.startTime?.takeIf { it.isNotBlank() }?.let { fmt.parse(it) }
            val originalEnd = task.endTime?.takeIf { it.isNotBlank() }?.let { fmt.parse(it) }

            val start: Date
            val end: Date

            if (originalStart != null && originalEnd != null) {
                val duration = originalEnd.time - originalStart.time
                cal.time = originalStart
                val minutes = cal.get(Calendar.MINUTE)

                cal.set(Calendar.HOUR_OF_DAY, toHour)
                cal.set(Calendar.MINUTE, minutes)
                start = cal.time
                end = Date(start.time + duration)
            } else {
                cal.set(Calendar.HOUR_OF_DAY, toHour)
                cal.set(Calendar.MINUTE, 0)
                start = cal.time
                cal.set(Calendar.HOUR_OF_DAY, toHour + 1)
                end = cal.time
            }

            val updated = task.copy(
                startTime = fmt.format(start),
                endTime = fmt.format(end)
            )
            updateTask(updated)
        } catch (e: Exception) {
            Log.e("Reschedule", "Error updating task time", e)
        }
    }

    // helper function for floating mic

    fun addTaskFromSpeech(
        title: String?,
        date: String?,
        startTime: String?,
        endTime: String?,
        description: String? = "",
        priority: Priority? = null
    ) {
        val safeTitle = (title ?: "").trim()
        if (safeTitle.isBlank()) {
            Log.w("SpeechAdd", "Skipped adding task because title is blank")
            return
        }

        val targetDate = (date ?: currentSelectedDate).ifBlank {
            // fallback to today in yyyy-MM-dd
            val cal = Calendar.getInstance()
            String.format(
                Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        val fmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
        var sTime = startTime?.trim().orEmpty()
        var eTime = endTime?.trim().orEmpty()

        if (sTime.isNotBlank() && eTime.isBlank()) {
            try {
                val cal = Calendar.getInstance().apply { time = fmt.parse(sTime)!! }
                cal.add(Calendar.HOUR_OF_DAY, 1)
                eTime = fmt.format(cal.time)
            } catch (_: Exception) { /* ignore and keep blank */ }
        }

        val task = Task(
            id = System.currentTimeMillis().toString(),
            title = safeTitle,
            description = description.orEmpty(),
            date = targetDate,
            startTime = sTime,
            endTime = eTime,
            isCompleted = false,
            priority = priority ?: Priority.DEFAULT
        )
        addTask(task)
        if (currentSelectedDate == targetDate) {
            getTasksForDate(targetDate)
        }
    }

    fun applyNlpResult(fields: Map<String, String?>) {
        val prio = fields["priority"]?.trim()?.uppercase(Locale.getDefault())
        val p = runCatching { Priority.valueOf(prio ?: "") }.getOrNull()
        addTaskFromSpeech(
            title = fields["title"],
            date = fields["date"],
            startTime = fields["start"] ?: fields["startTime"],
            endTime = fields["end"] ?: fields["endTime"],
            description = fields["desc"] ?: fields["description"],
            priority = p
        )
    }
}
