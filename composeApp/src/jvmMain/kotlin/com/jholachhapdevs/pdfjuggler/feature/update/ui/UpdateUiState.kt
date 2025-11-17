package com.jholachhapdevs.pdfjuggler.feature.update.ui

import com.jholachhapdevs.pdfjuggler.feature.update.domain.model.UpdateInfo

data class UpdateUiState(
    val loading: Boolean = false,
    val updateInfo: UpdateInfo? = null, // ? means it can be null
    val error: String? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Float? = null,
    val downloadedPath: String? = null,
    val showChangelog: Boolean = false,
    val downloadedBytes: Long = 0,
    val totalBytes: Long? = null,
    val downloadStartedAtMillis: Long? = null
)
