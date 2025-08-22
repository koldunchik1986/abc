package ru.neverlands.abclient.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import ru.neverlands.abclient.data.model.UserProfile
import ru.neverlands.abclient.ui.profile.components.ProfileEditScreen
import ru.neverlands.abclient.ui.profile.components.ProfileListScreen
import ru.neverlands.abclient.ui.theme.ABClientTheme

/**
 * Activity для управления профилями пользователей
 * Эквивалент FormProfiles из Windows версии
 */
@AndroidEntryPoint
class ProfileActivity : ComponentActivity() {
    
    companion object {
        private const val EXTRA_PROFILE_ID = "profile_id"
        
        fun createIntent(context: Context, profileId: String? = null): Intent {
            return Intent(context, ProfileActivity::class.java).apply {
                profileId?.let { putExtra(EXTRA_PROFILE_ID, it) }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
        
        setContent {
            ABClientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ProfileNavigationHost(
                        initialProfileId = profileId,
                        onFinish = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileNavigationHost(
    initialProfileId: String?,
    onFinish: () -> Unit
) {
    var currentScreen by remember { 
        mutableStateOf(
            if (initialProfileId != null) {
                ProfileScreen.Edit(initialProfileId)
            } else {
                ProfileScreen.List
            }
        )
    }
    
    when (val screen = currentScreen) {
        is ProfileScreen.List -> {
            ProfileListScreen(
                onProfileSelected = { profile ->
                    // Закрываем активность и возвращаемся в MainActivity
                    // В реальном приложении здесь можно было бы установить выбранный профиль
                    onFinish()
                },
                onEditProfile = { profileId ->
                    currentScreen = ProfileScreen.Edit(profileId)
                },
                onCreateProfile = {
                    currentScreen = ProfileScreen.Edit(null)
                },
                onBack = {
                    onFinish()
                }
            )
        }
        
        is ProfileScreen.Edit -> {
            ProfileEditScreen(
                profileId = screen.profileId,
                onSave = {
                    currentScreen = ProfileScreen.List
                },
                onCancel = {
                    if (initialProfileId != null) {
                        onFinish()
                    } else {
                        currentScreen = ProfileScreen.List
                    }
                }
            )
        }
    }
}

private sealed class ProfileScreen {
    object List : ProfileScreen()
    data class Edit(val profileId: String?) : ProfileScreen()
}