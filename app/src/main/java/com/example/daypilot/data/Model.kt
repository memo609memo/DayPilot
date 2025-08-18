package com.example.daypilot.data

data class TaskRequest(val text: String)
data class TaskResponse(
    val success: Boolean,
    val description: String,
    val date: String?,
    val time: String?,
    val weekday: String?
)
