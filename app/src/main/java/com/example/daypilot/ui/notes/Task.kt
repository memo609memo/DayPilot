package com.example.daypilot.ui.notes
import java.io.Serializable
data class Task(  val id: String = System.currentTimeMillis().toString(),
                  val title: String,
                  val description: String = "",
                  val date: String,
    ): Serializable
