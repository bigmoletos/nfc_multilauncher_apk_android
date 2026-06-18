package com.nfc.multilauncher.ui

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nfc.multilauncher.NfcApp
import com.nfc.multilauncher.data.model.Task
import com.nfc.multilauncher.data.nfc.NfcWriter
import com.nfc.multilauncher.data.nfc.WriteResult
import com.nfc.multilauncher.databinding.ActivityProgramTagBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProgramTagActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProgramTagBinding
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var task: Task? = null
    private val writer = NfcWriter()
    private var isWriting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgramTagBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Programmer un tag NFC"

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            binding.textStatus.text = "NFC non disponible sur cet appareil"
            binding.buttonCancel.setOnClickListener { finish() }
            return
        }

        if (!nfcAdapter!!.isEnabled) {
            binding.textStatus.text = "NFC désactivé. Activez-le dans les paramètres."
            binding.buttonCancel.setOnClickListener { finish() }
            return
        }

        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: run {
            finish(); return
        }

        lifecycleScope.launch {
            task = (application as NfcApp).repository.getTaskById(taskId)
            task?.let {
                val sizeBytes = writer.estimateJsonSize(it)
                binding.textTaskInfo.text = "Tâche : ${it.name} (${sizeBytes} octets)"
                binding.textStatus.text = "Approchez un tag NFC NTAG213 ou supérieur"
            } ?: run {
                binding.textStatus.text = "Tâche introuvable"
            }
        }

        binding.buttonCancel.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(
            this, pendingIntent,
            arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)),
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (isWriting) return
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
        val currentTask = task ?: return
        writeToTag(tag, currentTask)
    }

    private fun writeToTag(tag: Tag, task: Task) {
        isWriting = true
        binding.textStatus.text = "Écriture en cours..."
        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { writer.writeTask(tag, task) }
            binding.progressBar.visibility = android.view.View.GONE
            isWriting = false

            when (result) {
                is WriteResult.Success -> {
                    binding.textStatus.text = "Tag programmé avec succès !"
                    Toast.makeText(this@ProgramTagActivity, "Succès", Toast.LENGTH_SHORT).show()
                    android.os.Handler(mainLooper).postDelayed({ finish() }, 2000)
                }
                is WriteResult.TagTooSmall -> {
                    binding.textStatus.text = "Tag trop petit. Utilisez un NTAG215 ou NTAG216."
                }
                is WriteResult.NotWritable -> {
                    binding.textStatus.text = "Tag protégé en écriture."
                }
                is WriteResult.Error -> {
                    binding.textStatus.text = "Erreur : ${result.message}"
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
