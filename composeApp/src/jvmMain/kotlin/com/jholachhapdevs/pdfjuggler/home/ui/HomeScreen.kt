package com.jholachhapdevs.pdfjuggler.home.ui

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import com.jholachhapdevs.pdfjuggler.home.data.repository.HomeRepository
import com.jholachhapdevs.pdfjuggler.home.domain.usecase.GetUpdatesUseCase

object HomeScreen : Screen {
    private fun readResolve(): Any = HomeScreen

    @Composable
    override fun Content() {


        val screenModel = rememberScreenModel {
            HomeScreenModel(GetUpdatesUseCase(HomeRepository()))
        }
        HomeComponent(screenModel)
    }
}