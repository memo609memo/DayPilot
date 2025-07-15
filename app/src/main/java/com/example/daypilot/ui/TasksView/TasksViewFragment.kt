package com.example.daypilot.ui.TasksView

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.daypilot.R
import com.example.daypilot.databinding.FragmentTasksViewBinding
import com.example.daypilot.ui.notes.*



class TasksViewFragment : Fragment() {

    private var _binding: FragmentTasksViewBinding? = null
    private val binding get() = _binding!!

    private  lateinit var adapter: TaskAdapter
    private lateinit var tasksViewModel: TasksViewViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        _binding = FragmentTasksViewBinding.inflate(inflater, container, false)
        val root = binding.root

        tasksViewModel = ViewModelProvider(this).get(TasksViewViewModel::class.java)


        adapter = TaskAdapter { clickedTask ->
            Toast.makeText(requireContext(), "Clicked Task: ${clickedTask.title}", Toast.LENGTH_SHORT).show()
            val bundle = Bundle().apply {
                putString("taskId", clickedTask.id)
            }
            findNavController().navigate(R.id.action_navigation_tasksView_to_notifications, bundle)
        }

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
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.editTextTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.editTextDescription)

        AlertDialog.Builder(requireContext())
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
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }



}
