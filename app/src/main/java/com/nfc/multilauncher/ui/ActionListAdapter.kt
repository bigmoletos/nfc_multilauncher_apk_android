package com.nfc.multilauncher.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nfc.multilauncher.data.model.Action
import com.nfc.multilauncher.data.model.ActionType
import com.nfc.multilauncher.databinding.ItemActionBinding

class ActionListAdapter(
    private val onDelete: (Action) -> Unit,
    private val onMoveUp: (Int) -> Unit,
    private val onMoveDown: (Int) -> Unit
) : ListAdapter<Action, ActionListAdapter.ActionViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
        val binding = ItemActionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ActionViewHolder(private val binding: ItemActionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(action: Action, position: Int) {
            binding.textActionType.text = action.type.name
            binding.textActionDetail.text = when (action.type) {
                ActionType.LAUNCH_APP -> action.packageName ?: "-"
                ActionType.LAUNCH_URL -> action.url ?: "-"
                ActionType.LAUNCH_MUSIC -> action.packageName ?: "Musique par défaut"
                ActionType.DELAY -> "${action.delayMs ?: 3000} ms"
            }
            binding.buttonDeleteAction.setOnClickListener { onDelete(action) }
            binding.buttonMoveUp.setOnClickListener { onMoveUp(position) }
            binding.buttonMoveDown.setOnClickListener { onMoveDown(position) }
            binding.buttonMoveUp.isEnabled = position > 0
            binding.buttonMoveDown.isEnabled = position < itemCount - 1
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Action>() {
            override fun areItemsTheSame(old: Action, new: Action) = old.id == new.id
            override fun areContentsTheSame(old: Action, new: Action) = old == new
        }
    }
}
