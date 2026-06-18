package com.nfc.multilauncher.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nfc.multilauncher.NfcApp
import com.nfc.multilauncher.data.nfc.NfcReader
import com.nfc.multilauncher.data.nfc.ReadResult
import com.nfc.multilauncher.databinding.ActivityMainBinding
import com.nfc.multilauncher.service.TaskExecutionService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: TaskViewModel by viewModels {
        TaskViewModel.Factory((application as NfcApp).repository)
    }
    private lateinit var adapter: TaskListAdapter
    private val nfcReader = NfcReader()
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC non disponible sur cet appareil", Toast.LENGTH_LONG).show()
        }

        setupRecyclerView()
        observeTasks()

        binding.fabCreateTask.setOnClickListener {
            startActivity(Intent(this, CreateTaskActivity::class.java))
        }

        requestNotificationPermission()
        handleNfcIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent) {
        val action = intent.action ?: return
        if (action != NfcAdapter.ACTION_NDEF_DISCOVERED &&
            action != NfcAdapter.ACTION_TAG_DISCOVERED &&
            action != NfcAdapter.ACTION_TECH_DISCOVERED) return

        when (val result = nfcReader.readFromIntent(intent)) {
            is ReadResult.Success -> {
                Toast.makeText(this, "Tag NFC lu : ${result.task.name}", Toast.LENGTH_SHORT).show()
                TaskExecutionService.startFor(this, result.task)
            }
            is ReadResult.NoNdefData -> {
                Toast.makeText(this, "Tag NFC sans données reconnues", Toast.LENGTH_SHORT).show()
            }
            is ReadResult.InvalidFormat -> {
                Toast.makeText(this, "Format de tag non reconnu", Toast.LENGTH_SHORT).show()
            }
            is ReadResult.Error -> {
                Toast.makeText(this, "Erreur : ${result.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = TaskListAdapter(
            onEdit = { task ->
                val intent = Intent(this, CreateTaskActivity::class.java).apply {
                    putExtra(CreateTaskActivity.EXTRA_TASK_ID, task.id)
                }
                startActivity(intent)
            },
            onDelete = { task ->
                AlertDialog.Builder(this)
                    .setTitle("Supprimer la tâche")
                    .setMessage("Supprimer « ${task.name} » ?")
                    .setPositiveButton("Supprimer") { _, _ -> viewModel.deleteTask(task) }
                    .setNegativeButton("Annuler", null)
                    .show()
            },
            onProgram = { task ->
                if (nfcAdapter == null) {
                    Toast.makeText(this, "NFC non disponible", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(this, ProgramTagActivity::class.java).apply {
                        putExtra(ProgramTagActivity.EXTRA_TASK_ID, task.id)
                    }
                    startActivity(intent)
                }
            }
        )
        binding.recyclerTasks.adapter = adapter
    }

    private fun observeTasks() {
        lifecycleScope.launch {
            viewModel.tasks.collect { tasks ->
                adapter.submitList(tasks)
                binding.textEmpty.visibility =
                    if (tasks.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100
            )
        }
    }
}
