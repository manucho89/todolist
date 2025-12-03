package com.example.todolistapp2

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolistapp2.adapter.TaskListAdapter
import com.example.todolistapp2.model.TaskList
import com.example.todolistapp2.viewmodel.TaskListViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskListAdapter
    private val viewModel: TaskListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.recyclerViewLists)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = TaskListAdapter { taskList ->
            openTasksActivity(taskList)
        }

        recyclerView.adapter = adapter

        // Observar cambios en las listas
        viewModel.allListsWithCount.observe(this) { listsWithCount ->
            adapter.submitList(listsWithCount)
        }

        // Configurar botón flotante
        val fabAddList: FloatingActionButton = findViewById(R.id.fabAddList)
        fabAddList.setOnClickListener {
            showAddListDialog()
        }
    }

    private fun showAddListDialog() {
        val editText = EditText(this)
        editText.hint = "Nombre de la lista"

        AlertDialog.Builder(this)
            .setTitle("Nueva Lista")
            .setView(editText)
            .setPositiveButton("Crear") { _, _ ->
                val listName = editText.text.toString()
                if (listName.isNotBlank()) {
                    val newList = TaskList(
                        name = listName,
                        color = getRandomColor()
                    )
                    viewModel.insertList(newList)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun getRandomColor(): Int {
        val colors = listOf(
            0xFFE91E63.toInt(), // Rosa
            0xFF9C27B0.toInt(), // Púrpura
            0xFF673AB7.toInt(), // Púrpura oscuro
            0xFF3F51B5.toInt(), // Índigo
            0xFF2196F3.toInt(), // Azul
            0xFF009688.toInt(), // Verde azulado
            0xFF4CAF50.toInt(), // Verde
            0xFFFF9800.toInt(), // Naranja
            0xFFFF5722.toInt()  // Naranja oscuro
        )
        return colors[Random.nextInt(colors.size)]
    }

    private fun openTasksActivity(taskList: TaskList) {
        val intent = Intent(this, TasksActivity::class.java)
        intent.putExtra("LIST_ID", taskList.id)
        intent.putExtra("LIST_NAME", taskList.name)
        startActivity(intent)
    }
}