package com.nfc.multilauncher.data.nfc

import com.nfc.multilauncher.data.model.Action
import com.nfc.multilauncher.data.model.Task

data class NfcTagData(
    val taskId: String,
    val taskName: String,
    val actions: List<Action>
) {
    fun toTask(): Task = Task(
        id = taskId,
        name = taskName,
        actions = actions
    )
}
