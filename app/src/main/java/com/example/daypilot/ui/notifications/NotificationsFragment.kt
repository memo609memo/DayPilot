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

    //variables///////////////////////////////////////////////////
    private var currentTaskKey: String? = null
    private val repeatToggles = MutableList(7) { false }
    private var selectedStartHour: Int = -1
    private var selectedStartMinute: Int = -1
    private var selectedEndHour: Int = -1
    private var selectedEndMinute: Int = -1
    /////////////////////////////////////////////////////////////

    private var _binding: FragmentNotificationsBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!



    //function for picking a photo from the users device
    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { imageUri ->
            getTitle { title ->
                if (title != null) {
                    uploadImgToDB(imageUri, title)
                }
            }
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel = ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //retrieve task id sent as an argument to this fragment
        val taskId = arguments?.getString("taskId")

        //call helper functions to setup UI components
        setupRepeatButtons()
        setupTimePicker()
        taskId?.let { getTaskDataFromFirebase(it)}

        //save button section
        binding.saveButton.setOnClickListener {
            //grab values for title, description, and new dates
            val newTitle = binding.editTaskName.text.toString()
            val updatedDescription = binding.noteBody.text.toString()
            val newDate = binding.taskDate.text.toString()

            //require a title for tasks
            if (newTitle.isEmpty()) {
                binding.editTaskName.error = "Please enter title"
                return@setOnClickListener
            }

            //call function to update the repeating tasks
            updateRepeatingTasks()

            //set values for start and end times
            val startTimeTextBox = binding.startTimeTextView.text.toString()
            val endTimeTextBox = binding.endTimeTextView.text.toString()

            //make sure that both start and end times are entered and that end time is after the start time
            if(startTimeTextBox.isNotBlank() && endTimeTextBox.isNotBlank()) {

                val startTimeMinMode = selectedStartHour * 60 + selectedStartMinute
                val endTimeMinMode = selectedEndHour * 60 + selectedEndMinute

                if (endTimeMinMode <= startTimeMinMode) {
                    binding.endTimeTextView.error = "End time must be later than start time"
                    return@setOnClickListener
                }
            }

            //after all is said and done with saving, update the information to firebase
            if (taskId != null) {
                updateTaskInFirebase(taskId, newTitle, updatedDescription,newDate)
            }

            findNavController().previousBackStackEntry
                ?.savedStateHandle
                ?.set("refreshNeeded", true)

            findNavController().popBackStack()
        }

        //cancel button to go back to previous page
        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        //call dialog for picking a date
        binding.taskDate.setOnClickListener {
            showDatePickerDialog()
        }

        //button for blowing up the repeats if its too small to click on screen for users
        binding.repeatTextView.setOnClickListener {
            showRepeatSelectionDialog()
        }

        //button for adding images to your task
        binding.imageButton.setOnClickListener {
            imagePicker.launch("image/*")
        }
        //not used yet, will be for adding voice memos to your task
        binding.micButton.setOnClickListener {
            //addMediaCard("voice", "test")
        }

        return root
    }

    private fun showRepeatSelectionDialog() {
        //create list of weekdays in order of buttons
        val weekDays = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        //convert the toggles to an array of booleans to show whether selected or not
        val selectedItems = repeatToggles.toBooleanArray()

        //build dialog
        val builder = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Repeat On")
            //set up multiple choice with weekdays and selected items
            .setMultiChoiceItems(weekDays.toTypedArray(), selectedItems){ _, index, isChecked ->
                //update repeat toggles list
                repeatToggles[index] = isChecked
            }
                //update UI when clicked
                .setPositiveButton("Ok") { _, _ ->
                    setupRepeatButtons()
                }
                .setNegativeButton("Cancel", null)
        //create dialog
        builder.create().show()
    }


    private fun setupRepeatButtons() {
        //assigning repeat buttons to list
        val buttons = listOf(
            binding.repeatSunday,
            binding.repeatMonday,
            binding.repeatTuesday,
            binding.repeatWednesday,
            binding.repeatThursday,
            binding.repeatFriday,
            binding.repeatSaturday
        )

        //using list set the onclick to either enable or disable the button (selected) that is all (js handles actual repeat logic)
        buttons.forEachIndexed { index, button ->
            button.isSelected = repeatToggles[index]


            button.setOnClickListener {
                repeatToggles[index] = !repeatToggles[index]
                button.isSelected = repeatToggles[index]


            }
        }
    }

    //set up timepickers so when clicked itll open the dialog pertaining to whichever one was clicked (start or end) and send time data with it
    private fun setupTimePicker() {
        binding.startTimeTextView.setOnClickListener {
            showTimePickerDialog(selectedStartHour, selectedStartMinute, binding.startTimeTextView)

        }

        binding.endTimeTextView.setOnClickListener {
            showTimePickerDialog(selectedEndHour, selectedEndMinute, binding.endTimeTextView)
        }
    }

    //
    private fun showTimePickerDialog(initialHour: Int, initialMinute: Int, textView: TextView){
        //get current time to use as the default time
        val calendar = Calendar.getInstance()
        //determine the hour and minute to display - either already existing OR the default time from the calendar
        val hourToDisplay = if (initialHour != -1) initialHour else calendar.get(Calendar.HOUR_OF_DAY);
        val minuteToDisplay = if (initialMinute != -1) initialMinute else calendar.get(Calendar.MINUTE);


        //creating the time picker dialog
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            //handle user selection
            { _, selectedHour: Int, selectedMinute: Int ->
                //check which text view was clicked to know which value we are changing
                when(textView) {
                    //change start time variables
                    binding.startTimeTextView -> {
                        //update variables
                        selectedStartHour = selectedHour
                        selectedStartMinute = selectedMinute

                        //if there is no end time (which means user is doing this for the first time) then end time will automatically be the same time + 1 hour
                        if(binding.endTimeTextView.text.isBlank()) {
                            selectedEndHour = selectedHour + 1
                            selectedEndMinute = selectedMinute
                            updateTimeTextView(binding.endTimeTextView, selectedEndHour, selectedEndMinute)
                        }
                    }
                    //change end time variables
                    binding.endTimeTextView -> {
                        //update variables
                        selectedEndHour = selectedHour
                        selectedEndMinute = selectedMinute
                    }
                }
                //update textviews
                updateTimeTextView(textView, selectedHour, selectedMinute)
            },
            //set initial hour and minute + 24H setting
            hourToDisplay,
            minuteToDisplay,
            false
        )
        //show the dialog
        timePickerDialog.show()
    }

    private fun updateTimeTextView(textView: TextView, selectedHour: Int, selectedMinute: Int) {
        //if the hour and minute are actually selected
        if (selectedHour != -1 && selectedMinute != -1) {
            //send the 24 hour time to the formatter
            val formattedTime = formatTime(selectedHour, selectedMinute)
            //set the textview to the returned formatted time
            textView.text = formattedTime
        }
    }

    //function for formatting time into 12 hour AM PM
    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM";
        val displayHour = if(hour == 0 || hour == 12) 12 else hour % 12
        // %02d will force hour and minute to have a padded zero if single digit
        return String.format("%02d:%02d %s", displayHour, minute, amPm)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getTaskDataFromFirebase(taskId: String) {
        //set up user and reference to database
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/tasks")

        //search through DB for task with matching ID
        ref.orderByChild("id").equalTo(taskId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //go through results (1)
                    for(taskSnapshot in snapshot.children) {
                        //convert snapshot to task id
                        val task = taskSnapshot.getValue(Task::class.java)
                        if (task != null) {
                            //store key for later
                            currentTaskKey = taskSnapshot.key

                            //populate the UI with the results from the database
                            binding.editTaskName.setText(task.title)
                            binding.noteBody.setText(task.description)
                            binding.taskDate.text = task.date
                            binding.startTimeTextView.text = task.startTime
                            binding.endTimeTextView.text = task.endTime
                            val repeatList = task.repeats
                            //fill each repeat button depending on values
                            repeatList.forEachIndexed{ index, value ->
                                repeatToggles[index] = value
                            }
                            //call func to set up the repeat buttons for usability
                            setupRepeatButtons()
                            //call func to load the media cards
                            loadMediaCards(currentTaskKey!!)
                            //stop after matching and finishing
                            break
                        }
                    }
                   }

                //log any errors
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

        //set up user and database references
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/tasks")

        //bind start and end times to values from UI
        val startTime = binding.startTimeTextView.text.toString()
        val endTime = binding.endTimeTextView.text.toString()

        //search through db to find task with matching ID
        ref.orderByChild("id").equalTo(taskId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //go through results (1)
                    for(taskSnapshot in snapshot.children) {
                        //create map of children for all fields which are getting updated/uploaded to DB
                        val updates = mapOf<String, Any>(
                            "title" to newTitle,
                            "description" to newDescription,
                            "date" to newDate,
                            "startTime" to startTime,
                            "endTime" to endTime,
                            "repeats" to repeatToggles
                        )
                        //apply updates
                        taskSnapshot.ref.updateChildren(updates)
                        //stop after found and updated
                        break
                    }
                }

                //log any errors
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Error", error.message)
                }
            })
    }




    private fun addMediaCard(type: String, title: String, imageUriOrUrl: Any) {

        //get container for cards to be added to
        val container = binding.cardContainer

        //get correct card for type (only image cards can be added at this time)
        val layoutId = when (type) {
            "image" -> R.layout.image_card
            "voice" -> R.layout.voice_card
            else -> R.layout.image_card
        }


        val cardView = layoutInflater.inflate(layoutId, container, false)

        //get label and icon for card (icon doesn't matter right now as much since only image cards exists)
        val icon = cardView.findViewById<ImageView>(R.id.media_Icon)
        val label = cardView.findViewById<TextView>(R.id.media_Name)

        //set label as the title of the image
        label.text = title

        //convert imageuri or url to uri obj
        val uri = when (imageUriOrUrl) {
            is Uri -> imageUriOrUrl
            is String -> Uri.parse(imageUriOrUrl)
            //not supported then exit
            else -> return
        }

        //set click to show pop up of uploaded img when card is tapped
        cardView.setOnClickListener {
            showImagePopup(uri, container, cardView)
        }

        //add newly added card to the container
        container.addView(cardView)
    }

    //when user clicks an already added media card containing an image, this popup will show them the image they uploaded
    private fun showImagePopup(imageUri: Uri, container: LinearLayout, view: View) {
        //setup the dialogs layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_popup,null)
        val imageView = dialogView.findViewById<ImageView>(R.id.popupImage)
        val closeButton = dialogView.findViewById<Button>(R.id.closePopup)
        val deleteButton = dialogView.findViewById<Button>(R.id.deleteButton)

        //use glide to load the image into the imageview
        Glide.with(this)
            .load(imageUri)
            .into(imageView)

        //create dialog with view created
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        //assign buttons with functionality
        closeButton.setOnClickListener { dialog.dismiss()}
        deleteButton.setOnClickListener {
            container.removeView(view)
                dialog.dismiss() }

        //show dialog to user
        dialog.show()
    }

    //function to create titles for media items
    private fun getTitle(title: (String?) -> Unit) {

        //dialog setup and rules
        val input = EditText(requireContext())
        input.hint = "Title"
        input.maxLines = 1
        input.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(5))

        //build and show the dialog
        val dialog = AlertDialog.Builder(requireContext()).setTitle("Title your Image").setView(input).setPositiveButton("Ok") {_, _ ->
            //grab trimmed text from input box
            val text = input.text.toString().trim()
            //follow rules
            if (text.length in 1..5) {
                //set the title to the users entered name
                title(text)
            } else { //set to null if the user did not follow rules and warn them so
                Toast.makeText(context, "Please enter a valid text up to 5 characters", Toast.LENGTH_LONG).show()
                title(null)
            }
        }.setNegativeButton("Cancel") { _, _ ->
            title(null)
        }.create()

        dialog.show()
    }

    //function to upload image to our database
    private fun uploadImgToDB(imageUri: Uri, title: String) {

        // userid and reference to database location
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference

        //grab current task and create reference for selected image
        val taskKey = currentTaskKey
        val imageRef = storageRef.child("/users/$uid/tasks/$taskKey/${System.currentTimeMillis()}.jpg")

        //upload image to firebase storage
        imageRef.putFile(imageUri).addOnSuccessListener {
            //if successful upload then we get public URl for img
            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                //if the task key already exists save URL to database
                if (taskKey != null) {
                    saveImgUrlToDB(taskKey, title, downloadUri.toString())
                }
                //add a media card to show the newly added image on the UI
                addMediaCard("image", title, downloadUri)
            }
        } // tell the user if their upload fails
        .addOnFailureListener {
            Toast.makeText(context, "Failed to upload image", Toast.LENGTH_LONG).show()
        }
    }

    //function to save the image URL to our database
    private fun saveImgUrlToDB(taskId: String, title: String, imageUrl: String) {

        // userid and reference to database location
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/tasks/$taskId/media")

        //generate new key for media item
        val mediaId = ref.push().key ?: return

        //create map to hold all of the different children of the media item
        val mediaData = mapOf(
            "id" to mediaId,
            "type" to "image",
            "title" to title,
            "url" to imageUrl
        )

        //save the medias map of info to the database under the newly created key
        ref.child(mediaId).setValue(mediaData)
    }

    //this function is used for loading each image/voice memo attached to a task
    private fun loadMediaCards(taskId: String) {

        // userid and reference to database location
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/tasks/$taskId/media")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //loop through each media item
                for (mediaSnapshot in snapshot.children) {
                    //grab the type, title, and url for each media
                    val type = mediaSnapshot.child("type").getValue(String::class.java) ?: continue
                    val title = mediaSnapshot.child("title").getValue(String::class.java) ?: continue
                    val url = mediaSnapshot.child("url").getValue(String::class.java) ?: continue

                    //call the add media card function so it can display on the page
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