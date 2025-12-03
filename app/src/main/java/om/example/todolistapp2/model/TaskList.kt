package com.example.todolistapp2.model

data class TaskList(
    val id: Int,
    val name: String,
    val color: Int = 0xFF6200EE.toInt(),
    var taskCount: Int = 0
)