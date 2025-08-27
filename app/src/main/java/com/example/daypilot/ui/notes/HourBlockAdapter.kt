package com.example.daypilot.ui.notes

import android.content.ClipData
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.daypilot.R
// Adapter for displaying a list of HourBlocks in the agenda view
class HourBlockAdapter(private val onEdit: (Task) -> Unit,// Callback when a task is edited, deleted,mar as completed etc
                       private val onDelete: (Task) -> Unit,
                       private val onComplete: (Task) -> Unit,
                       private val onTaskClick: (Task) -> Unit,
                       private val dragListener: TaskDragListener,
                       var dragHelper: TaskDragAndDropHelper? = null
): ListAdapter<HourBlock, HourBlockAdapter.HourBlockViewHolder>(DiffCallBack()) {
    // Flag to optionally show/hide a message when no tasks exist
    var showEmptyMessage = false

    // ViewHolder for each hour block
    inner class HourBlockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val hourTextView: TextView= itemView.findViewById(R.id.textViewHour)
        val taskRecyclerView: RecyclerView = itemView.findViewById(R.id.recyclerViewTasksHorizontal)

    }
    // Inflate the layout for an hour block
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourBlockViewHolder {
       val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hour_block,parent,false)
        return HourBlockViewHolder(view)
    }
    // Bind hour block data to the ViewHolder
    override fun onBindViewHolder(holder: HourBlockViewHolder, position: Int) {
        val hourBlock = currentList[position]// Get the HourBlock at this position

        // Format the hour label(1 AM, 12 Pm, etc)
        holder.hourTextView.text = when (hourBlock.hour) {
            -1 -> "All Day"// Special case for all-day tasks
            0 -> "12 AM"
            in 1..11 -> "${hourBlock.hour} AM"
            12 -> "12 PM"
            in 13..23 -> "${hourBlock.hour - 12} PM"
            else -> "${hourBlock.hour}:00"
        }
        // Create a TaskAdapter for the horizontal RecyclerView of tasks
        val taskAdapter = TaskAdapter(
            onEdit = onEdit,
            onDelete = onDelete,
            onComplete = onComplete,
            onItemClicked = onTaskClick,
            dragListener = null,// individual tasks don't handle drag here
            hourBlock = hourBlock.hour// provide the hour for task reference
        )
        // Set up the horizontal RecyclerView for tasks
        holder.taskRecyclerView.apply {
            layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
            adapter = taskAdapter
            setHasFixedSize(true)
        }
        // Submit the tasks for this hour block to the adapter
        taskAdapter.submitList(hourBlock.tasks)

        // Enable drag-and-drop support for moving tasks between hours
        holder.itemView.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DROP -> {
                    // Retrieve the dragged task and its original hour
                    val dragData = event.localState as? Pair<Task, Int> ?: return@setOnDragListener true
                    val task = dragData.first
                    val fromHour = dragData.second
                    val toHour = hourBlock.hour
                    // Only trigger move if the task is dropped in a different hour
                    if (fromHour != toHour) {
                        dragListener.onTaskMoved(task, fromHour, toHour)
                    }
                    true
                }
                else -> true
            }
        }
        // Set up the drag start listener for each task in this hour block
        taskAdapter.setOnStartDragListener { view, task ->
            val dragShadow = View.DragShadowBuilder(view)
            val dragData = Pair(task, hourBlock.hour)
            view.startDragAndDrop(
                ClipData.newPlainText("taskId", task.id),  // ClipData for the dragged task
                dragShadow,
                dragData,
                0
            )
        }
    }


    class DiffCallBack : DiffUtil.ItemCallback<HourBlock>() {
        override fun areItemsTheSame(oldItem: HourBlock, newItem: HourBlock) = oldItem.hour == newItem.hour
        override fun areContentsTheSame(oldItem: HourBlock, newItem: HourBlock) = oldItem == newItem
    }
    // Returns the position of the first hour block with tasks, or 0 if none exist
    fun getFirstTaskPosition(): Int {
        return currentList.indexOfFirst { it.tasks.isNotEmpty() }.takeIf { it != -1 } ?: 0
    }
}