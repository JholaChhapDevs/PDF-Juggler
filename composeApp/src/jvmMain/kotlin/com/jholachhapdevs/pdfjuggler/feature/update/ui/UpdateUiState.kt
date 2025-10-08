package com.jholachhapdevs.pdfjuggler.feature.update.ui

import com.jholachhapdevs.pdfjuggler.feature.update.domain.model.UpdateInfo

data class UpdateUiState(
    val loading: Boolean = false,
    val updateInfo: UpdateInfo? = null, // ? means it can be null
    val error: String? = null
)