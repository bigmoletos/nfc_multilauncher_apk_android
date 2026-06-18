package com.nfc.multilauncher.data.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.google.gson.Gson
import com.nfc.multilauncher.data.model.Task

sealed class WriteResult {
    object Success : WriteResult()
    data class Error(val message: String) : WriteResult()
    object TagTooSmall : WriteResult()
    object NotWritable : WriteResult()
}

class NfcWriter {

    private val gson = Gson()

    fun writeTask(tag: Tag, task: Task): WriteResult {
        val tagData = NfcTagData(
            taskId = task.id,
            taskName = task.name,
            actions = task.actions
        )
        val json = gson.toJson(tagData)
        val payload = json.toByteArray(Charsets.UTF_8)

        val mimeType = "application/com.nfc.multilauncher"
        val record = NdefRecord.createMime(mimeType, payload)
        val message = NdefMessage(arrayOf(record))

        return tryWriteNdef(tag, message) ?: tryFormatAndWrite(tag, message)
            ?: WriteResult.Error("Tag non supporté")
    }

    private fun tryWriteNdef(tag: Tag, message: NdefMessage): WriteResult? {
        val ndef = Ndef.get(tag) ?: return null
        return try {
            ndef.connect()
            if (!ndef.isWritable) return WriteResult.NotWritable
            if (ndef.maxSize < message.toByteArray().size) return WriteResult.TagTooSmall
            ndef.writeNdefMessage(message)
            WriteResult.Success
        } catch (e: Exception) {
            WriteResult.Error(e.message ?: "Erreur écriture NDEF")
        } finally {
            runCatching { ndef.close() }
        }
    }

    private fun tryFormatAndWrite(tag: Tag, message: NdefMessage): WriteResult? {
        val formatable = NdefFormatable.get(tag) ?: return null
        return try {
            formatable.connect()
            formatable.format(message)
            WriteResult.Success
        } catch (e: Exception) {
            WriteResult.Error(e.message ?: "Erreur formatage tag")
        } finally {
            runCatching { formatable.close() }
        }
    }

    fun estimateJsonSize(task: Task): Int {
        val tagData = NfcTagData(task.id, task.name, task.actions)
        return gson.toJson(tagData).toByteArray(Charsets.UTF_8).size
    }
}
