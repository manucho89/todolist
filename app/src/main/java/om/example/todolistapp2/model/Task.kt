package com.example.todolistapp2.model

data class Task(
    val id: Int,
    val title: String,
    var isCompleted: Boolean = false,
    val listId: Int // ID de la lista a la que pertenece
)