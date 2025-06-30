package com.example.daypilot.ui.notes

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.daypilot.R

class HourBlockAdapter( private val onTaskClick: (Task) -> Unit
): ListAdapter<HourBlock, HourBlockAdapter.HourBlockViewHolder>(DiffCallBack()) {

    inner class HourBlockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val hourTextView: TextView= itemView.findViewById(R.id.textViewHour)
        val taskContainerLayout: LinearLayout = itemView.findViewById(R.id.layoutTasksContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourBlockViewHolder {
       val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hour_block,parent,false)
        return HourBlockViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourBlockViewHolder, position: Int) {
        val hourBlock = currentList[position]

        holder.hourTextView.text = if (hourBlock.hour == -1)
            "All Day"
        else
            String.format("%02d:00 - %02d:00", hourBlock.hour, (hourBlock.hour + 1) % 24)

        val container = holder.taskContainerLayout
        container.removeAllViews()

        if (hourBlock.tasks.isEmpty()) {
            val noTaskView = TextView(holder.itemView.context).apply {
                text = "No tasks"
                setTextColor(Color.GRAY)
                setPadding(16, 8, 0, 8)
            }
            container.addView(noTaskView)
        } else {
            hourBlock.tasks.forEach { task ->
                val card = LayoutInflater.from(holder.itemView.context)
                    .inflate(R.layout.item_task, container, false)

                val titleView = card.findViewById<TextView>(R.id.textViewTitle)
                val descView = card.findViewById<TextView>(R.id.textViewDescription)

                titleView.text = task.title
                descView.text = if (task.startTime.isNotBlank() && task.endTime.isNotBlank()) {
                    "${task.startTime} - ${task.endTime}"
                } else {
                    "No time specified"
                }
                card.setOnClickListener {
                    onTaskClick(task)
                }

                container.addView(card)
            }
        }
    }
    class DiffCallBack : DiffUtil.ItemCallback<HourBlock>() {
        override fun areItemsTheSame(oldItem: HourBlock, newItem: HourBlock) = oldItem.hour == newItem.hour
        override fun areContentsTheSame(oldItem: HourBlock, newItem: HourBlock) = oldItem == newItem
    }
}