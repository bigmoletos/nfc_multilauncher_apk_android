package com.nfc.multilauncher.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.nfc.multilauncher.data.model.Action
import com.nfc.multilauncher.data.model.ActionType
import com.nfc.multilauncher.databinding.DialogAddActionBinding

class AddActionDialog(
    private val onActionAdded: (Action) -> Unit
) : DialogFragment() {

    private var _binding: DialogAddActionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddActionBinding.inflate(LayoutInflater.from(requireContext()))

        val types = ActionType.values().map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerActionType.adapter = adapter

        binding.spinnerActionType.setOnItemSelectedListener(
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?, view: android.view.View?,
                    position: Int, id: Long
                ) {
                    updateVisibility(ActionType.values()[position])
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        )
        updateVisibility(ActionType.LAUNCH_APP)

        return AlertDialog.Builder(requireContext())
            .setTitle("Ajouter une action")
            .setView(binding.root)
            .setPositiveButton("Ajouter") { _, _ ->
                val type = ActionType.values()[binding.spinnerActionType.selectedItemPosition]
                val action = buildAction(type)
                action?.let { onActionAdded(it) }
            }
            .setNegativeButton("Annuler", null)
            .create()
    }

    private fun updateVisibility(type: ActionType) {
        binding.layoutPackage.visibility =
            if (type == ActionType.LAUNCH_APP || type == ActionType.LAUNCH_MUSIC)
                android.view.View.VISIBLE else android.view.View.GONE
        binding.layoutUrl.visibility =
            if (type == ActionType.LAUNCH_URL || type == ActionType.LAUNCH_MUSIC)
                android.view.View.VISIBLE else android.view.View.GONE
        binding.layoutDelay.visibility =
            if (type == ActionType.DELAY) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun buildAction(type: ActionType): Action? {
        return when (type) {
            ActionType.LAUNCH_APP -> {
                val pkg = binding.editPackage.text.toString().trim()
                if (pkg.isEmpty()) { binding.editPackage.error = "Obligatoire"; return null }
                Action(type = type, packageName = pkg)
            }
            ActionType.LAUNCH_URL -> {
                val url = binding.editUrl.text.toString().trim()
                if (url.isEmpty()) { binding.editUrl.error = "Obligatoire"; return null }
                if (!url.startsWith("http")) { binding.editUrl.error = "URL invalide"; return null }
                Action(type = type, url = url)
            }
            ActionType.LAUNCH_MUSIC -> {
                val pkg = binding.editPackage.text.toString().trim().ifEmpty { null }
                val url = binding.editUrl.text.toString().trim().ifEmpty { null }
                Action(type = type, packageName = pkg, url = url)
            }
            ActionType.DELAY -> {
                val ms = binding.editDelay.text.toString().toLongOrNull() ?: 3000L
                Action(type = type, delayMs = ms.coerceIn(100, 30_000))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
