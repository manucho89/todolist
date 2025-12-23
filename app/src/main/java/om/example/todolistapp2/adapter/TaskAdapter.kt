package com.example.todolistapp2.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.todolistapp2.R
import com.example.todolistapp2.model.Task

class TaskAdapter(
    private val onTaskChecked: (Task) -> Unit,
    private val onTaskDelete: (Task) -> Unit,
    private val onTaskPriorityChanged: (Task) -> Unit,
    private val onTaskLongClick: (Task) -> Unit  // ⭐ NUEVO callback para long click
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(DiffCallback()) {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBoxTask)
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewTaskTitle)
        val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDeleteTask)
        val buttonPriority: ImageButton = itemView.findViewById(R.id.buttonPriority)

        fun bind(task: Task) {
            textViewTitle.text = task.title
            checkBox.isChecked = task.isCompleted

            updatePriorityIcon(task.isPriority)

            if (task.isCompleted) {
                textViewTitle.paintFlags = textViewTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                textViewTitle.alpha = 0.5f
            } else {
                textViewTitle.paintFlags = textViewTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                textViewTitle.alpha = 1f
            }

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                val updatedTask = task.copy(isCompleted = isChecked)
                onTaskChecked(updatedTask)
            }

            buttonPriority.setOnClickListener {
                val updatedTask = task.copy(isPriority = !task.isPriority)
                onTaskPriorityChanged(updatedTask)
            }

            buttonDelete.setOnClickListener {
                onTaskDelete(task)
            }

            // ⭐ NUEVO: Long click en toda la tarea
            itemView.setOnLongClickListener {
                onTaskLongClick(task)
                true
            }
        }

        private fun updatePriorityIcon(isPriority: Boolean) {
            if (isPriority) {
                buttonPriority.setImageResource(android.R.drawable.star_big_on)
                buttonPriority.setColorFilter(android.graphics.Color.parseColor("#FFC107"))
            } else {
                buttonPriority.setImageResource(android.R.drawable.star_big_off)
                buttonPriority.clearColorFilter()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class DiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}