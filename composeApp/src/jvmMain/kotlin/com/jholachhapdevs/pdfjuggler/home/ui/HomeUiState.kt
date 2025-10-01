package com.jholachhapdevs.pdfjuggler.home.ui

import com.jholachhapdevs.pdfjuggler.home.domain.model.UpdateInfo

data class HomeUiState(
    val loading: Boolean = false,
    val updateInfo: UpdateInfo? = null, // ? means it can be null
    val error: String? = null
)