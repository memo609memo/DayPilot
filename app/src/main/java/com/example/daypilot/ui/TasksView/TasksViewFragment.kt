package com.example.daypilot.ui.TasksView

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.daypilot.R
import com.example.daypilot.databinding.FragmentTasksViewBinding
import com.example.daypilot.ui.notes.SwipeToActionCallback
import com.example.daypilot.ui.notes.Task
import com.example.daypilot.ui.notes.TaskAdapter
import com.example.daypilot.ui.notes.isDarkMode


class TasksViewFragment : Fragment() {

    private var _binding: FragmentTasksViewBinding? = null
    private val binding get() = _binding!!

    private  lateinit var adapter: TaskAdapter
    private lateinit var tasksViewModel: TasksViewViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        _binding = FragmentTasksViewBinding.inflate(inflater, container, false)
        val root = binding.root

        tasksViewModel = ViewModelProvider(this).get(TasksViewViewModel::class.java)


        adapter = TaskAdapter(
            onEdit = {},
            onDelete = {},
            onComplete = {},
            onItemClicked = { clickedTask ->
                val bundle = Bundle().apply {
                    putString("taskId", clickedTask.id)
                }
                findNavController().navigate(R.id.action_navigation_tasksView_to_notifications, bundle)
            },
            matchParentWidth = true,

        )

        binding.recyclerTasksView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTasksView.adapter = adapter

        tasksViewModel.allTasks.observe(viewLifecycleOwner, { tasks ->
            adapter.submitList(tasks)
        })

        // Attach swipe callback
        val swipeCallback = SwipeToActionCallback(
            requireContext(),
            adapter,
            onEdit = { position ->
                val task = adapter.currentList[position]
            },
            onDelete = { position ->
                val task = adapter.currentList[position]
                tasksViewModel.deleteTask(task)
            }
        )
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerTasksView)

        // Add task button
        binding.btnAddTask.setOnClickListener {
            showAddTaskDialog()
        }

        tasksViewModel.loadAllTasks()

        return root
    }

    private fun showAddTaskDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task_notime, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.editTextTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.editTextDescription)

        val alertDialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setTitle("Add Task")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleInput.text.toString()
                val description = descriptionInput.text.toString()
                if (title.isNotBlank()) {
                    //Michael: Moving the id creation to here from Task.kt so firebase serializing works correctly
                    val task = Task(id = System.currentTimeMillis().toString(), title = title, description = description, date = "")
                    tasksViewModel.addTask(task)

                } else {
                    Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        alertDialog.setOnShowListener {
            val saveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val cancelButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            val isDark = requireContext().isDarkMode()
            if (!isDark) {
                saveButton.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_color
                    )
                )
                cancelButton.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_color
                    )
                )
            } else {
                saveButton.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.dark_text_color
                    )
                )
                cancelButton.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.dark_text_color
                    )
                )
            }
        }

        alertDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }



}
