package com.example.todolistapp2.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.todolistapp2.model.Task
import com.example.todolistapp2.model.TaskList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TaskList::class, Task::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskListDao(): TaskListDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "todo_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database.taskListDao(), database.taskDao())
                    }
                }
            }
        }

        suspend fun populateDatabase(taskListDao: TaskListDao, taskDao: TaskDao) {
            // Crear listas de ejemplo
            val list1Id = taskListDao.insertList(
                TaskList(name = "Compras", color = 0xFF4CAF50.toInt())
            ).toInt()

            val list2Id = taskListDao.insertList(
                TaskList(name = "Trabajo", color = 0xFF2196F3.toInt())
            ).toInt()

            val list3Id = taskListDao.insertList(
                TaskList(name = "Personal", color = 0xFFE91E63.toInt())
            ).toInt()

            // Crear tareas para "Compras"
            taskDao.insertTask(Task(title = "Comprar leche", listId = list1Id))
            taskDao.insertTask(Task(title = "Pan integral", listId = list1Id))
            taskDao.insertTask(Task(title = "Frutas y verduras", listId = list1Id))

            // Crear tareas para "Trabajo"
            taskDao.insertTask(Task(title = "Revisar emails", isCompleted = true, listId = list2Id))
            taskDao.insertTask(Task(title = "Reunión con equipo", listId = list2Id))
            taskDao.insertTask(Task(title = "Entregar informe", listId = list2Id))

            // Crear tareas para "Personal"
            taskDao.insertTask(Task(title = "Llamar al médico", listId = list3Id))
            taskDao.insertTask(Task(title = "Hacer ejercicio", isCompleted = true, listId = list3Id))
            taskDao.insertTask(Task(title = "Leer 30 minutos", listId = list3Id))
        }
    }
}