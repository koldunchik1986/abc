package com.koldunchik1986.ANL.ui.tabmanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import com.koldunchik1986.ANL.ui.theme.ABClientTheme

/**
 * Менеджер вкладок - полный аналог FormNewTab из Windows версии
 * Позволяет создавать новые вкладки с различными типами адресов
 */
@AndroidEntryPoint
class TabManagerActivity : ComponentActivity() {
    
    companion object {
        const val RESULT_ADDRESS = "result_address"
        
        fun createIntent(context: Context): Intent {
            return Intent(context, TabManagerActivity::class.java)
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
                    TabManagerScreen(
                        onBack = { finish() },
                        onAddressConfirmed = { address ->
                            val resultIntent = Intent().apply {
                                putExtra(RESULT_ADDRESS, address)
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
fun TabManagerScreen(
    onBack: () -> Unit,
    onAddressConfirmed: (String) -> Unit,
    viewModel: TabManagerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadClipboardText()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новая вкладка") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val address = viewModel.getFormattedAddress()
                            if (address != null) {
                                onAddressConfirmed(address)
                            }
                        },
                        enabled = uiState.isAddressValid
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Создать")
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Поле ввода адреса
            AddressInputCard(
                uiState = uiState,
                onAddressChanged = viewModel::setAddress
            )
            
            // Выбор типа адреса
            AddressTypeSelectionCard(
                uiState = uiState,
                onAddressTypeChanged = viewModel::setAddressType
            )
            
            // Кнопка создания
            Button(
                onClick = {
                    val address = viewModel.getFormattedAddress()
                    if (address != null) {
                        onAddressConfirmed(address)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState.isAddressValid
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Создать вкладку",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun AddressInputCard(
    uiState: TabManagerUiState,
    onAddressChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Адрес",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            OutlinedTextField(
                value = uiState.addressInput,
                onValueChange = onAddressChanged,
                label = { Text("Введите адрес или имя игрока") },
                placeholder = { 
                    Text(
                        when (uiState.selectedAddressType) {
                            AddressType.PLAYER_INFO -> "Имя игрока"
                            AddressType.FIGHT_LOG -> "ID лога или имя игрока"
                            AddressType.FORUM -> "Номер темы или ссылка"
                            AddressType.URL -> "http://example.com"
                        }
                    )
                },
                isError = !uiState.isAddressValid && uiState.addressInput.isNotEmpty(),
                supportingText = {
                    if (!uiState.isAddressValid && uiState.addressInput.isNotEmpty()) {
                        Text(
                            text = "Некорректный формат адреса",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (uiState.addressInput.isNotEmpty()) {
                        Text(
                            text = "Предварительный адрес: ${uiState.previewAddress}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AddressTypeSelectionCard(
    uiState: TabManagerUiState,
    onAddressTypeChanged: (AddressType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Тип адреса",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AddressType.values().forEach { addressType ->
                    AddressTypeOption(
                        addressType = addressType,
                        isSelected = uiState.selectedAddressType == addressType,
                        onSelected = { onAddressTypeChanged(addressType) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddressTypeOption(
    addressType: AddressType,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelected,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = addressType.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
            Text(
                text = addressType.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
