package com.example.todolistapp2.model

data class TaskListWithCountExtended(
    val taskList: TaskList?,
    val taskCount: Int,
    val isSpecialImportant: Boolean = false
)