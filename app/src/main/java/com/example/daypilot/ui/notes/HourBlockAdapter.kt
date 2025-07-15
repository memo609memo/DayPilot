package com.example.daypilot.ui.notes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.daypilot.R

class HourBlockAdapter(private val onEdit: (Task) -> Unit,
                       private val onDelete: (Task) -> Unit,
                       private val onComplete: (Task) -> Unit,
                       private val onTaskClick: (Task) -> Unit
): ListAdapter<HourBlock, HourBlockAdapter.HourBlockViewHolder>(DiffCallBack()) {

    var showEmptyMessage = false
    inner class HourBlockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val hourTextView: TextView= itemView.findViewById(R.id.textViewHour)
        val taskRecyclerView: RecyclerView = itemView.findViewById(R.id.recyclerViewTasksHorizontal)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourBlockViewHolder {
       val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hour_block,parent,false)
        return HourBlockViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourBlockViewHolder, position: Int) {
        val hourBlock = currentList[position]

        holder.hourTextView.text = when (hourBlock.hour) {
            -1 -> "All Day"
            0 -> "12 AM"
            in 1..11 -> "${hourBlock.hour} AM"
            12 -> "12 PM"
            in 13..23 -> "${hourBlock.hour - 12} PM"
            else -> "${hourBlock.hour}:00"
        }

        // Set up horizontal RecyclerView for tasks
        val taskAdapter = TaskAdapter(onEdit = onEdit, onDelete = onDelete, onComplete = onComplete, onItemClicked = onTaskClick)
        holder.taskRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
        holder.taskRecyclerView.adapter = taskAdapter
        holder.taskRecyclerView.setHasFixedSize(true)
        taskAdapter.submitList(hourBlock.tasks)
    }


    class DiffCallBack : DiffUtil.ItemCallback<HourBlock>() {
        override fun areItemsTheSame(oldItem: HourBlock, newItem: HourBlock) = oldItem.hour == newItem.hour
        override fun areContentsTheSame(oldItem: HourBlock, newItem: HourBlock) = oldItem == newItem
    }

    fun getFirstTaskPosition(): Int {
        return currentList.indexOfFirst { it.tasks.isNotEmpty() }.takeIf { it != -1 } ?: 0
    }
}