package com.example.todolistapp2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.todolistapp2.R
import com.example.todolistapp2.model.TaskList
import com.example.todolistapp2.model.TaskListWithCount

class TaskListAdapter(
    private val onListClick: (TaskList) -> Unit
) : ListAdapter<TaskListWithCount, TaskListAdapter.TaskListViewHolder>(DiffCallback()) {

    inner class TaskListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorIndicator: View = itemView.findViewById(R.id.colorIndicator)
        val textViewListName: TextView = itemView.findViewById(R.id.textViewListName)
        val textViewTaskCount: TextView = itemView.findViewById(R.id.textViewTaskCount)

        fun bind(taskListWithCount: TaskListWithCount) {
            val taskList = taskListWithCount.taskList
            colorIndicator.setBackgroundColor(taskList.color)
            textViewListName.text = taskList.name
            textViewTaskCount.text = "${taskListWithCount.taskCount} tareas"

            itemView.setOnClickListener {
                onListClick(taskList)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_list, parent, false)
        return TaskListViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class DiffCallback : DiffUtil.ItemCallback<TaskListWithCount>() {
        override fun areItemsTheSame(oldItem: TaskListWithCount, newItem: TaskListWithCount): Boolean {
            return oldItem.taskList.id == newItem.taskList.id
        }

        override fun areContentsTheSame(oldItem: TaskListWithCount, newItem: TaskListWithCount): Boolean {
            return oldItem == newItem
        }
    }
}