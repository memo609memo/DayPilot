package com.example.daypilot.ui.notes
import java.io.Serializable
data class Task(  val id: String = "",
                  val title: String = "",
                  val description: String = "",
                  val date: String = "",
                  val startTime: String = "",
                  val endTime: String = "",
    ): Serializable
