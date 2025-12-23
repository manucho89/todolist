package com.example.todolistapp2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolistapp2.adapter.TaskListAdapter
import com.example.todolistapp2.model.TaskList
import com.example.todolistapp2.utils.SwipeToDeleteCallback
import com.example.todolistapp2.viewmodel.TaskListViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Locale
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskListAdapter
    private lateinit var switchTheme: SwitchMaterial
    private val viewModel: TaskListViewModel by viewModels()

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.get(0) ?: return@registerForActivityResult

            if (spokenText.isNotBlank()) {
                val newList = TaskList(
                    name = spokenText,
                    color = getRandomColor()
                )
                viewModel.insertList(newList)
                Toast.makeText(this, "Lista creada: $spokenText", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        setContentView(R.layout.activity_main)

        switchTheme = findViewById(R.id.switchTheme)
        switchTheme.isChecked = isDarkMode

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply()

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            recreate()
        }

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

        setupSwipeToDelete()

        viewModel.allListsWithCount.observe(this) { listsWithCount ->
            adapter.submitList(listsWithCount)
        }

        val fabAddList: FloatingActionButton = findViewById(R.id.fabAddList)
        fabAddList.setOnClickListener {
            showAddListDialog()
        }
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val listWithCount = adapter.currentList[position]

                // ⭐ No permitir eliminar la entrada especial
                if (listWithCount.isSpecialImportant) {
                    adapter.notifyItemChanged(position)
                    Toast.makeText(this@MainActivity, "No se puede eliminar esta lista especial", Toast.LENGTH_SHORT).show()
                    return
                }

                val taskList = listWithCount.taskList ?: return

                val deletedList = taskList

                viewModel.deleteList(taskList)

                val taskCount = listWithCount.taskCount
                val message = if (taskCount > 0) {
                    "Lista \"${taskList.name}\" eliminada ($taskCount tarea(s))"
                } else {
                    "Lista \"${taskList.name}\" eliminada"
                }

                Snackbar.make(
                    recyclerView,
                    message,
                    Snackbar.LENGTH_LONG
                ).setAction("DESHACER") {
                    viewModel.insertList(deletedList)
                }.show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)
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

                Snackbar.make(
                    recyclerView,
                    "Lista \"${taskList.name}\" eliminada",
                    Snackbar.LENGTH_LONG
                ).setAction("DESHACER") {
                    viewModel.insertList(taskList)
                }.show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAddListDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_list, null)
        val editText = dialogView.findViewById<EditText>(R.id.editTextListName)
        val voiceButton = dialogView.findViewById<ImageButton>(R.id.buttonVoiceInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Nueva Lista")
            .setView(dialogView)
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
            .create()

        voiceButton.setOnClickListener {
            dialog.dismiss()
            startVoiceRecognitionForList()
        }

        dialog.show()
    }

    private fun startVoiceRecognitionForList() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di el nombre de la lista")
        }

        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Tu dispositivo no soporta reconocimiento de voz",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getRandomColor(): Int {
        val colors = listOf(
            0xFFE91E63.toInt(),
            0xFF9C27B0.toInt(),
            0xFF673AB7.toInt(),
            0xFF3F51B5.toInt(),
            0xFF2196F3.toInt(),
            0xFF009688.toInt(),
            0xFF4CAF50.toInt(),
            0xFFFF9800.toInt(),
            0xFFFF5722.toInt()
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