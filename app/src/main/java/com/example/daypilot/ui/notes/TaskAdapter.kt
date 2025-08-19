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


interface TaskDragListener {
    fun onTaskMoved(task: Task, fromHour: Int, toHour: Int)
}
class TaskAdapter(
    private val onEdit: (Task) -> Unit,
    private val onDelete: (Task) -> Unit,
    private val onComplete: (Task) -> Unit,
    private val onItemClicked: (Task) -> Unit,
    private val matchParentWidth: Boolean = false,
    private val dragListener: TaskDragAndDropHelper? = null,
    private val hourBlock: Int? = null,
    private var startDragListener: ((View, Task) -> Unit)? = null
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(DiffCallBack()){

    inner class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            binding.textViewTitle.text = task.title
            binding.textViewDescription.text =
                task.description.takeIf { it.isNotBlank() } ?: "No description"
            binding.textViewTime.text =
                if (task.startTime.isNotBlank() && task.endTime.isNotBlank())
                    "${task.startTime} – ${task.endTime}"
                else
                    "No time specified"

            if (task.isCompleted) {
                binding.textViewIsCompleted.visibility = View.VISIBLE
                binding.textViewTime.visibility = View.GONE
                binding.textViewDescription.visibility = View.GONE
                binding.cardViewTask.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.completedTaskBackground)
                )
            } else {
                binding.textViewIsCompleted.visibility = View.GONE
                binding.textViewTime.visibility = View.VISIBLE
                binding.textViewDescription.visibility = View.VISIBLE

                val priorityColorRes = task.priority?.colorResId ?: R.color.priority_default
                binding.cardViewTask.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, priorityColorRes)
                )

            }

            val params = binding.cardViewTask.layoutParams
            params.width = if (matchParentWidth) {
                ViewGroup.LayoutParams.MATCH_PARENT
            } else {
                ViewGroup.LayoutParams.WRAP_CONTENT
            }
            binding.cardViewTask.layoutParams = params



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

            binding.root.setOnClickListener {
                onItemClicked(task)
            }

            binding.cardViewTask.setOnLongClickListener {
                startDragListener?.invoke(it, task)
                true
            }
        }
    }
    fun setOnStartDragListener(listener: (View, Task) -> Unit) {
        startDragListener = listener
    }


    class DiffCallBack : DiffUtil.ItemCallback<Task>(){

        override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id


        override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int ): TaskViewHolder{
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task)
    }

}
