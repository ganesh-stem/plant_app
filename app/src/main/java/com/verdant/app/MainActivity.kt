package com.verdant.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.verdant.app.data.knowledge.KnowledgeBase
import com.verdant.app.data.preferences.UserPreferences
import com.verdant.app.data.preferences.UserPreferencesRepository
import com.verdant.app.data.repository.ScanRepository
import com.verdant.app.ui.navigation.VerdantNavHost
import com.verdant.app.ui.theme.VerdantTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: UserPreferencesRepository,
    repository: ScanRepository,
    knowledgeBase: KnowledgeBase,
) : ViewModel() {
    /** Null until preferences load; the splash screen stays up until then. */
    val preferences: StateFlow<UserPreferences?> = preferences.preferences
        .map<UserPreferences, UserPreferences?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch { runCatching { repository.cleanUpOrphanImages() } }
        // Parse the knowledge base off the main thread before anything needs it.
        viewModelScope.launch(Dispatchers.Default) { runCatching { knowledgeBase.plants } }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splash.setKeepOnScreenCondition { viewModel.preferences.value == null }

        setContent {
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
            val current = prefs ?: return@setContent
            VerdantTheme(themeMode = current.themeMode, dynamicColor = current.dynamicColor) {
                VerdantNavHost(showOnboarding = !current.onboardingComplete)
            }
        }
    }
}
