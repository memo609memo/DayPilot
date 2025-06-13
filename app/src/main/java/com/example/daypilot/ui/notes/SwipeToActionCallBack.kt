package com.example.daypilot.ui.notes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SwipeToActionCallback(
    private val context: Context,
    private val adapter: TaskAdapter,
    private val onEdit: (Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val backgroundPaint = Paint().apply { color = Color.LTGRAY }
    private val deletePaint = Paint().apply { color = Color.RED; textSize = 40f; isFakeBoldText = true }
    private val editPaint = Paint().apply { color = Color.BLUE; textSize = 40f; isFakeBoldText = true }

    override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition

        AlertDialog.Builder(context)
            .setTitle("Choose action")
            .setItems(arrayOf("Edit", "Delete")) { _, which ->
                when (which) {
                    0 -> onEdit(position)
                    1 -> onDelete(position)
                }
            }
            .setOnDismissListener {
                notifyRestore(viewHolder)
            }
            .show()
    }

    private fun notifyRestore(viewHolder: RecyclerView.ViewHolder) {
        val position = viewHolder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            adapter.notifyItemChanged(position) // triggers rebind + animation
        }
    }
    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float, dY: Float,
        actionState: Int, isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val height = itemView.bottom - itemView.top
        val width = height / 3

        if (dX < 0) {

            canvas.drawRect(
                itemView.right.toFloat() + dX,
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat(),
                backgroundPaint
            )


            canvas.drawText("Delete", itemView.right - 3 * width.toFloat(), itemView.top + height / 1.5f, deletePaint)


            canvas.drawText("Edit", itemView.right - 6 * width.toFloat(), itemView.top + height / 1.5f, editPaint)
        }

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}