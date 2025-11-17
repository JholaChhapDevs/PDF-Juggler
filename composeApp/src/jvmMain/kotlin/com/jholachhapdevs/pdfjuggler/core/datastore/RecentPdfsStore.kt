package com.jholachhapdevs.pdfjuggler.core.datastore

import java.io.File

object RecentPdfsStore {
    private const val KEY = "recent_pdfs"
    private const val MAX = 5

    suspend fun getRecentPaths(): List<String> {
        val raw = PrefsManager.getString(KEY) ?: return emptyList()
        return raw.split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX)
            .filter { File(it).exists() }
    }

    suspend fun addRecentPath(path: String) {
        val current = getRecentPaths().toMutableList()
        current.removeAll { it.equals(path, ignoreCase = false) }
        current.add(0, path)
        val trimmed = current.take(MAX)
        PrefsManager.saveString(KEY, trimmed.joinToString("\n"))
    }

    suspend fun clear() {
        PrefsManager.delete(KEY)
    }
}