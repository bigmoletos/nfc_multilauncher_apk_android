package com.nfc.multilauncher.data.model

import java.util.UUID

data class Action(
    val id: String = UUID.randomUUID().toString(),
    val type: ActionType,
    val packageName: String? = null,
    val url: String? = null,
    val delayMs: Long? = null,
    val order: Int = 0
)
