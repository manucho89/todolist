package com.example.todolistapp2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolistapp2.adapter.TaskAdapter
import com.example.todolistapp2.model.SharedTask
import com.example.todolistapp2.model.SharedTaskList
import com.example.todolistapp2.model.Task
import com.example.todolistapp2.model.TaskList
import com.example.todolistapp2.utils.SwipeToDeleteCallback
import com.example.todolistapp2.viewmodel.TaskListViewModel
import com.example.todolistapp2.viewmodel.TaskViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import java.util.Locale

class TasksActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskAdapter
    private val taskViewModel: TaskViewModel by viewModels()
    private val listViewModel: TaskListViewModel by viewModels()
    private var listId: Int = -1
    private var listName: String = ""
    private var listColor: Int = 0
    private var currentTasks: List<Task> = emptyList()

    // Launcher para reconocimiento de voz
    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.get(0) ?: return@registerForActivityResult

            // Crear la tarea con el texto dictado
            if (spokenText.isNotBlank()) {
                val newTask = Task(
                    title = spokenText,
                    listId = listId
                )
                taskViewModel.insertTask(newTask)
                Toast.makeText(this, "Tarea creada: $spokenText", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks)

        // Verificar si es un deep link
        if (intent?.action == Intent.ACTION_VIEW) {
            handleDeepLink(intent)
            return
        }

        // Obtener datos de la lista desde el Intent
        listId = intent.getIntExtra("LIST_ID", -1)
        listName = intent.getStringExtra("LIST_NAME") ?: "Tareas"

        // Log para depuración
        Log.d("TasksActivity", "LIST_ID recibido: $listId")
        Log.d("TasksActivity", "LIST_NAME recibido: $listName")

        setupUI()
        setupSwipeToDelete()
        observeTasks()
    }

    private fun setupUI() {
        // Configurar Toolbar
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.title = listName
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_bold)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.recyclerViewTasks)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = TaskAdapter(
            onTaskChecked = { task ->
                taskViewModel.updateTask(task)
            },
            onTaskDelete = { task ->
                showDeleteConfirmation(task)
            }
        )

        recyclerView.adapter = adapter

        // Configurar botón flotante
        val fabAddTask: FloatingActionButton = findViewById(R.id.fabAddTask)
        fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val task = adapter.currentList[position]

                // Eliminar la tarea
                taskViewModel.deleteTask(task)

                // Mostrar Snackbar con opción de deshacer
                Snackbar.make(
                    recyclerView,
                    "Tarea eliminada",
                    Snackbar.LENGTH_LONG
                ).setAction("DESHACER") {
                    // Restaurar la tarea
                    taskViewModel.insertTask(task)
                }.show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun observeTasks() {
        if (listId != -1) {
            taskViewModel.getTasksForList(listId).observe(this) { tasks ->
                Log.d("TasksActivity", "Tareas recibidas: ${tasks.size}")
                currentTasks = tasks
                adapter.submitList(tasks)
            }
        } else {
            Log.e("TasksActivity", "ERROR: LIST_ID es -1, no se puede cargar tareas")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_tasks, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                shareList()
                true
            }
            R.id.action_delete_completed -> {
                deleteCompletedTasks()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deleteCompletedTasks() {
        val completedTasks = currentTasks.filter { it.isCompleted }

        if (completedTasks.isEmpty()) {
            Toast.makeText(this, "No hay tareas completadas para eliminar", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Eliminar tareas completadas")
            .setMessage("¿Estás seguro de que quieres eliminar ${completedTasks.size} tarea(s) completada(s)?")
            .setPositiveButton("Eliminar") { _, _ ->
                val deletedTasks = completedTasks.toList()

                completedTasks.forEach { task ->
                    taskViewModel.deleteTask(task)
                }

                Snackbar.make(
                    recyclerView,
                    "${completedTasks.size} tarea(s) eliminada(s)",
                    Snackbar.LENGTH_LONG
                ).setAction("DESHACER") {
                    deletedTasks.forEach { task ->
                        taskViewModel.insertTask(task)
                    }
                }.show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun shareList() {
        if (currentTasks.isEmpty()) {
            Toast.makeText(this, "No hay tareas para compartir", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedTasks = currentTasks.map { task ->
            SharedTask(task.title, task.isCompleted)
        }
        val sharedList = SharedTaskList(listName, listColor, sharedTasks)

        val gson = Gson()
        val json = gson.toJson(sharedList)
        val encodedData = Base64.encodeToString(json.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
        val shareLink = "https://manucho89.github.io/todolist/?data=$encodedData"

        val message = "📋 *${listName}*\n\n" +
                "Te comparto mi lista de tareas (${currentTasks.size} tareas)\n\n" +
                "Haz clic aquí para importarla en tu app:\n$shareLink"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            setPackage("com.whatsapp")
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            val genericIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            startActivity(Intent.createChooser(genericIntent, "Compartir lista"))
        }
    }

    private fun handleDeepLink(intent: Intent) {
        val data: Uri? = intent.data
        val encodedData = data?.getQueryParameter("data")

        if (encodedData != null) {
            try {
                val json = String(Base64.decode(encodedData, Base64.URL_SAFE))
                val gson = Gson()
                val sharedList = gson.fromJson(json, SharedTaskList::class.java)
                showImportDialog(sharedList)
            } catch (e: Exception) {
                Log.e("TasksActivity", "Error al procesar deep link", e)
                Toast.makeText(this, "Error al importar la lista", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            finish()
        }
    }

    private fun showImportDialog(sharedList: SharedTaskList) {
        AlertDialog.Builder(this)
            .setTitle("Importar Lista")
            .setMessage("¿Quieres importar la lista '${sharedList.listName}' con ${sharedList.tasks.size} tareas?")
            .setPositiveButton("Importar") { _, _ ->
                importList(sharedList)
            }
            .setNegativeButton("Cancelar") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun importList(sharedList: SharedTaskList) {
        val newList = TaskList(
            name = sharedList.listName,
            color = sharedList.listColor
        )

        listViewModel.insertListWithCallback(newList) { newListId ->
            sharedList.tasks.forEach { sharedTask ->
                val task = Task(
                    title = sharedTask.title,
                    isCompleted = sharedTask.isCompleted,
                    listId = newListId.toInt()
                )
                taskViewModel.insertTask(task)
            }

            runOnUiThread {
                Toast.makeText(this, "Lista importada correctamente", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
        }
    }

    private fun showAddTaskDialog() {
        // Crear un layout personalizado con EditText y botón de voz
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val editText = dialogView.findViewById<EditText>(R.id.editTextTaskTitle)
        val voiceButton = dialogView.findViewById<ImageButton>(R.id.buttonVoiceInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Nueva Tarea")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val taskTitle = editText.text.toString()
                if (taskTitle.isNotBlank()) {
                    val newTask = Task(
                        title = taskTitle,
                        listId = listId
                    )
                    Log.d("TasksActivity", "Creando tarea con listId: $listId")
                    taskViewModel.insertTask(newTask)
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        // Configurar botón de voz
        voiceButton.setOnClickListener {
            dialog.dismiss()
            startVoiceRecognitionForTask()
        }

        dialog.show()
    }

    private fun startVoiceRecognitionForTask() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di el nombre de la tarea")
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

    private fun showDeleteConfirmation(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar tarea")
            .setMessage("¿Estás seguro de que quieres eliminar '${task.title}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                taskViewModel.deleteTask(task)

                Snackbar.make(
                    recyclerView,
                    "Tarea eliminada",
                    Snackbar.LENGTH_LONG
                ).setAction("DESHACER") {
                    taskViewModel.insertTask(task)
                }.show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}