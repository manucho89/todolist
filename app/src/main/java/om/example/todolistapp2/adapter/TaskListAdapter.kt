package com.example.todolistapp2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.todolistapp2.R
import com.example.todolistapp2.model.TaskList

class TaskListAdapter(
    private val taskLists: MutableList<TaskList>,
    private val onListClick: (TaskList) -> Unit
) : RecyclerView.Adapter<TaskListAdapter.TaskListViewHolder>() {

    inner class TaskListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorIndicator: View = itemView.findViewById(R.id.colorIndicator)
        val textViewListName: TextView = itemView.findViewById(R.id.textViewListName)
        val textViewTaskCount: TextView = itemView.findViewById(R.id.textViewTaskCount)

        fun bind(taskList: TaskList) {
            colorIndicator.setBackgroundColor(taskList.color)
            textViewListName.text = taskList.name
            textViewTaskCount.text = "${taskList.taskCount} tareas"

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
        holder.bind(taskLists[position])
    }

    override fun getItemCount(): Int = taskLists.size

    fun addList(taskList: TaskList) {
        taskLists.add(taskList)
        notifyItemInserted(taskLists.size - 1)
    }
}