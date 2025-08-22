package com.koldunchik1986.ANL.ui.tabs.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.koldunchik1986.ANL.ui.tabs.model.TabType
import com.koldunchik1986.ANL.ui.tabs.viewmodel.TabViewModel

/**
 * Диалог добавления новой вкладки
 */
@Composable
fun AddTabDialog(
    viewModel: TabViewModel = hiltViewModel()
) {
    val isVisible by viewModel.isAddTabDialogVisible.collectAsStateWithLifecycle()
    val selectedTabType by viewModel.selectedTabType.collectAsStateWithLifecycle()
    val customUrl by viewModel.customUrl.collectAsStateWithLifecycle()
    val customTitle by viewModel.customTitle.collectAsStateWithLifecycle()
    
    if (isVisible) {
        Dialog(
            onDismissRequest = { viewModel.hideAddTabDialog() }
        ) {
            AddTabDialogContent(
                selectedTabType = selectedTabType,
                customUrl = customUrl,
                customTitle = customTitle,
                canAddTab = viewModel.canAddNewTab(),
                isDataValid = viewModel.isNewTabDataValid(),
                onTabTypeSelected = viewModel::setSelectedTabType,
                onUrlChanged = viewModel::setCustomUrl,
                onTitleChanged = viewModel::setCustomTitle,
                onCreateTab = viewModel::createTab,
                onDismiss = viewModel::hideAddTabDialog
            )
        }
    }
}

@Composable
private fun AddTabDialogContent(
    selectedTabType: TabType,
    customUrl: String,
    customTitle: String,
    canAddTab: Boolean,
    isDataValid: Boolean,
    onTabTypeSelected: (TabType) -> Unit,
    onUrlChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onCreateTab: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Заголовок
            Text(
                text = "Добавить вкладку",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            if (!canAddTab) {
                // Предупреждение о превышении лимита
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Достигнуто максимальное количество вкладок",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                // Выбор типа вкладки
                Text(
                    text = "Тип вкладки",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabTypeOption(
                        tabType = TabType.FORUM,
                        title = "Форум",
                        description = "Форум игры",
                        isSelected = selectedTabType == TabType.FORUM,
                        onSelect = onTabTypeSelected
                    )
                    
                    TabTypeOption(
                        tabType = TabType.CHAT,
                        title = "Чат",
                        description = "Окно чата",
                        isSelected = selectedTabType == TabType.CHAT,
                        onSelect = onTabTypeSelected
                    )
                    
                    TabTypeOption(
                        tabType = TabType.NOTEPAD,
                        title = "Блокнот",
                        description = "Текстовый блокнот",
                        isSelected = selectedTabType == TabType.NOTEPAD,
                        onSelect = onTabTypeSelected
                    )
                    
                    TabTypeOption(
                        tabType = TabType.CUSTOM,
                        title = "Пользовательская",
                        description = "Произвольный URL",
                        isSelected = selectedTabType == TabType.CUSTOM,
                        onSelect = onTabTypeSelected
                    )
                }
                
                // Поля для пользовательской вкладки
                if (selectedTabType == TabType.CUSTOM || 
                    selectedTabType == TabType.FORUM) {
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = onTitleChanged,
                        label = { Text("Название вкладки") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedTabType == TabType.CUSTOM,
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = onUrlChanged,
                        label = { Text("URL") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedTabType == TabType.CUSTOM,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri
                        ),
                        singleLine = true,
                        placeholder = {
                            Text("http://example.com")
                        }
                    )
                }
            }
            
            // Кнопки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Отмена")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = onCreateTab,
                    enabled = canAddTab && isDataValid
                ) {
                    Text("Создать")
                }
            }
        }
    }
}

/**
 * Опция выбора типа вкладки
 */
@Composable
private fun TabTypeOption(
    tabType: TabType,
    title: String,
    description: String,
    isSelected: Boolean,
    onSelect: (TabType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = { onSelect(tabType) },
                role = Role.RadioButton
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
