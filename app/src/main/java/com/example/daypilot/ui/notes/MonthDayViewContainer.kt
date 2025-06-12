package com.example.daypilot.ui.notes
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.daypilot.R
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.view.ViewContainer

class MonthDayViewContainer(view: View) : ViewContainer(view) {

    val textView: TextView = view.findViewById(R.id.textViewDayNumber)
    val selectedView: View = view.findViewById(R.id.selectedBackground)
    val eventDot: View = view.findViewById(R.id.eventDot)

    lateinit var day: CalendarDay

    // Optional: helper function to bind UI with day state
    fun bind(
        selectedDate: CalendarDay?,
        today: CalendarDay?,
        hasEvent: Boolean,
        onClick: (CalendarDay) -> Unit
    ) {
        day = day

        val date = day.date
        val isSelected = selectedDate?.date == date
        val isToday = today?.date == date

        textView.text = date.dayOfMonth.toString()

        selectedView.visibility = if (isSelected) View.VISIBLE else View.GONE
        eventDot.visibility = if (hasEvent) View.VISIBLE else View.GONE


        when {
            isSelected -> {
                textView.setTextColor(ContextCompat.getColor(view.context, android.R.color.white))
                textView.setTypeface(null, Typeface.BOLD)
            }
            isToday -> {
                textView.setTextColor(ContextCompat.getColor(view.context, R.color.purple_500))
                textView.setTypeface(null, Typeface.BOLD)
            }
            else -> {
                textView.setTextColor(ContextCompat.getColor(view.context, android.R.color.black))
                textView.setTypeface(null, Typeface.NORMAL)
            }
        }

        view.setOnClickListener {
            onClick(day)
        }
    }
}