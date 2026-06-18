package com.nfc.multilauncher.domain

import android.content.Context
import com.nfc.multilauncher.data.model.Action
import com.nfc.multilauncher.data.model.ActionType
import com.nfc.multilauncher.data.model.Task

data class ExecutionProgress(
    val currentIndex: Int,
    val total: Int,
    val currentAction: Action,
    val result: ActionResult?
)

class TaskExecutor(private val context: Context) {

    private val actionExecutor = ActionExecutor(context)

    suspend fun executeSequentially(
        task: Task,
        onProgress: ((ExecutionProgress) -> Unit)? = null
    ): List<ActionResult> {
        val sortedActions = task.actions.sortedBy { it.order }
        val results = mutableListOf<ActionResult>()

        sortedActions.forEachIndexed { index, action ->
            onProgress?.invoke(
                ExecutionProgress(index, sortedActions.size, action, null)
            )

            val result = when (action.type) {
                ActionType.DELAY -> {
                    DelayExecutor.wait(action.delayMs ?: 3000L)
                    ActionResult.Skipped
                }
                else -> actionExecutor.execute(action)
            }

            results.add(result)
            onProgress?.invoke(
                ExecutionProgress(index, sortedActions.size, action, result)
            )
        }

        return results
    }
}
