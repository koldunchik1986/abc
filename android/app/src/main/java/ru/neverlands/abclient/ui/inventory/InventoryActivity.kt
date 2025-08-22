package ru.neverlands.abclient.ui.inventory

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dagger.hilt.android.AndroidEntryPoint
import ru.neverlands.abclient.ui.theme.ABClientTheme

/**
 * Управление инвентарем - функциональность на основе InvEntry из Windows версии
 * Включает сортировку, пакетные операции и торговлю
 */
@AndroidEntryPoint
class InventoryActivity : ComponentActivity() {
    
    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, InventoryActivity::class.java)
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
                    InventoryScreen(
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadInventory()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Инвентарь") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.sortInventory() }) {
                        Icon(Icons.Default.Sort, contentDescription = "Сортировать")
                    }
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            if (uiState.isCompactView) Icons.Default.ViewList else Icons.Default.ViewModule,
                            contentDescription = "Вид"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.refreshInventory() }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Обновить")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Панель фильтров и статистики
            InventoryStatsCard(
                modifier = Modifier.padding(16.dp),
                uiState = uiState,
                onFilterChanged = viewModel::setFilter,
                onShowExpiredToggled = viewModel::toggleShowExpired
            )
            
            // Список предметов
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.filteredItems) { item ->
                    InventoryItemCard(
                        item = item,
                        isCompactView = uiState.isCompactView,
                        onWear = { viewModel.wearItem(item) },
                        onUse = { viewModel.useItem(item) },
                        onSell = { viewModel.sellItem(item) },
                        onDrop = { viewModel.dropItem(item) },
                        onBulkSell = { viewModel.bulkSellItem(item) },
                        onBulkDrop = { viewModel.bulkDropItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryStatsCard(
    modifier: Modifier = Modifier,
    uiState: InventoryUiState,
    onFilterChanged: (String) -> Unit,
    onShowExpiredToggled: () -> Unit
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Предметов: ${uiState.items.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (uiState.expiredItemsCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Просрочено: ${uiState.expiredItemsCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Фильтр по названию
            OutlinedTextField(
                value = uiState.searchFilter,
                onValueChange = onFilterChanged,
                label = { Text("Поиск предметов") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Переключатели
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FilterChip(
                    onClick = onShowExpiredToggled,
                    label = { Text("Показать просроченные") },
                    selected = uiState.showExpired,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun InventoryItemCard(
    item: InventoryItem,
    isCompactView: Boolean,
    onWear: () -> Unit,
    onUse: () -> Unit,
    onSell: () -> Unit,
    onDrop: () -> Unit,
    onBulkSell: () -> Unit,
    onBulkDrop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isExpired) {
                Color(0xFFF5E5E5)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (isCompactView) {
            CompactItemView(
                item = item,
                onWear = onWear,
                onUse = onUse,
                onSell = onSell,
                onDrop = onDrop
            )
        } else {
            DetailedItemView(
                item = item,
                onWear = onWear,
                onUse = onUse,
                onSell = onSell,
                onDrop = onDrop,
                onBulkSell = onBulkSell,
                onBulkDrop = onBulkDrop
            )
        }
    }
}

@Composable
private fun CompactItemView(
    item: InventoryItem,
    onWear: () -> Unit,
    onUse: () -> Unit,
    onSell: () -> Unit,
    onDrop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Иконка предмета
        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.name,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Fit
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Информация о предмете
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if (item.count > 1) "${item.name} (${item.count} шт.)" else item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (item.isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            
            if (item.price > 0) {
                Text(
                    text = "Цена: ${item.price} NV",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Кнопки действий
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (item.canWear) {
                IconButton(onClick = onWear) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Надеть", tint = MaterialTheme.colorScheme.primary)
                }
            }
            
            if (item.canSell) {
                IconButton(onClick = onSell) {
                    Icon(Icons.Default.AttachMoney, contentDescription = "Продать", tint = Color(0xFF4CAF50))
                }
            }
            
            IconButton(onClick = onDrop) {
                Icon(Icons.Default.Delete, contentDescription = "Выбросить", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DetailedItemView(
    item: InventoryItem,
    onWear: () -> Unit,
    onUse: () -> Unit,
    onSell: () -> Unit,
    onDrop: () -> Unit,
    onBulkSell: () -> Unit,
    onBulkDrop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Заголовок с иконкой
        Row(
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (item.isExpired) {
                    Text(
                        text = "ПРОСРОЧЕНО!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = if (item.count > 1) "${item.name} (${item.count} шт.)" else item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                
                if (item.properties.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.properties,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (item.expiryDate.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Срок годности: ${item.expiryDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Информация о предмете
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                if (item.price > 0) {
                    Text(
                        text = "Цена: ${item.price} NV",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (item.durability.isNotEmpty()) {
                    Text(
                        text = "Долговечность: ${item.durability}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Column {
                if (item.level > 0) {
                    Text(
                        text = "Уровень: ${item.level}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (item.mass > 0) {
                    Text(
                        text = "Масса: ${item.mass}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Кнопки действий
        LazyColumn {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.canWear) {
                        Button(
                            onClick = onWear,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Надеть")
                        }
                    }
                    
                    if (item.canUse) {
                        Button(
                            onClick = onUse,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Использовать")
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.canSell) {
                        OutlinedButton(
                            onClick = onSell,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Продать за ${item.sellPrice} NV")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = onDrop,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Выбросить")
                    }
                }
            }
            
            // Пакетные операции для множественных предметов
            if (item.count > 1) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (item.canSell) {
                            OutlinedButton(
                                onClick = onBulkSell,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Продать пачку за ${item.sellPrice * item.count} NV")
                            }
                        }
                        
                        OutlinedButton(
                            onClick = onBulkDrop,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Выбросить всю пачку")
                        }
                    }
                }
            }
        }
    }
}