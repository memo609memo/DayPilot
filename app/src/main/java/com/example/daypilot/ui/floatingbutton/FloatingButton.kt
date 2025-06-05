package com.example.daypilot.ui.floatingbutton

import android.app.Activity
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import com.example.daypilot.R

class FloatingButton(private val activity: Activity) {

    companion object {
        // code for startActivityForResult
        private const val REQ_SPEECH = 1001
    }

    // Get  WindowManager from the activity
    private val windowManager =
        activity.getSystemService(Activity.WINDOW_SERVICE) as WindowManager

    // Inflate  layout for the overlay
    private val floatingView: View =
        View.inflate(activity, R.layout.floating_mic_button, null)

    // Set up LayoutParams for overlays
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 100
        y = 100
    }

    init {
        //
        val micIcon = floatingView.findViewById<ImageView>(R.id.mic_icon)

        // attach  combined drag or click listener
        micIcon.setOnTouchListener(DragOrClickListener(micIcon))

        // clickListener
        micIcon.setOnClickListener {
            Toast.makeText(activity, "Mic button clicked!", Toast.LENGTH_SHORT).show()
            startSpeechRecognition()
        }

        // add the floating view to WindowManager so it appears on screen
        windowManager.addView(floatingView, params)
    }


    fun remove() {
        windowManager.removeView(floatingView)
    }


    // speech recognition method
    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now…")
        }
        activity.startActivityForResult(intent, REQ_SPEECH)
    }

    private inner class DragOrClickListener(val view: View) : View.OnTouchListener {
        private val touchSlop = ViewConfiguration.get(activity).scaledTouchSlop
        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        private var isDragging = false

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Record where the overlay was and where we pressed
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    // Return false so a quick tap still triggers performClick()
                    return false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    if (!isDragging && (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop)) {
                        isDragging = true
                    }

                    if (isDragging) {
                        // Update the overlay position
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(floatingView, params)
                        // Consume the move event so click won’t fire
                        return true
                    }
                    // If not yet a drag, don’t consume it—could still be a tap
                    return false
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // This was a tap (no significant movement)  fire onClick()
                        v.performClick()
                        return true
                    }
                    // If it was a drag, just take the “up” to finish dragging
                    return true
                }
            }
            return false
        }
    }
}
