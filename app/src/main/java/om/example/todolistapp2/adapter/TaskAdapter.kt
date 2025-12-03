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
    private val onTaskDelete: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(DiffCallback()) {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBoxTask)
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewTaskTitle)
        val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDeleteTask)

        fun bind(task: Task) {
            textViewTitle.text = task.title
            checkBox.isChecked = task.isCompleted

            // Tachar el texto si está completada
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

            buttonDelete.setOnClickListener {
                onTaskDelete(task)
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