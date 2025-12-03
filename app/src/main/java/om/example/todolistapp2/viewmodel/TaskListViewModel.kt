package com.example.todolistapp2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.todolistapp2.database.AppDatabase
import com.example.todolistapp2.model.TaskList
import com.example.todolistapp2.model.TaskListWithCount
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TaskListViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val taskListDao = database.taskListDao()
    private val taskDao = database.taskDao()

    val allListsWithCount: LiveData<List<TaskListWithCount>> = taskListDao.getAllLists()
        .map { lists ->
            lists.map { list ->
                val count = taskDao.getTaskCountForList(list.id)
                TaskListWithCount(list, count)
            }
        }
        .asLiveData()

    fun insertList(taskList: TaskList) {
        viewModelScope.launch {
            taskListDao.insertList(taskList)
        }
    }

    fun insertListWithCallback(taskList: TaskList, callback: (Long) -> Unit) {
        viewModelScope.launch {
            val newListId = taskListDao.insertList(taskList)
            callback(newListId)
        }
    }

    fun updateList(taskList: TaskList) {
        viewModelScope.launch {
            taskListDao.updateList(taskList)
        }
    }

    fun deleteList(taskList: TaskList) {
        viewModelScope.launch {
            taskListDao.deleteList(taskList)
        }
    }
}