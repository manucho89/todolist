package com.example.todolistapp2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolistapp2.adapter.TaskAdapter
import com.example.todolistapp2.model.SharedTask
import com.example.todolistapp2.model.SharedTaskList
import com.example.todolistapp2.model.Task
import com.example.todolistapp2.model.TaskList
import com.example.todolistapp2.viewmodel.TaskListViewModel
import com.example.todolistapp2.viewmodel.TaskViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson

class TasksActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskAdapter
    private val taskViewModel: TaskViewModel by viewModels()
    private val listViewModel: TaskListViewModel by viewModels()
    private var listId: Int = -1
    private var listName: String = ""
    private var listColor: Int = 0
    private var currentTasks: List<Task> = emptyList()

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
        observeTasks()
    }

    private fun setupUI() {
        // Configurar Toolbar
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.title = listName
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareList() {
        if (currentTasks.isEmpty()) {
            Toast.makeText(this, "No hay tareas para compartir", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear objeto compartible
        val sharedTasks = currentTasks.map { task ->
            SharedTask(task.title, task.isCompleted)
        }
        val sharedList = SharedTaskList(listName, listColor, sharedTasks)

        // Convertir a JSON
        val gson = Gson()
        val json = gson.toJson(sharedList)

        // Codificar en Base64
        val encodedData = Base64.encodeToString(json.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)

        // Crear enlace de GitHub Pages (HTTPS clickeable)
        val shareLink = "https://manucho89.github.io/todolist/?data=$encodedData"

        // Crear mensaje para WhatsApp
        val message = "📋 *${listName}*\n\n" +
                "Te comparto mi lista de tareas (${currentTasks.size} tareas)\n\n" +
                "Haz clic aquí para importarla en tu app:\n$shareLink"

        // Compartir por WhatsApp
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            setPackage("com.whatsapp")
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Si WhatsApp no está instalado, usar compartir genérico
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
                // Decodificar Base64
                val json = String(Base64.decode(encodedData, Base64.URL_SAFE))

                // Parsear JSON
                val gson = Gson()
                val sharedList = gson.fromJson(json, SharedTaskList::class.java)

                // Mostrar diálogo de confirmación
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
        // Crear nueva lista
        val newList = TaskList(
            name = sharedList.listName,
            color = sharedList.listColor
        )

        // Insertar lista y luego las tareas
        listViewModel.insertListWithCallback(newList) { newListId ->
            // Insertar tareas
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

                // Abrir MainActivity para ver la lista importada
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
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
                    Log.d("TasksActivity", "Creando tarea con listId: $listId")
                    taskViewModel.insertTask(newTask)
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
                taskViewModel.deleteTask(task)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}