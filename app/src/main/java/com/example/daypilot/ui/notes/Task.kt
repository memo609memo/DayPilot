package com.example.daypilot.ui.notes
import java.io.Serializable
data class Task(
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val startTime: String = "",
    val completed: Boolean = false,
    val id: String = ""
    ): Serializable
