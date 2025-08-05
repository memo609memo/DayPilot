package com.example.daypilot.ui.notes

import com.example.daypilot.R

enum class Priority(val displayName: String, val colorResId: Int) {
    DEFAULT("Default", R.color.priority_default),
    IMPORTANT("Important", R.color.priority_important),
    URGENT("Urgent", R.color.priority_urgent)
}