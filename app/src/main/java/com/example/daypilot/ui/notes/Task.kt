package com.example.daypilot.ui.notes
import com.google.firebase.database.PropertyName
import java.io.Serializable
data class Task(  val id: String = "",
                  val title: String = "",
                  val description: String = "",
                  val date: String = "",
                  val startTime: String = "",
                  val endTime: String = "",
                  val repeats: List<Boolean> = List(7) { false },
                  @get:PropertyName("completed") @set:PropertyName("completed")
                  var isCompleted: Boolean = false

    ): Serializable
