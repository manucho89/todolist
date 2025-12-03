package com.example.todolistapp2

import android.os.Bundle
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolistapp2.adapter.TaskAdapter
import com.example.todolistapp2.model.Task
import com.example.todolistapp2.viewmodel.TaskViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TasksActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskAdapter
    private val viewModel: TaskViewModel by viewModels()
    private var listId: Int = -1
    private var listName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks)

        // Obtener datos de la lista desde el Intent
        listId = intent.getIntExtra("LIST_ID", -1)
        listName = intent.getStringExtra("LIST_NAME") ?: "Tareas"

        // Configurar Toolbar
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.title = listName
        toolbar.setNavigationOnClickListener {
            finish() // Volver atrás
        }

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.recyclerViewTasks)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = TaskAdapter(
            onTaskChecked = { task ->
                viewModel.updateTask(task)
            },
            onTaskDelete = { task ->
                showDeleteConfirmation(task)
            }
        )

        recyclerView.adapter = adapter

        // Observar cambios en las tareas
        viewModel.getTasksForList(listId).observe(this) { tasks ->
            adapter.submitList(tasks)
        }

        // Configurar botón flotante
        val fabAddTask: FloatingActionButton = findViewById(R.id.fabAddTask)
        fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun showAddTaskDialog() {
        val editText = EditText(this)
        editText.hint = "Título de la tarea"

        AlertDialog.Builder(this)
            .setTitle("Nueva Tarea")
            .setView(editText)
            .setPositiveButton("Crear") { _, _ ->
                val taskTitle = editText.text.toString()
                if (taskTitle.isNotBlank()) {
                    val newTask = Task(
                        title = taskTitle,
                        listId = listId
                    )
                    viewModel.insertTask(newTask)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteConfirmation(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar tarea")
            .setMessage("¿Estás seguro de que quieres eliminar '${task.title}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteTask(task)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}