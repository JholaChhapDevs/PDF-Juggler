package com.jholachhapdevs.pdfjuggler.feature.update.ui

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import com.jholachhapdevs.pdfjuggler.feature.update.data.repository.UpdateRepository
import com.jholachhapdevs.pdfjuggler.feature.update.domain.usecase.GetUpdatesUseCase

object UpdateScreen : Screen {
    private fun readResolve(): Any = UpdateScreen

    @Composable
    override fun Content() {

        val screenModel = rememberScreenModel {
            UpdateScreenModel(GetUpdatesUseCase(UpdateRepository()))
        }
        UpdateComponent(screenModel)
    }
}