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
import com.example.daypilot.MainActivity
import com.example.daypilot.R

// Do in need the code to req permission
// do mic in xml ?

class FloatingButton private constructor(private val activity: Activity) {

    companion object {
        @Volatile
        private var current: FloatingButton? = null  // Made this to ensure floatingbutton is not recreated

        @Synchronized
        fun show(activity: Activity) {
            val existing = current
            if (existing != null) {
                return
            }
            current = FloatingButton(activity)
        }

        @Synchronized
        fun hide() {
            current?.remove()
            current = null
        }
    }

    private val windowManager =
        activity.getSystemService(Activity.WINDOW_SERVICE) as WindowManager

    private val floatingView: View =
        View.inflate(activity, R.layout.floating_mic_button, null)

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
        val micIcon = floatingView.findViewById<ImageView>(R.id.mic_icon)
        micIcon.setOnTouchListener(DragOrClickListener(micIcon))
        micIcon.setOnClickListener {
            Toast.makeText(activity, "Mic button clicked!", Toast.LENGTH_SHORT).show()
            startSpeechRecognition()
        }
        windowManager.addView(floatingView, params)
    }

    fun remove() {
        try {
            windowManager.removeView(floatingView)
        } catch (_: Exception) {
        }
    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now…")
        }
        activity.startActivityForResult(intent, MainActivity.REQ_SPEECH)
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
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    return false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    if (!isDragging && (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop)) {
                        isDragging = true
                    }

                    if (isDragging) {
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                    return false
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        v.performClick()
                        return true
                    }
                    return true
                }
            }
            return false
        }
    }
}
