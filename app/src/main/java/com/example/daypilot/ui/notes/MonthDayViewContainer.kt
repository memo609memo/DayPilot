package com.example.daypilot.ui.notes
import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.daypilot.R
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.view.ViewContainer

class MonthDayViewContainer(view: View) : ViewContainer(view) {

    val dayNumberText: TextView = view.findViewById(R.id.textViewDayNumber)
    val selectedBackground: View = view.findViewById(R.id.selectedBackground)
    val eventDot: View = view.findViewById(R.id.eventDot)

    lateinit var day: CalendarDay

    @RequiresApi(Build.VERSION_CODES.R)
    fun bind(
        newDay: CalendarDay,
        selectedDate: CalendarDay?,
        today: CalendarDay?,
        hasEvent: Boolean,
        onClick: (CalendarDay) -> Unit
    ) {
        day = newDay
        val date = day.date
        val isSelected = selectedDate?.date == date
        val isToday = today?.date == date

        dayNumberText.text = date.dayOfMonth.toString()

        selectedBackground.visibility = if (isSelected) View.VISIBLE else View.GONE
        eventDot.visibility = if (hasEvent) View.VISIBLE else View.GONE

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
        dayNumberText.setTypeface(null, if (isSelected || isToday) Typeface.BOLD else Typeface.NORMAL)

        view.setOnClickListener {
            onClick(day)
        }
    }
}

fun Context.isDarkMode(): Boolean {
    val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return currentNightMode == Configuration.UI_MODE_NIGHT_YES
}