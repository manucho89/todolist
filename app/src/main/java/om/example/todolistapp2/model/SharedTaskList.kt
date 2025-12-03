package com.example.todolistapp2.model

data class SharedTaskList(
    val listName: String,
    val listColor: Int,
    val tasks: List<SharedTask>
)

data class SharedTask(
    val title: String,
    val isCompleted: Boolean
)