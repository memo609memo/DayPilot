package com.example.daypilot.ui.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.daypilot.databinding.ItemTaskBinding

class TaskAdapter(private val onItemClicked: (Task) -> Unit) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(DiffCallBack()){

    class  TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task, onItemClicked: (Task) -> Unit) {
            binding.textViewTitle.text = task.title
            binding.textViewDescription.text = task.description
            itemView.setOnClickListener { onItemClicked(task) }
        }
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

        holder.bind(task, onItemClicked)
    }

}
