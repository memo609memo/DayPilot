package com.example.daypilot.ui.notes

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import com.example.daypilot.R

enum class Priority(val displayName: String, val lightColorResId: Int, val darkColorResId: Int) {
    DEFAULT("Default", R.color.priority_default, R.color.dark_priority_default),
    IMPORTANT("Important", R.color.priority_important, R.color.dark_priority_important),
    URGENT("Urgent", R.color.priority_urgent, R.color.dark_priority_urgent);

    fun getColor(context: Context): Int {
        val isDark = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        val resId = if (isDark) darkColorResId else lightColorResId
        return ContextCompat.getColor(context, resId)
    }
}