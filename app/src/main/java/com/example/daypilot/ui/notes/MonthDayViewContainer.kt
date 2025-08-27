package com.example.daypilot.ui.notes
import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.daypilot.R
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.view.ViewContainer
// ViewContainer class for a single day cell in the Month CalendarView
class MonthDayViewContainer(view: View) : ViewContainer(view) {
    // UI elements inside the day cell
    val dayNumberText: TextView = view.findViewById(R.id.textViewDayNumber)// Shows the day number
    val selectedBackground: View = view.findViewById(R.id.selectedBackground)// Highlight for selected day
    val eventDot: View = view.findViewById(R.id.eventDot)// Dot to indicate a task/event exists

    lateinit var day: CalendarDay
    // Bind the data for this day cell
    @RequiresApi(Build.VERSION_CODES.R)
    fun bind(
        newDay: CalendarDay,// The day to display
        selectedDate: CalendarDay?,// Currently selected date in calendar
        today: CalendarDay?,// Today's date
        hasEvent: Boolean,// Whether the day has tasks/events
        onClick: (CalendarDay) -> Unit// Callback when the day is clicked
    ) {
        day = newDay
        val date = day.date
        // Check if this day is selected
        val isSelected = selectedDate?.date == date
        val isToday = today?.date == date
        // Set the day number text
        dayNumberText.text = date.dayOfMonth.toString()
        // Show event dot if the day has tasks/events
        selectedBackground.visibility = if (isSelected) View.VISIBLE else View.GONE
        eventDot.visibility = if (hasEvent) View.VISIBLE else View.GONE
        // Set text color based on dark mode, selection, and today
        dayNumberText.setTextColor(
            when {

                !view.context.resources.configuration.isNightModeActive && isSelected -> ContextCompat.getColor(view.context, android.R.color.white)
                view.context.resources.configuration.isNightModeActive && isSelected -> ContextCompat.getColor(view.context, android.R.color.black)
                !view.context.resources.configuration.isNightModeActive && isToday -> ContextCompat.getColor(view.context, R.color.today)
                view.context.resources.configuration.isNightModeActive && isToday -> ContextCompat.getColor(view.context, R.color.dark_today)
                else -> {

                    if (!view.context.resources.configuration.isNightModeActive) {
                        ContextCompat.getColor(view.context, R.color.text_color)
                    }
                    else {
                        ContextCompat.getColor(view.context, R.color.dark_text_color)
                    }

                }
            }
        )
        // Make the day number bold if it's selected or today
        dayNumberText.setTypeface(null, if (isSelected || isToday) Typeface.BOLD else Typeface.NORMAL)
        // Set click listener to call the provided onClick callback
        view.setOnClickListener {
            onClick(day)
        }
    }
}

fun Context.isDarkMode(): Boolean {
    val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return currentNightMode == Configuration.UI_MODE_NIGHT_YES
}