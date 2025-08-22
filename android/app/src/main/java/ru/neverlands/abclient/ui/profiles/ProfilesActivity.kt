package ru.neverlands.abclient.ui.profiles

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ru.neverlands.abclient.ui.theme.ABClientTheme
import ru.neverlands.abclient.data.model.UserProfile
import java.text.SimpleDateFormat
import java.util.*

/**
 * Экран выбора профилей - полный аналог FormProfiles из Windows версии
 * Позволяет выбрать, создать или редактировать профили пользователей
 */
@AndroidEntryPoint
class ProfilesActivity : ComponentActivity() {
    
    companion object {
        const val RESULT_SELECTED_PROFILE = "selected_profile"
        
        fun createIntent(context: Context): Intent {
            return Intent(context, ProfilesActivity::class.java)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ABClientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ProfilesScreen(
                        onBack = { finish() },
                        onProfileSelected = { profile ->
                            val resultIntent = Intent().apply {
                                putExtra(RESULT_SELECTED_PROFILE, profile.userNick)
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    onBack: () -> Unit,
    onProfileSelected: (UserProfile) -> Unit,
    viewModel: ProfilesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadProfiles()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Выбор профиля") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.createNewProfile() }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Создать профиль")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.profiles.isEmpty()) {
                EmptyProfilesState(
                    onCreateNewProfile = { viewModel.createNewProfile() }
                )
            } else {
                // Список профилей
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.profiles) { profile ->
                        ProfileCard(
                            profile = profile,
                            isSelected = profile == uiState.selectedProfile,
                            onProfileSelected = { viewModel.selectProfile(profile) },
                            onEditProfile = { viewModel.editProfile(profile) },
                            onDeleteProfile = { viewModel.deleteProfile(profile) }
                        )
                    }
                }
                
                // Информация о выбранном профиле
                uiState.selectedProfile?.let { profile ->
                    SelectedProfileInfo(profile = profile)
                }
                
                // Кнопки действий
                ActionButtons(
                    selectedProfile = uiState.selectedProfile,
                    onUseProfile = { profile ->
                        onProfileSelected(profile)
                    },
                    onCreateNewProfile = { viewModel.createNewProfile() }
                )
            }
        }
    }
}

@Composable
private fun EmptyProfilesState(
    onCreateNewProfile: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Профили не найдены",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Создайте новый профиль для начала работы",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onCreateNewProfile,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Создать профиль")
        }
    }
}

@Composable
private fun ProfileCard(
    profile: UserProfile,
    isSelected: Boolean,
    onProfileSelected: () -> Unit,
    onEditProfile: () -> Unit,
    onDeleteProfile: () -> Unit
) {
    Card(
        onClick = onProfileSelected,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка профиля
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Информация о профиле
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = profile.userNick,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                if (profile.userNick.isNotEmpty()) {
                    Text(
                        text = "Логин: ${profile.userNick}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            // Кнопки действий
            Row {
                IconButton(onClick = onEditProfile) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Редактировать",
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                
                IconButton(onClick = onDeleteProfile) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedProfileInfo(
    profile: UserProfile
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Информация о профиле",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            // Последний заход - аналог ConstLastLogOn из Windows версии
            val lastLoginText = if (profile.lastLogon > 0) {
                val date = Date(profile.lastLogon)
                val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                "Последний заход в игру: ${formatter.format(date)}"
            } else {
                "Последний заход в игру: никогда"
            }
            
            Text(
                text = lastLoginText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (profile.userNick.isNotEmpty()) {
                Text(
                    text = "Игровой логин: ${profile.userNick}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (profile.mapLocation.isNotEmpty()) {
                Text(
                    text = "Текущая локация: ${profile.mapLocation}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    selectedProfile: UserProfile?,
    onUseProfile: (UserProfile) -> Unit,
    onCreateNewProfile: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onCreateNewProfile,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Создать")
        }
        
        Button(
            onClick = { selectedProfile?.let(onUseProfile) },
            enabled = selectedProfile != null,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Использовать")
        }
    }
}