package com.example.daypilot.ui.notes

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
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
        val taskContainerLayout: LinearLayout = itemView.findViewById(R.id.layoutTasksContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourBlockViewHolder {
       val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hour_block,parent,false)
        return HourBlockViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourBlockViewHolder, position: Int) {
        val hourBlock = currentList[position]
        val container = holder.taskContainerLayout
        container.removeAllViews()

        holder.hourTextView.text = if (hourBlock.hour == -1)
            "All Day"
        else
            String.format("%02d:00 - %02d:00", hourBlock.hour, (hourBlock.hour + 1) % 24)

        hourBlock.tasks.forEach { task ->
            val card = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.item_task, container, false)

            val titleView = card.findViewById<TextView>(R.id.textViewTitle)
            val descView = card.findViewById<TextView>(R.id.textViewDescription)
            val timeView = card.findViewById<TextView>(R.id.textViewTime)
            val completedView = card.findViewById<TextView>(R.id.textViewIsCompleted)
            val moreOptions = card.findViewById<ImageView>(R.id.imageViewMore)
            val cardView = card.findViewById<CardView>(R.id.cardViewTask)

            titleView.text = task.title

            if (task.isCompleted) {
                completedView.visibility = View.VISIBLE
                descView.visibility = View.GONE
                timeView.visibility = View.GONE
                Log.d("Adapter", "Setting background for completed task: ${task.title}")
                cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.completedTaskBackground))
            } else {
                completedView.visibility = View.GONE
                descView.visibility = View.VISIBLE
                timeView.visibility = View.VISIBLE
                cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.inputTextBox))

                descView.text = task.description.takeIf { it.isNotBlank() } ?: "No description"
                timeView.text = if (task.startTime.isNotBlank() && task.endTime.isNotBlank())
                    "${task.startTime} – ${task.endTime}"
                else
                    "No time specified"
            }

            moreOptions.setOnClickListener {
                val popup = PopupMenu(holder.itemView.context, moreOptions)
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

            card.setOnClickListener { onTaskClick(task) }

            container.addView(card)
        }
    }
    class DiffCallBack : DiffUtil.ItemCallback<HourBlock>() {
        override fun areItemsTheSame(oldItem: HourBlock, newItem: HourBlock) = oldItem.hour == newItem.hour
        override fun areContentsTheSame(oldItem: HourBlock, newItem: HourBlock) = oldItem == newItem
    }

    fun getFirstTaskPosition(): Int {
        return currentList.indexOfFirst { it.tasks.isNotEmpty() }.takeIf { it != -1 } ?: 0
    }
}