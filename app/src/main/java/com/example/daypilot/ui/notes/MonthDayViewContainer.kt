package com.example.daypilot.ui.notes
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.daypilot.R
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.view.ViewContainer

class MonthDayViewContainer(view: View) : ViewContainer(view) {

    val dayNumberText: TextView = view.findViewById(R.id.textViewDayNumber)
    val selectedBackground: View = view.findViewById(R.id.selectedBackground)
    val eventDot: View = view.findViewById(R.id.eventDot)

    lateinit var day: CalendarDay

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
                isSelected -> ContextCompat.getColor(view.context, android.R.color.white)
                isToday -> ContextCompat.getColor(view.context, R.color.purple_500)
                else -> ContextCompat.getColor(view.context, android.R.color.black)
            }
        )
        dayNumberText.setTypeface(null, if (isSelected || isToday) Typeface.BOLD else Typeface.NORMAL)

        view.setOnClickListener {
            onClick(day)
        }
    }
}