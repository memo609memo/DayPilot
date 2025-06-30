package com.example.daypilot.ui.notes

data class HourBlock(
    val hour: Int,
    val tasks: MutableList<Task> = mutableListOf()
)
