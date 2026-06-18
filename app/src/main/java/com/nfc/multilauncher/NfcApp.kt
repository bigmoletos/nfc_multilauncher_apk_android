package com.nfc.multilauncher

import android.app.Application
import com.nfc.multilauncher.data.local.AppDatabase
import com.nfc.multilauncher.data.local.TaskRepository

class NfcApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { TaskRepository(database.taskDao()) }
}
