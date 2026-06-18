package com.nfc.multilauncher.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nfc.multilauncher.data.model.Task
import com.nfc.multilauncher.databinding.ItemTaskBinding

class TaskListAdapter(
    private val onEdit: (Task) -> Unit,
    private val onDelete: (Task) -> Unit,
    private val onProgram: (Task) -> Unit
) : ListAdapter<Task, TaskListAdapter.TaskViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.textTaskName.text = task.name
            binding.textActionCount.text = "${task.actions.size} action(s)"
            binding.buttonEdit.setOnClickListener { onEdit(task) }
            binding.buttonDelete.setOnClickListener { onDelete(task) }
            binding.buttonProgram.setOnClickListener { onProgram(task) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Task>() {
            override fun areItemsTheSame(old: Task, new: Task) = old.id == new.id
            override fun areContentsTheSame(old: Task, new: Task) = old == new
        }
    }
}
