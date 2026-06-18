package com.nfc.multilauncher.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nfc.multilauncher.data.model.Action
import com.nfc.multilauncher.data.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepository(private val dao: TaskDao) {

    private val gson = Gson()
    private val actionListType = object : TypeToken<List<Action>>() {}.type

    val allTasksFlow: Flow<List<Task>> = dao.getAllTasksFlow().map { entities ->
        entities.map { it.toTask(deserializeActions(it.actionsJson)) }
    }

    suspend fun getAllTasks(): List<Task> =
        dao.getAllTasks().map { it.toTask(deserializeActions(it.actionsJson)) }

    suspend fun getTaskById(id: String): Task? =
        dao.getTaskById(id)?.let { it.toTask(deserializeActions(it.actionsJson)) }

    suspend fun saveTask(task: Task) {
        dao.insertTask(TaskEntity.fromTask(task, serializeActions(task.actions)))
    }

    suspend fun updateTask(task: Task) {
        val entity = TaskEntity.fromTask(
            task.copy(updatedAt = System.currentTimeMillis()),
            serializeActions(task.actions)
        )
        dao.insertTask(entity)
    }

    suspend fun deleteTask(task: Task) {
        dao.deleteTaskById(task.id)
    }

    private fun serializeActions(actions: List<Action>): String = gson.toJson(actions)

    private fun deserializeActions(json: String): List<Action> =
        if (json.isEmpty()) emptyList()
        else gson.fromJson(json, actionListType) ?: emptyList()
}
