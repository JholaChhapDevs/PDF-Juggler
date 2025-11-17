package com.jholachhapdevs.pdfjuggler.feature.update.ui

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import com.jholachhapdevs.pdfjuggler.feature.update.data.repository.UpdateRepository
import com.jholachhapdevs.pdfjuggler.feature.update.domain.usecase.GetUpdatesUseCase
import com.jholachhapdevs.pdfjuggler.core.util.Env

object UpdateScreen : Screen {
    private fun readResolve(): Any = UpdateScreen

    @Composable
    override fun Content() {

        // correct lifecycle-aware screenModel
        val screenModel = rememberScreenModel {
            UpdateScreenModel(GetUpdatesUseCase(UpdateRepository()))
        }

        UpdateHomeBanner(
            modifier = androidx.compose.ui.Modifier,
            currentVersionCode = Env.APP_VERSION_CODE,
            onClose = {},
            screenModel = screenModel
        )
    }
}
