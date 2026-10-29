package com.verdant.app.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Onboarding : Route
    @Serializable data object Home : Route
    @Serializable data object History : Route
    @Serializable data object Settings : Route
    @Serializable data object Camera : Route
    @Serializable data class Analysis(val imageUri: String) : Route
    @Serializable data class Result(val scanId: Long) : Route
}
