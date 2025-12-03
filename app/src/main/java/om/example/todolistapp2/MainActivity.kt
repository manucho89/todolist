package com.example.todolistapp2

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolistapp2.adapter.TaskListAdapter
import com.example.todolistapp2.model.TaskList
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskListAdapter
    private val taskLists = mutableListOf<TaskList>()
    private var nextId = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.recyclerViewLists)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = TaskListAdapter(taskLists) { taskList ->
            // TODO: Navegar a la pantalla de tareas
            // Por ahora solo mostramos un mensaje
            showMessage("Clicked en: ${taskList.name}")
        }

        recyclerView.adapter = adapter

        // Configurar botón flotante
        val fabAddList: FloatingActionButton = findViewById(R.id.fabAddList)
        fabAddList.setOnClickListener {
            showAddListDialog()
        }

        // Añadir algunas listas de ejemplo
        addSampleLists()
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
                        id = nextId++,
                        name = listName,
                        color = getRandomColor()
                    )
                    adapter.addList(newList)
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

    private fun addSampleLists() {
        val sampleLists = listOf(
            TaskList(nextId++, "Compras", 0xFF4CAF50.toInt(), 5),
            TaskList(nextId++, "Trabajo", 0xFF2196F3.toInt(), 3),
            TaskList(nextId++, "Personal", 0xFFE91E63.toInt(), 7)
        )

        taskLists.addAll(sampleLists)
        adapter.notifyDataSetChanged()
    }

    private fun showMessage(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}