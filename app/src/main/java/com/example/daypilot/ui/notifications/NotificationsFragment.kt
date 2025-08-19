package com.example.daypilot.ui.notifications

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.daypilot.R
import com.example.daypilot.databinding.FragmentNotificationsBinding
import com.example.daypilot.ui.notes.NotesViewModel
import com.example.daypilot.ui.notes.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import java.time.LocalDate
import java.util.Calendar
import java.util.UUID

class NotificationsFragment : Fragment() {

    companion object { private const val TAG = "NotificationsFrag" }

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private var currentTaskKey: String? = null
    private val repeatToggles = MutableList(7) { false }

    private var selectedStartHour = -1
    private var selectedStartMinute = -1
    private var selectedEndHour = -1
    private var selectedEndMinute = -1

    private var descriptionIncoming: String? = null
    private var dateIncoming: String? = null
    private var timeIncoming: String? = null
    private var weekdayIncoming: String? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { imageUri ->
            getTitle { title ->
                if (title != null) uploadImgToDB(imageUri, title)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root = binding.root

        val taskId = arguments?.getString("taskId")
        arguments?.let { args ->
            descriptionIncoming = args.getString("description")
            dateIncoming = args.getString("date")
            timeIncoming = args.getString("time")
            weekdayIncoming = args.getString("weekday")
            Log.d(TAG, "Args desc=$descriptionIncoming date=$dateIncoming time=$timeIncoming weekday=$weekdayIncoming")
        }

        descriptionIncoming?.let {
            binding.editTaskName.setText(it)
            binding.noteBody.setText(it)
        }
        dateIncoming?.let { binding.taskDate.text = it }
        timeIncoming?.let { binding.startTimeTextView.text = it }
        weekdayIncoming?.let { wd -> mapWeekdayToIndex(wd)?.let { idx -> repeatToggles[idx] = true } }

        setupRepeatButtons()
        setupTimePicker()
        taskId?.let { loadTask(it) }

        binding.taskDate.setOnClickListener { showDatePickerDialog() }
        binding.repeatTextView.setOnClickListener { showRepeatSelectionDialog() }
        binding.imageButton.setOnClickListener { imagePicker.launch("image/*") }
        binding.micButton.setOnClickListener { /* your voice UI if needed */ }

        binding.saveButton.setOnClickListener {
            val title = binding.editTaskName.text.toString().trim()
            val description = binding.noteBody.text.toString().trim()
            val date = binding.taskDate.text.toString().trim()
            val startTime = binding.startTimeTextView.text.toString().trim()
            val endTime = binding.endTimeTextView.text.toString().trim()

            if (title.isEmpty()) {
                binding.editTaskName.error = "Please enter title"
                return@setOnClickListener
            }

            if (startTime.isNotEmpty() && endTime.isNotEmpty()
                && selectedStartHour >= 0 && selectedEndHour >= 0
            ) {
                val startMin = selectedStartHour * 60 + selectedStartMinute
                val endMin = selectedEndHour * 60 + selectedEndMinute
                if (endMin <= startMin) {
                    Toast.makeText(requireContext(), "End time must be later than start time", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            if (taskId == null) {
                createTask(title, description, date, startTime, endTime)
            } else {
                updateTask(taskId, title, description, date, startTime, endTime)
            }

            updateRepeatingTasks()
        }

        binding.cancelButton.setOnClickListener { returnToNotes() }

        return root
    }

    private fun showRepeatSelectionDialog() {
        val weekDays = listOf("Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday")
        val selectedItems = repeatToggles.toBooleanArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Repeat On")
            .setMultiChoiceItems(weekDays.toTypedArray(), selectedItems) { _, index, isChecked ->
                repeatToggles[index] = isChecked
            }
            .setPositiveButton("Ok") { _, _ -> setupRepeatButtons() }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun setupRepeatButtons() {
        val buttons = listOf(
            binding.repeatSunday, binding.repeatMonday, binding.repeatTuesday,
            binding.repeatWednesday, binding.repeatThursday, binding.repeatFriday, binding.repeatSaturday
        )
        buttons.forEachIndexed { index, button ->
            button.isSelected = repeatToggles[index]
            button.setOnClickListener {
                repeatToggles[index] = !repeatToggles[index]
                button.isSelected = repeatToggles[index]
            }
        }
    }

    private fun setupTimePicker() {
        binding.startTimeTextView.setOnClickListener {
            showTimePickerDialog(selectedStartHour, selectedStartMinute, binding.startTimeTextView)
        }
        binding.endTimeTextView.setOnClickListener {
            showTimePickerDialog(selectedEndHour, selectedEndMinute, binding.endTimeTextView)
        }
    }

    private fun showTimePickerDialog(initialHour: Int, initialMinute: Int, textView: TextView) {
        val cal = Calendar.getInstance()
        val hourToDisplay = if (initialHour != -1) initialHour else cal.get(Calendar.HOUR_OF_DAY)
        val minuteToDisplay = if (initialMinute != -1) initialMinute else cal.get(Calendar.MINUTE)

        val dlg = TimePickerDialog(
            requireContext(),
            { _, h, m ->
                when (textView) {
                    binding.startTimeTextView -> {
                        selectedStartHour = h
                        selectedStartMinute = m
                        if (binding.endTimeTextView.text.isBlank()) {
                            selectedEndHour = h + 1
                            selectedEndMinute = m
                            updateTimeTextView(binding.endTimeTextView, selectedEndHour, selectedEndMinute)
                        }
                    }
                    binding.endTimeTextView -> {
                        selectedEndHour = h
                        selectedEndMinute = m
                    }
                }
                updateTimeTextView(textView, h, m)
            },
            hourToDisplay, minuteToDisplay, false
        )
        dlg.show()
    }

    private fun updateTimeTextView(textView: TextView, h: Int, m: Int) {
        if (h != -1 && m != -1) textView.text = formatTime(h, m)
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = if (hour == 0 || hour == 12) 12 else hour % 12
        return String.format("%02d:%02d %s", displayHour, minute, amPm)
    }

    private fun showDatePickerDialog() {
        val c = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val selectedDate = LocalDate.of(y, m + 1, d)
                binding.taskDate.text = selectedDate.toString()
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun createTask(title: String, description: String, date: String, start: String, end: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/tasks")
        val id = ref.push().key ?: UUID.randomUUID().toString()

        val task = Task(
            id = id,
            title = title,
            description = description,
            date = date,
            startTime = start,
            endTime = end,
            isCompleted = false
        )

        ref.child(id).setValue(task)
            .addOnSuccessListener {
                ref.child(id).child("repeats").setValue(repeatToggles)
                currentTaskKey = id
                ViewModelProvider(requireActivity())[NotesViewModel::class.java].addTask(task)
                Toast.makeText(requireContext(), "Task saved", Toast.LENGTH_SHORT).show()
                returnToNotes()
            }
            .addOnFailureListener {
                Log.e(TAG, "createTask failed", it)
                Toast.makeText(requireContext(), "Failed to save task", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTask(taskId: String, title: String, description: String, date: String, start: String, end: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/tasks")

        ref.orderByChild("id").equalTo(taskId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val updates = mapOf(
                        "title" to title,
                        "description" to description,
                        "date" to date,
                        "startTime" to start,
                        "endTime" to end,
                        "repeats" to repeatToggles
                    )
                    snapshot.children.firstOrNull()?.let { node ->
                        currentTaskKey = node.key
                        node.ref.updateChildren(updates)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Task updated", Toast.LENGTH_SHORT).show()
                                returnToNotes()
                            }
                            .addOnFailureListener {
                                Log.e(TAG, "updateTask failed", it)
                                Toast.makeText(requireContext(), "Failed to update task", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "updateTask:onCancelled", error.toException())
                }
            })
    }

    private fun loadTask(taskId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/tasks")

        ref.orderByChild("id").equalTo(taskId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val node = snapshot.children.firstOrNull() ?: return
                    currentTaskKey = node.key
                    node.getValue(Task::class.java)?.let { t ->
                        binding.editTaskName.setText(t.title)
                        binding.noteBody.setText(t.description)
                        binding.taskDate.text = t.date
                        binding.startTimeTextView.text = t.startTime
                        binding.endTimeTextView.text = t.endTime
                    }
                    node.child("repeats").let { rep ->
                        if (rep.exists()) {
                            for (i in 0..6) {
                                repeatToggles[i] = rep.child(i.toString()).getValue(Boolean::class.java) == true
                            }
                            setupRepeatButtons()
                        }
                    }
                    currentTaskKey?.let { loadMediaCards(it) }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "loadTask:onCancelled", error.toException())
                }
            })
    }

    private fun addMediaCard(type: String, title: String, imageUriOrUrl: Any) {
        val container = binding.cardContainer
        val layoutId = when (type) {
            "image" -> R.layout.image_card
            "voice" -> R.layout.voice_card
            else -> R.layout.image_card
        }
        val cardView = layoutInflater.inflate(layoutId, container, false)
        val label = cardView.findViewById<TextView>(R.id.media_Name)
        label.text = title

        val uri = when (imageUriOrUrl) {
            is Uri -> imageUriOrUrl
            is String -> Uri.parse(imageUriOrUrl)
            else -> return
        }

        cardView.setOnClickListener { showImagePopup(uri, container, cardView) }
        container.addView(cardView)
    }

    private fun showImagePopup(imageUri: Uri, container: LinearLayout, view: View) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_popup, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.popupImage)
        val closeButton = dialogView.findViewById<Button>(R.id.closePopup)
        val deleteButton = dialogView.findViewById<Button>(R.id.deleteButton)

        Glide.with(this).load(imageUri).into(imageView)

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        closeButton.setOnClickListener { dialog.dismiss() }
        deleteButton.setOnClickListener {
            container.removeView(view)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun getTitle(callback: (String?) -> Unit) {
        val input = EditText(requireContext()).apply {
            hint = "Title"
            maxLines = 1
            filters = arrayOf<InputFilter>(InputFilter.LengthFilter(5))
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Title your Image")
            .setView(input)
            .setPositiveButton("Ok") { _, _ ->
                val text = input.text.toString().trim()
                if (text.length in 1..5) callback(text) else {
                    Toast.makeText(context, "Please enter a valid text up to 5 characters", Toast.LENGTH_LONG).show()
                    callback(null)
                }
            }
            .setNegativeButton("Cancel") { _, _ -> callback(null) }
            .create()
            .show()
    }

    private fun uploadImgToDB(imageUri: Uri, title: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val storageRef = FirebaseStorage.getInstance().reference
        val taskKey = currentTaskKey ?: return
        val imageRef = storageRef.child("/users/$uid/tasks/$taskKey/${System.currentTimeMillis()}.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveImgUrlToDB(taskKey, title, downloadUri.toString())
                    addMediaCard("image", title, downloadUri)
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to upload image", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveImgUrlToDB(taskId: String, title: String, imageUrl: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/tasks/$taskId/media")
        val mediaId = ref.push().key ?: return
        val mediaData = mapOf(
            "id" to mediaId,
            "type" to "image",
            "title" to title,
            "url" to imageUrl
        )
        ref.child(mediaId).setValue(mediaData)
    }

    private fun loadMediaCards(taskId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/tasks/$taskId/media")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (mediaSnapshot in snapshot.children) {
                    val type = mediaSnapshot.child("type").getValue(String::class.java) ?: continue
                    val title = mediaSnapshot.child("title").getValue(String::class.java) ?: continue
                    val url = mediaSnapshot.child("url").getValue(String::class.java) ?: continue
                    addMediaCard(type, title, url)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, error.message)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun mapWeekdayToIndex(name: String): Int? {
        return when (name.trim().lowercase()) {
            "sunday","sun" -> 0
            "monday","mon" -> 1
            "tuesday","tue" -> 2
            "wednesday","wed" -> 3
            "thursday","thu","thur","thurs" -> 4
            "friday","fri" -> 5
            "saturday","sat" -> 6
            else -> null
        }
    }

    private fun returnToNotes() {
        val nav = findNavController()
        nav.previousBackStackEntry?.savedStateHandle?.set("refreshNeeded", true)
        if (!nav.popBackStack()) {
            val start = nav.graph.startDestinationId
            if (!nav.popBackStack(start, false)) {
                nav.navigate(start)
            }
        }
    }
}

fun updateRepeatingTasks() {
    val functions = FirebaseFunctions.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val data = hashMapOf("uid" to uid)
    functions.getHttpsCallable("setRepeatingTasksOnCall").call(data)
}
