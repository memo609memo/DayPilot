package com.example.daypilot.ui.notes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.daypilot.R
import com.example.daypilot.databinding.ItemTaskBinding

// Interface to handle task drag events
interface TaskDragListener {
    // Called when a task is moved from one hour block to another
    fun onTaskMoved(task: Task, fromHour: Int, toHour: Int)
}
class TaskAdapter(
    private val onEdit: (Task) -> Unit,// Callback for editing a task
    private val onDelete: (Task) -> Unit, // Callback for deleting a task
    private val onComplete: (Task) -> Unit,// Callback for marking a task complete
    private val onItemClicked: (Task) -> Unit,// Callback for clicking the task
    private val matchParentWidth: Boolean = false,
    private val dragListener: TaskDragAndDropHelper? = null,
    private val hourBlock: Int? = null,
    private var startDragListener: ((View, Task) -> Unit)? = null// Callback to start drag
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(DiffCallBack()){

    inner class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {

            val darkMode = binding.root.context.isDarkMode()// Check if dark mode is enabled

            // Set task title
            binding.textViewTitle.text = task.title
            // Set task description or default text if empty
            binding.textViewDescription.text =
                task.description.takeIf { it.isNotBlank() } ?: "No description"
            // Set task start-end time or default text
            binding.textViewTime.text =
                if (task.startTime.isNotBlank() && task.endTime.isNotBlank())
                    "${task.startTime} – ${task.endTime}"
                else
                    "No time specified"
            // Customize UI if task is completed
            if (task.isCompleted) {
                binding.textViewIsCompleted.visibility = View.VISIBLE
                binding.textViewTime.visibility = View.GONE
                binding.textViewDescription.visibility = View.GONE
                if (!darkMode) {
                    binding.cardViewTask.setCardBackgroundColor(
                        ContextCompat.getColor(
                            binding.root.context,
                            R.color.completedTaskBackground
                        )
                    )
                }
                else {
                    binding.cardViewTask.setCardBackgroundColor(
                        ContextCompat.getColor(
                            binding.root.context,
                            R.color.dark_completedTaskBackground
                        )
                    )
                }
            } else {
                binding.textViewIsCompleted.visibility = View.GONE
                binding.textViewTime.visibility = View.VISIBLE
                binding.textViewDescription.visibility = View.VISIBLE
                // Set card background color based on priority
                binding.cardViewTask.setCardBackgroundColor(
                    task.priority?.getColor(binding.root.context) ?: ContextCompat.getColor(binding.root.context, R.color.priority_default)
                )

            }
            // Adjust task card width if needed
            val params = binding.cardViewTask.layoutParams
            params.width = if (matchParentWidth) {
                ViewGroup.LayoutParams.MATCH_PARENT
            } else {
                ViewGroup.LayoutParams.WRAP_CONTENT
            }
            binding.cardViewTask.layoutParams = params


            // Handle "more" menu clicks for edit, delete, complete
            binding.imageViewMore.setOnClickListener {
                val popup = PopupMenu(binding.root.context, binding.imageViewMore)
                popup.menuInflater.inflate(R.menu.menu_task_options, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_edit -> { onEdit(task); true }
                        R.id.menu_delete -> { onDelete(task); true }
                        R.id.menu_complete -> { onComplete(task); true }
                        else -> false
                    }
                }
                popup.show()
            }
            // Handle task card click
            binding.root.setOnClickListener {
                onItemClicked(task)
            }
            // Handle long press to start drag
            binding.cardViewTask.setOnLongClickListener {
                startDragListener?.invoke(it, task)
                true
            }
        }
    }
    // Allow setting the drag listener from outside
    fun setOnStartDragListener(listener: (View, Task) -> Unit) {
        startDragListener = listener
    }

    // DiffUtil for optimizing RecyclerView updates
    class DiffCallBack : DiffUtil.ItemCallback<Task>(){
        // Check if items represent the same task by ID
        override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id

        // Check if contents are the same (title, description, etc.)
        override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int ): TaskViewHolder{
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }
    // Bind the task data to ViewHolder
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task)
    }



}
