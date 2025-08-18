package com.example.daypilot.ui.notifications

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.daypilot.R
import androidx.navigation.fragment.findNavController
import com.example.daypilot.databinding.FragmentNotificationsBinding
import com.example.daypilot.ui.notes.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.util.Calendar
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide
import com.google.firebase.functions.FirebaseFunctions

class NotificationsFragment : Fragment() {

    private var currentTaskKey: String? = null

    private val repeatToggles = MutableList(7) { false }
    private var selectedStartHour: Int = -1
    private var selectedStartMinute: Int = -1
    private var selectedEndHour: Int = -1
    private var selectedEndMinute: Int = -1

    private var _binding: FragmentNotificationsBinding? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { imageUri ->
            getTitle { title ->
                if (title != null) {
                    uploadImgToDB(imageUri, title)
                }
            }
        }
    }

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel = ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val taskId = arguments?.getString("taskId")

        setupRepeatButtons()
        setupTimePicker()
        taskId?.let { getTaskDataFromFirebase(it)}

        //save button section
        binding.saveButton.setOnClickListener {
            val newTitle = binding.editTaskName.text.toString()
            val updatedDescription = binding.noteBody.text.toString()
            val newDate = binding.taskDate.text.toString()

            if (newTitle.isEmpty()) {
                binding.editTaskName.error = "Please enter title"
                return@setOnClickListener
            }

            updateRepeatingTasks()

            val startTimeTextBox = binding.startTimeTextView.text.toString()
            val endTimeTextBox = binding.endTimeTextView.text.toString()

            if(startTimeTextBox.isNotBlank() && endTimeTextBox.isNotBlank()) {

                val startTimeMinMode = selectedStartHour * 60 + selectedStartMinute
                val endTimeMinMode = selectedEndHour * 60 + selectedEndMinute

                if (endTimeMinMode <= startTimeMinMode) {
                    binding.endTimeTextView.error = "End time must be later than start time"
                    return@setOnClickListener
                }
            }


            if (taskId != null) {
                updateTaskInFirebase(taskId, newTitle, updatedDescription,newDate)
            }

            findNavController().previousBackStackEntry
                ?.savedStateHandle
                ?.set("refreshNeeded", true)

            findNavController().popBackStack()
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.taskDate.setOnClickListener {
            showDatePickerDialog()
        }

        binding.repeatTextView.setOnClickListener {
            showRepeatSelectionDialog()
        }

        binding.imageButton.setOnClickListener {
            imagePicker.launch("image/*")
        }
        binding.micButton.setOnClickListener {
            //addMediaCard("voice", "test")
        }

        return root
    }

    private fun showRepeatSelectionDialog() {
        val weekDays = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val selectedItems = repeatToggles.toBooleanArray()

        val builder = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Repeat On")
            .setMultiChoiceItems(weekDays.toTypedArray(), selectedItems){ _, index, isChecked ->
                repeatToggles[index] = isChecked
            }
                .setPositiveButton("Ok") { _, _ ->
                    setupRepeatButtons()
                }
                .setNegativeButton("Cancel", null)

        builder.create().show()
    }

    private fun setupRepeatButtons() {
        val buttons = listOf(
            binding.repeatSunday,
            binding.repeatMonday,
            binding.repeatTuesday,
            binding.repeatWednesday,
            binding.repeatThursday,
            binding.repeatFriday,
            binding.repeatSaturday
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

    private fun showTimePickerDialog(initialHour: Int, initialMinute: Int, textView: TextView){
        val calendar = Calendar.getInstance()
        val hourToDisplay = if (initialHour != -1) initialHour else calendar.get(Calendar.HOUR_OF_DAY);
        val minuteToDisplay = if (initialMinute != -1) initialMinute else calendar.get(Calendar.MINUTE);



        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour: Int, selectedMinute: Int ->
                when(textView) {
                    binding.startTimeTextView -> {
                        selectedStartHour = selectedHour
                        selectedStartMinute = selectedMinute
                    }
                    binding.endTimeTextView -> {
                        selectedEndHour = selectedHour
                        selectedEndMinute = selectedMinute
                    }
                }
                updateTimeTextView(textView, selectedHour, selectedMinute)
            },
            hourToDisplay,
            minuteToDisplay,
            false
        )
        timePickerDialog.show()
    }

    private fun updateTimeTextView(textView: TextView, selectedHour: Int, selectedMinute: Int) {
        if (selectedHour != -1 && selectedMinute != -1) {
            val formattedTime = formatTime(selectedHour, selectedMinute)
            textView.text = formattedTime
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM";
        val displayHour = if(hour == 0 || hour == 12) 12 else hour % 12
        return String.format("%02d:%02d %s", displayHour, minute, amPm)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getTaskDataFromFirebase(taskId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/tasks")

        ref.orderByChild("id").equalTo(taskId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for(taskSnapshot in snapshot.children) {
                        val task = taskSnapshot.getValue(Task::class.java)
                        if (task != null) {
                            currentTaskKey = taskSnapshot.key

                            binding.editTaskName.setText(task.title)
                            binding.noteBody.setText(task.description)
                            binding.taskDate.text = task.date
                            binding.startTimeTextView.text = task.startTime
                            binding.endTimeTextView.text = task.endTime
                            val repeatList = task.repeats
                            repeatList.forEachIndexed{ index, value ->
                                repeatToggles[index] = value
                            }
                            setupRepeatButtons()
                            loadMediaCards(currentTaskKey!!)
                            break
                        }
                    }
                   }


                override fun onCancelled(error: DatabaseError) {
                    Log.e("Error", error.message)
                }
            })
    }



    //Don't forget to add in Time Start + Time End
    //Also need to add Repeat section when that is done
    //Luis will also need to add the logic for saving the date in here!

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                binding.taskDate.text = selectedDate.toString()


            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }
    private fun updateTaskInFirebase(taskId: String, newTitle: String, newDescription: String,newDate: String) {

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/tasks")

        val startTime = binding.startTimeTextView.text.toString()
        val endTime = binding.endTimeTextView.text.toString()

        ref.orderByChild("id").equalTo(taskId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for(taskSnapshot in snapshot.children) {

                        val updates = mapOf<String, Any>(
                            "title" to newTitle,
                            "description" to newDescription,
                            "date" to newDate,
                            "startTime" to startTime,
                            "endTime" to endTime,
                            "repeats" to repeatToggles
                        )
                        taskSnapshot.ref.updateChildren(updates)
                        break
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Error", error.message)
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

        val icon = cardView.findViewById<ImageView>(R.id.media_Icon)
        val label = cardView.findViewById<TextView>(R.id.media_Name)

        label.text = title

        val uri = when (imageUriOrUrl) {
            is Uri -> imageUriOrUrl
            is String -> Uri.parse(imageUriOrUrl)
            else -> return
        }

        cardView.setOnClickListener {
            showImagePopup(uri, container, cardView)
        }

        container.addView(cardView)
    }

    private fun showImagePopup(imageUri: Uri, container: LinearLayout, view: View) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_popup,null)
        val imageView = dialogView.findViewById<ImageView>(R.id.popupImage)
        val closeButton = dialogView.findViewById<Button>(R.id.closePopup)
        val deleteButton = dialogView.findViewById<Button>(R.id.deleteButton)


        Glide.with(this)
            .load(imageUri)
            .into(imageView)


        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        closeButton.setOnClickListener { dialog.dismiss()}
        deleteButton.setOnClickListener {
            container.removeView(view)
                dialog.dismiss() }
        dialog.show()
    }

    private fun getTitle(title: (String?) -> Unit) {
        val input = EditText(requireContext())
        input.hint = "Title"
        input.maxLines = 1
        input.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(5))

        val dialog = AlertDialog.Builder(requireContext()).setTitle("Title your Image").setView(input).setPositiveButton("Ok") {_, _ ->
            val text = input.text.toString().trim()
            if (text.length in 1..5) {
                title(text)
            } else {
                Toast.makeText(context, "Please enter a valid text up to 5 characters", Toast.LENGTH_LONG).show()
                title(null)
            }
        }.setNegativeButton("Cancel") { _, _ ->
            title(null)
        }.create()

        dialog.show()
    }

    private fun uploadImgToDB(imageUri: Uri, title: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
        val taskKey = currentTaskKey
        val imageRef = storageRef.child("/users/$uid/tasks/$taskKey/${System.currentTimeMillis()}.jpg")

        imageRef.putFile(imageUri).addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                if (taskKey != null) {
                    saveImgUrlToDB(taskKey, title, downloadUri.toString())
                }
                addMediaCard("image", title, downloadUri)
            }
        }
        .addOnFailureListener {
            Toast.makeText(context, "Failed to upload image", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveImgUrlToDB(taskId: String, title: String, imageUrl: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid
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
                Log.e("Error", error.message)
            }
        })
    }

}

fun updateRepeatingTasks() {

    val functions = FirebaseFunctions.getInstance()

    val uid = FirebaseAuth.getInstance().currentUser?.uid

    val data = hashMapOf("uid" to uid)

    functions
        .getHttpsCallable("setRepeatingTasksOnCall")
        .call(data)
}