package com.nfc.multilauncher.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nfc.multilauncher.NfcApp
import com.nfc.multilauncher.data.model.Action
import com.nfc.multilauncher.data.model.Task
import com.nfc.multilauncher.databinding.ActivityCreateTaskBinding
import kotlinx.coroutines.launch
import java.util.UUID

class CreateTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateTaskBinding
    private val viewModel: TaskViewModel by viewModels {
        TaskViewModel.Factory((application as NfcApp).repository)
    }
    private lateinit var actionAdapter: ActionListAdapter
    private val actions = mutableListOf<Action>()
    private var editTaskId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        editTaskId = intent.getStringExtra(EXTRA_TASK_ID)

        setupActionList()
        loadExistingTask()

        binding.buttonAddAction.setOnClickListener {
            AddActionDialog { action ->
                val ordered = action.copy(order = actions.size)
                actions.add(ordered)
                refreshActions()
            }.show(supportFragmentManager, "add_action")
        }

        binding.buttonSave.setOnClickListener { saveTask() }

        binding.buttonProgramNfc.setOnClickListener {
            if (validateInput()) {
                saveAndProgram()
            }
        }
    }

    private fun setupActionList() {
        actionAdapter = ActionListAdapter(
            onDelete = { action ->
                actions.remove(action)
                reorderActions()
                refreshActions()
            },
            onMoveUp = { position ->
                if (position > 0) {
                    val tmp = actions[position]
                    actions[position] = actions[position - 1]
                    actions[position - 1] = tmp
                    reorderActions()
                    refreshActions()
                }
            },
            onMoveDown = { position ->
                if (position < actions.size - 1) {
                    val tmp = actions[position]
                    actions[position] = actions[position + 1]
                    actions[position + 1] = tmp
                    reorderActions()
                    refreshActions()
                }
            }
        )
        binding.recyclerActions.adapter = actionAdapter
    }

    private fun loadExistingTask() {
        val taskId = editTaskId ?: return
        lifecycleScope.launch {
            val task = (application as NfcApp).repository.getTaskById(taskId) ?: return@launch
            binding.editTaskName.setText(task.name)
            actions.clear()
            actions.addAll(task.actions.sortedBy { it.order })
            refreshActions()
        }
    }

    private fun refreshActions() {
        actionAdapter.submitList(actions.toList())
    }

    private fun reorderActions() {
        actions.forEachIndexed { index, action ->
            actions[index] = action.copy(order = index)
        }
    }

    private fun validateInput(): Boolean {
        val name = binding.editTaskName.text.toString().trim()
        if (name.isEmpty()) {
            binding.editTaskName.error = "Nom de tâche obligatoire"
            return false
        }
        if (actions.isEmpty()) {
            Toast.makeText(this, "Ajoutez au moins une action", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun buildTask(): Task {
        val name = binding.editTaskName.text.toString().trim()
        reorderActions()
        return Task(
            id = editTaskId ?: UUID.randomUUID().toString(),
            name = name,
            actions = actions.toList()
        )
    }

    private fun saveTask() {
        if (!validateInput()) return
        val task = buildTask()
        if (editTaskId != null) viewModel.updateTask(task) else viewModel.saveTask(task)
        Toast.makeText(this, "Tâche sauvegardée", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun saveAndProgram() {
        if (!validateInput()) return
        val task = buildTask()
        editTaskId = task.id
        if (editTaskId != null) viewModel.updateTask(task) else viewModel.saveTask(task)
        val intent = Intent(this, ProgramTagActivity::class.java).apply {
            putExtra(ProgramTagActivity.EXTRA_TASK_ID, task.id)
        }
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
