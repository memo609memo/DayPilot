package com.example.daypilot.ui.notes

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class TaskDragAndDropHelper(
    private val adapter: HourBlockAdapter,
    private val dragListener: TaskDragListener
) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        // Allow dragging up and down
        return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPos = viewHolder.bindingAdapterPosition
        val toPos = target.bindingAdapterPosition

        val fromBlock = adapter.currentList.getOrNull(fromPos) ?: return false
        val toBlock = adapter.currentList.getOrNull(toPos) ?: return false

        if (fromBlock.tasks.isEmpty()) return false

        val task = fromBlock.tasks.first()
        dragListener.onTaskMoved(task, fromBlock.hour, toBlock.hour)

        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

    }

    override fun isLongPressDragEnabled() = true
}