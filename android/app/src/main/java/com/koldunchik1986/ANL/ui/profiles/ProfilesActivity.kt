package com.koldunchik1986.ANL.ui.profiles

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
import com.koldunchik1986.ANL.ui.theme.ABClientTheme
import com.koldunchik1986.ANL.data.model.UserProfile
import java.text.SimpleDateFormat
import java.util.*

/**
 * Экран управления профилями - аналог FormProfiles из Windows клиента
 * Отображает список, позволяет создавать и редактировать профили пользователей
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
    
    // Если редактируем профиль, показываем экран редактирования
    uiState.editingProfile?.let { profile ->
        ProfileEditScreen(
            profile = profile,
            onSave = { updatedProfile ->
                viewModel.saveEditedProfile(updatedProfile)
            },
            onCancel = {
                viewModel.cancelEditing()
            }
        )
        return@ProfilesScreen
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Управление профилями") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditScreen(
    profile: UserProfile,
    onSave: (UserProfile) -> Unit,
    onCancel: () -> Unit
) {
    var nick by remember { mutableStateOf(profile.userNick) }
    var password by remember { mutableStateOf(profile.userPassword) }
    var autoLogon by remember { mutableStateOf(profile.userAutoLogon) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактирование профиля") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
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
            OutlinedTextField(
                value = nick,
                onValueChange = { nick = it },
                label = { Text("Ник персонажа") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = autoLogon,
                    onCheckedChange = { autoLogon = it }
                )
                Text("Автоматический вход")
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Отмена")
                }
                
                Button(
                    onClick = {
                        val updatedProfile = profile.copy(
                            userNick = nick,
                            userPassword = password,
                            userAutoLogon = autoLogon
                        )
                        onSave(updatedProfile)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Сохранить")
                }
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
            text = "Профили отсутствуют",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Добавьте новый профиль для начала работы",
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
            
            // Ник и данные
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
                        text = "Ник: ${profile.userNick}",
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
            
            // Дата последнего входа - аналог ConstLastLogOn из Windows клиента
            val lastLoginText = if (profile.lastLogon > 0) {
                val date = Date(profile.lastLogon)
                val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                "Последний вход в игру: ${formatter.format(date)}"
            } else {
                "Последний вход в игру: Никогда"
            }
            
            Text(
                text = lastLoginText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (profile.userNick.isNotEmpty()) {
                Text(
                    text = "Имя персонажа: ${profile.userNick}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (profile.mapLocation.isNotEmpty()) {
                Text(
                    text = "Текущее местоположение: ${profile.mapLocation}",
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