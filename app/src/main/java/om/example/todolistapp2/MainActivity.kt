package com.example.todolistapp2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolistapp2.adapter.TaskListAdapter
import com.example.todolistapp2.model.TaskList
import com.example.todolistapp2.viewmodel.TaskListViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskListAdapter
    private lateinit var switchTheme: SwitchMaterial
    private val viewModel: TaskListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Aplicar tema guardado ANTES de setContentView
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        setContentView(R.layout.activity_main)

        // Configurar switch de tema
        switchTheme = findViewById(R.id.switchTheme)
        switchTheme.isChecked = isDarkMode

        // Configurar listener del switch
        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            // Guardar preferencia
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply()

            // Cambiar tema
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            // Recrear activity para aplicar el tema inmediatamente
            recreate()
        }

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.recyclerViewLists)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = TaskListAdapter(
            onListClick = { taskList ->
                openTasksActivity(taskList)
            },
            onListLongClick = { taskList ->
                showListOptionsDialog(taskList)
            }
        )

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

    private fun showListOptionsDialog(taskList: TaskList) {
        val options = arrayOf("✏️ Editar nombre", "🗑️ Eliminar lista")

        AlertDialog.Builder(this)
            .setTitle(taskList.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditListDialog(taskList)
                    1 -> showDeleteListConfirmation(taskList)
                }
            }
            .show()
    }

    private fun showEditListDialog(taskList: TaskList) {
        val editText = EditText(this)
        editText.setText(taskList.name)
        editText.setSelection(taskList.name.length)

        AlertDialog.Builder(this)
            .setTitle("Editar nombre de lista")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = editText.text.toString()
                if (newName.isNotBlank()) {
                    val updatedList = taskList.copy(name = newName)
                    viewModel.updateList(updatedList)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteListConfirmation(taskList: TaskList) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar lista")
            .setMessage("¿Estás seguro de que quieres eliminar '${taskList.name}'? Todas las tareas de esta lista también se eliminarán.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteList(taskList)
            }
            .setNegativeButton("Cancelar", null)
            .show()
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