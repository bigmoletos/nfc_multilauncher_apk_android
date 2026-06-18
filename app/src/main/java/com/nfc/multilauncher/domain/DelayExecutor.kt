package com.nfc.multilauncher.domain

import kotlinx.coroutines.delay

object DelayExecutor {
    suspend fun wait(ms: Long) {
        if (ms > 0) delay(ms.coerceIn(0, 30_000))
    }
}
