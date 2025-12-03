package com.example.todolistapp2.database

import androidx.room.*
import com.example.todolistapp2.model.TaskList
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {

    @Query("SELECT * FROM task_lists ORDER BY id DESC")
    fun getAllLists(): Flow<List<TaskList>>

    @Query("SELECT * FROM task_lists WHERE id = :listId")
    suspend fun getListById(listId: Int): TaskList?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(taskList: TaskList): Long

    @Update
    suspend fun updateList(taskList: TaskList)

    @Delete
    suspend fun deleteList(taskList: TaskList)
}