package com.example.daypilot.ui.notes

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.daypilot.R
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.view.ViewContainer

class WeekDayViewContainer(view: View) : ViewContainer(view) {
    private val context: Context = view.context

    val dayNumberText: TextView = view.findViewById(R.id.textViewDayNumber)
    val dayOfWeekText: TextView = view.findViewById(R.id.textViewDayOfWeek)
    val selectedBackground: View = view.findViewById(R.id.selectedBackground)
    val eventDot: View = view.findViewById(R.id.eventDot)

    lateinit var day: WeekDay

    fun bind(day: WeekDay, isSelected: Boolean, hasEvent: Boolean, isCurrentMonth: Boolean, isToday: Boolean) {
        this.day = day
        dayNumberText.text = day.date.dayOfMonth.toString()
        dayOfWeekText.text = day.date.dayOfWeek.name.take(3).replaceFirstChar { it.uppercase() }

        // Show selected background circle
        selectedBackground.visibility = if (isSelected) View.VISIBLE else View.GONE

        // Text color: white if selected, black for current month, gray for other months
        dayNumberText.setTextColor(
            if (isSelected) Color.WHITE
            else if (isCurrentMonth) Color.BLACK
            else Color.GRAY
        )

        // Day of week text color lighter for days outside current month
        dayOfWeekText.setTextColor(
            if (isCurrentMonth) ContextCompat.getColor(context, android.R.color.darker_gray)
            else Color.LTGRAY
        )

        // Highlight today (if not selected)
        if (!isSelected && isToday) {
            dayNumberText.setTypeface(null, Typeface.BOLD)
            dayNumberText.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
        } else {
            dayNumberText.setTypeface(null, Typeface.NORMAL)
        }

        // Show or hide event dot
        eventDot.visibility = if (hasEvent) View.VISIBLE else View.GONE
    }
}