package ru.neverlands.abclient.ui.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.neverlands.abclient.ui.profile.viewmodel.ProfileEditViewModel

/**
 * Экран редактирования профиля пользователя
 * Эквивалент FormProfile из Windows версии
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    profileId: String? = null,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    viewModel: ProfileEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(profileId) {
        if (profileId != null) {
            viewModel.loadProfile(profileId)
        } else {
            viewModel.createNewProfile()
        }
    }
    
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSave()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (profileId != null) "Редактирование профиля" else "Новый профиль"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveProfile() },
                        enabled = uiState.isValid && !uiState.isLoading
                    ) {
                        Text("Сохранить")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            ProfileEditContent(
                uiState = uiState,
                onNickChanged = viewModel::setNick,
                onPasswordChanged = viewModel::setPassword,
                onFlashPasswordChanged = viewModel::setFlashPassword,
                onUserKeyChanged = viewModel::setUserKey,
                onAutoLogonChanged = viewModel::setAutoLogon,
                onUseProxyChanged = viewModel::setUseProxy,
                onProxyAddressChanged = viewModel::setProxyAddress,
                onProxyUserNameChanged = viewModel::setProxyUserName,
                onProxyPasswordChanged = viewModel::setProxyPassword,
                onEncryptionToggled = viewModel::toggleEncryption,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
    
    // Диалог ошибки
    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Ошибка") },
            text = { Text(uiState.errorMessage ?: "Unknown error") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun ProfileEditContent(
    uiState: ProfileEditUiState,
    onNickChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onFlashPasswordChanged: (String) -> Unit,
    onUserKeyChanged: (String) -> Unit,
    onAutoLogonChanged: (Boolean) -> Unit,
    onUseProxyChanged: (Boolean) -> Unit,
    onProxyAddressChanged: (String) -> Unit,
    onProxyUserNameChanged: (String) -> Unit,
    onProxyPasswordChanged: (String) -> Unit,
    onEncryptionToggled: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPasswords by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Основные данные пользователя
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Данные пользователя",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = uiState.nick,
                    onValueChange = onNickChanged,
                    label = { Text("Имя пользователя") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.nick.isBlank()
                )
                
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = onPasswordChanged,
                    label = { Text("Пароль") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = uiState.password.isBlank(),
                    trailingIcon = {
                        IconButton(onClick = { showPasswords = !showPasswords }) {
                            Icon(
                                imageVector = if (showPasswords) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPasswords) "Скрыть пароли" else "Показать пароли"
                            )
                        }
                    }
                )
                
                OutlinedTextField(
                    value = uiState.flashPassword,
                    onValueChange = onFlashPasswordChanged,
                    label = { Text("Flash пароль (дополнительная защита)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation()
                )
                
                OutlinedTextField(
                    value = uiState.userKey,
                    onValueChange = onUserKeyChanged,
                    label = { Text("Ключ пользователя (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.autoLogon,
                        onCheckedChange = onAutoLogonChanged,
                        enabled = uiState.isValid && !uiState.isEncrypted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Автоматический вход в игру")
                }
                
                // Кнопка шифрования
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Защита паролей:")
                    TextButton(
                        onClick = onEncryptionToggled,
                        enabled = uiState.isValid
                    ) {
                        Text(
                            if (uiState.isEncrypted) "Убрать защиту" else "Защитить пароли"
                        )
                    }
                }
                
                if (uiState.isEncrypted) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Пароли защищены шифрованием",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
        
        // Настройки прокси
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.useProxy,
                        onCheckedChange = onUseProxyChanged
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Использовать прокси",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (uiState.useProxy) {
                    OutlinedTextField(
                        value = uiState.proxyAddress,
                        onValueChange = onProxyAddressChanged,
                        label = { Text("Адрес прокси (host:port)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("proxy.example.com:8080") }
                    )
                    
                    OutlinedTextField(
                        value = uiState.proxyUserName,
                        onValueChange = onProxyUserNameChanged,
                        label = { Text("Имя пользователя прокси") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = uiState.proxyPassword,
                        onValueChange = onProxyPasswordChanged,
                        label = { Text("Пароль прокси") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation()
                    )
                }
            }
        }
    }
}

/**
 * Состояние UI для редактирования профиля
 */
data class ProfileEditUiState(
    val isLoading: Boolean = false,
    val nick: String = "",
    val password: String = "",
    val flashPassword: String = "",
    val userKey: String = "",
    val autoLogon: Boolean = false,
    val useProxy: Boolean = false,
    val proxyAddress: String = "",
    val proxyUserName: String = "",
    val proxyPassword: String = "",
    val isEncrypted: Boolean = false,
    val isValid: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        fun empty() = ProfileEditUiState()
    }
}