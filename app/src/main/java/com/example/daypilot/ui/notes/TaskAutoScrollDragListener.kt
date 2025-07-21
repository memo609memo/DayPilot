package com.example.daypilot.ui.notes

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.DragEvent
import android.view.View

import androidx.recyclerview.widget.RecyclerView

class TaskAutoScrollDragListener(
    private val recyclerView: RecyclerView,
    private val scrollThreshold: Int = 150,
    private val scrollSpeed: Int = 20
) : View.OnDragListener {

    private val handler = Handler(Looper.getMainLooper())
    private var autoScrollDirection = 0
    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            recyclerView.scrollBy(0, autoScrollDirection)
            handler.postDelayed(this, 16)
        }
    }

    private fun startAutoScroll(direction: Int) {
        if (autoScrollDirection == direction) return
        stopAutoScroll()
        autoScrollDirection = direction
        Log.d("TaskAutoScroll", "Start auto scroll: direction=$direction")
        handler.post(autoScrollRunnable)
    }

    private fun stopAutoScroll() {
        if (autoScrollDirection != 0) {
            Log.d("TaskAutoScroll", "Stop auto scroll")
        }
        handler.removeCallbacks(autoScrollRunnable)
        autoScrollDirection = 0
    }

    override fun onDrag(v: View?, event: DragEvent): Boolean {
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                Log.d("TaskAutoScroll", "Drag started")
                return true
            }
            DragEvent.ACTION_DRAG_LOCATION -> {

                val location = IntArray(2)
                recyclerView.getLocationOnScreen(location)
                val recyclerViewY = location[1]
                val dragY = event.y.toInt()

                val relativeY = dragY - recyclerViewY
                val height = recyclerView.height

                Log.d("TaskAutoScroll", "RelativeY=$relativeY, RecyclerView height=$height")

                when {
                    relativeY < scrollThreshold -> startAutoScroll(-scrollSpeed)
                    relativeY > height - scrollThreshold -> startAutoScroll(scrollSpeed)
                    else -> stopAutoScroll()
                }

                return true
            }

            DragEvent.ACTION_DRAG_ENDED,
            DragEvent.ACTION_DROP -> {
                stopAutoScroll()
                return true
            }

            else -> return false
        }
    }
}