package com.nfc.multilauncher.domain

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.nfc.multilauncher.data.model.Action
import com.nfc.multilauncher.data.model.ActionType

sealed class ActionResult {
    object Success : ActionResult()
    data class Error(val message: String) : ActionResult()
    object Skipped : ActionResult()
}

class ActionExecutor(private val context: Context) {

    fun execute(action: Action): ActionResult = when (action.type) {
        ActionType.LAUNCH_APP -> launchApp(action.packageName)
        ActionType.LAUNCH_URL -> launchUrl(action.url)
        ActionType.LAUNCH_MUSIC -> launchMusic(action.packageName, action.url)
        ActionType.DELAY -> ActionResult.Skipped // géré par TaskExecutor
    }

    private fun launchApp(packageName: String?): ActionResult {
        if (packageName.isNullOrBlank()) return ActionResult.Error("Package name vide")

        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
            ?: return ActionResult.Error("Application non trouvée: $packageName")

        return try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            ActionResult.Success
        } catch (e: Exception) {
            ActionResult.Error("Impossible de lancer $packageName: ${e.message}")
        }
    }

    private fun launchUrl(url: String?): ActionResult {
        if (url.isNullOrBlank()) return ActionResult.Error("URL vide")

        val uri = runCatching { Uri.parse(url) }.getOrNull()
            ?: return ActionResult.Error("URL invalide: $url")

        if (uri.scheme != "https" && uri.scheme != "http") {
            return ActionResult.Error("Seules les URLs http/https sont autorisées")
        }

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ActionResult.Success
            } else {
                ActionResult.Error("Aucune application pour ouvrir: $url")
            }
        } catch (e: Exception) {
            ActionResult.Error("Erreur ouverture URL: ${e.message}")
        }
    }

    private fun launchMusic(packageName: String?, playlistUrl: String?): ActionResult {
        // Si un package spécifique est fourni, le lancer directement
        if (!packageName.isNullOrBlank()) {
            val result = launchApp(packageName)
            if (result is ActionResult.Success && !playlistUrl.isNullOrBlank()) {
                // Essayer d'ouvrir la playlist via URI
                runCatching { launchUrl(playlistUrl) }
            }
            return result
        }

        // Fallback : intent musique générique
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MUSIC)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ActionResult.Success
            } else {
                ActionResult.Error("Aucune application musique trouvée")
            }
        } catch (e: Exception) {
            ActionResult.Error("Erreur lancement musique: ${e.message}")
        }
    }

    fun isAppInstalled(packageName: String): Boolean =
        runCatching {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            true
        }.getOrDefault(false)

    fun getInstalledApps(): List<Pair<String, String>> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { it.packageName to (it.loadLabel(pm).toString()) }
            .sortedBy { it.second }
    }
}
