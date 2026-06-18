package com.nfc.multilauncher.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nfc.multilauncher.data.model.Action
import com.nfc.multilauncher.data.model.Task

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val name: String,
    val actionsJson: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toTask(actions: List<Action>): Task = Task(
        id = id,
        name = name,
        actions = actions,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromTask(task: Task, actionsJson: String): TaskEntity = TaskEntity(
            id = task.id,
            name = task.name,
            actionsJson = actionsJson,
            createdAt = task.createdAt,
            updatedAt = task.updatedAt
        )
    }
}
