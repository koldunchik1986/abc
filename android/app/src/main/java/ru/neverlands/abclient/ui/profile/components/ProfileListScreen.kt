package ru.neverlands.abclient.ui.profile.components

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.neverlands.abclient.data.model.UserProfile
import ru.neverlands.abclient.ui.profile.viewmodel.ProfileListViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Экран выбора и управления профилями пользователей
 * Эквивалент FormProfiles из Windows версии
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    onProfileSelected: (UserProfile) -> Unit,
    onEditProfile: (String) -> Unit,
    onCreateProfile: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var showDeleteDialog by remember { mutableStateOf<UserProfile?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профили пользователей") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onCreateProfile) {
                        Icon(Icons.Default.Add, contentDescription = "Создать профиль")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateProfile
            ) {
                Icon(Icons.Default.Add, contentDescription = "Создать профиль")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                uiState.profiles.isEmpty() -> {
                    EmptyProfilesState(
                        onCreateProfile = onCreateProfile,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                else -> {
                    ProfileList(
                        profiles = uiState.profiles,
                        currentProfileId = uiState.currentProfileId,
                        onProfileClick = onProfileSelected,
                        onEditClick = onEditProfile,
                        onDeleteClick = { profile ->
                            showDeleteDialog = profile
                        },
                        onSetCurrentClick = { profile ->
                            viewModel.setCurrentProfile(profile.id)
                        }
                    )
                }
            }
        }
    }
    
    // Диалог подтверждения удаления
    showDeleteDialog?.let { profile ->
        DeleteProfileDialog(
            profile = profile,
            onConfirm = {
                viewModel.deleteProfile(profile.id)
                showDeleteDialog = null
            },
            onDismiss = {
                showDeleteDialog = null
            }
        )
    }
    
    // Показ ошибок
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            // TODO: Показать snackbar с ошибкой
        }
    }
}

@Composable
private fun ProfileList(
    profiles: List<UserProfile>,
    currentProfileId: String?,
    onProfileClick: (UserProfile) -> Unit,
    onEditClick: (String) -> Unit,
    onDeleteClick: (UserProfile) -> Unit,
    onSetCurrentClick: (UserProfile) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(profiles, key = { it.id }) { profile ->
            ProfileItem(
                profile = profile,
                isCurrent = profile.id == currentProfileId,
                onClick = { onProfileClick(profile) },
                onEditClick = { onEditClick(profile.id) },
                onDeleteClick = { onDeleteClick(profile) },
                onSetCurrentClick = { onSetCurrentClick(profile) }
            )
        }
    }
}

@Composable
private fun ProfileItem(
    profile: UserProfile,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSetCurrentClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = profile.getDisplayName(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        if (isCurrent) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "Текущий",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (profile.isPasswordProtected()) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = "Защищено",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        
                        if (profile.isProxyConfigured()) {
                            Icon(
                                Icons.Default.VpnKey,
                                contentDescription = "Прокси",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        
                        if (profile.userAutoLogon) {
                            Icon(
                                Icons.Default.Login,
                                contentDescription = "Автовход",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Последнее изменение: ${formatDate(profile.updatedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Меню действий
                var showMenu by remember { mutableStateOf(false) }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (!isCurrent) {
                            DropdownMenuItem(
                                text = { Text("Сделать текущим") },
                                onClick = {
                                    onSetCurrentClick()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                }
                            )
                        }
                        
                        DropdownMenuItem(
                            text = { Text("Редактировать") },
                            onClick = {
                                onEditClick()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("Удалить") },
                            onClick = {
                                onDeleteClick()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyProfilesState(
    onCreateProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Нет профилей",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Создайте первый профиль для входа в игру",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onCreateProfile
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Создать профиль")
        }
    }
}

@Composable
private fun DeleteProfileDialog(
    profile: UserProfile,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удаление профиля") },
        text = {
            Text("Вы уверены, что хотите удалить профиль \"${profile.getDisplayName()}\"?\n\nЭто действие нельзя отменить.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Удалить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

/**
 * Форматирует дату для отображения
 */
private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}