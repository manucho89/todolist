package com.example.todolistapp2.database

import androidx.room.*
import com.example.todolistapp2.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE listId = :listId ORDER BY id DESC")
    fun getTasksForList(listId: Int): Flow<List<Task>>

    @Query("SELECT COUNT(*) FROM tasks WHERE listId = :listId")
    suspend fun getTaskCountForList(listId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)
}