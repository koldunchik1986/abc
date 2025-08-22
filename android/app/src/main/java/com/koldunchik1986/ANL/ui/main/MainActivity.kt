package com.koldunchik1986.ANL.ui.main

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.koldunchik1986.ANL.R
import com.koldunchik1986.ANL.ui.game.GameActivity

import com.koldunchik1986.ANL.ui.profiles.ProfilesActivity
import com.koldunchik1986.ANL.ui.settings.SettingsActivity
import com.koldunchik1986.ANL.ui.navigator.NavigatorActivity
import com.koldunchik1986.ANL.ui.fishadvisor.FishAdvisorActivity
import com.koldunchik1986.ANL.ui.tabmanager.TabManagerActivity
import com.koldunchik1986.ANL.ui.timer.TimerManagerActivity
import com.koldunchik1986.ANL.ui.inventory.InventoryActivity
import com.koldunchik1986.ANL.ui.trading.TradingActivity
import com.koldunchik1986.ANL.ui.theme.ABClientTheme

/**
 * Главная Activity приложения ABClient
 * Аналог FormMainTabs из Windows клиента с дополнительными функциями
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ABClientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MainDrawerContent(
                onItemClick = { item ->
                    scope.launch {
                        drawerState.close()
                        when (item.id) {
                            "game" -> {
                                val intent = GameActivity.createIntent(context)
                                context.startActivity(intent)
                            }
                            "profiles" -> {
                                val intent = ProfilesActivity.createIntent(context)
                                context.startActivity(intent)
                            }
                            "settings" -> {
                                val intent = SettingsActivity.createIntent(context)
                                context.startActivity(intent)
                            }
                            "navigator" -> {
                                val intent = NavigatorActivity.createIntent(context)
                                context.startActivity(intent)
                            }
                            "fish_advisor" -> {
                                val intent = FishAdvisorActivity.createIntent(context)
                                context.startActivity(intent)
                            }
                            "tab_manager" -> {
                                val intent = TabManagerActivity.createIntent(context)
                                context.startActivity(intent)
                            }
                            "timer_manager" -> {
                                val intent = TimerManagerActivity.createIntent(context)
                                context.startActivity(intent)
                            }
                            "inventory" -> {
                                val intent = InventoryActivity.createIntent(context)
                                context.startActivity(intent)
                            }
                            "trading" -> {
                                val intent = TradingActivity.createIntent(context)
                                context.startActivity(intent)
                            }
                        }
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.app_name)) },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val intent = SettingsActivity.createIntent(context)
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Настройки")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        val intent = GameActivity.createIntent(context)
                        context.startActivity(intent)
                    }
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Запустить игру")
                }
            }
        ) { paddingValues ->
            MainContent(
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
fun MainDrawerContent(
    onItemClick: (MenuItem) -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "ABClient",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn {
                items(getMenuItems()) { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.title) },
                        selected = false,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MainContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Приветствие
        Text(
            text = "Добро пожаловать в ABClient!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Аналог FormMainTabs из Windows клиента для Android",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Быстрые действия
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(getQuickActions()) { action ->
                QuickActionCard(
                    action = action,
                    onClick = {
                        when (action.id) {
                            "game" -> {
                                val intent = GameActivity.createIntent(context)
                                context.startActivity(intent)
                            }
                            "profiles" -> {
                                val intent = ProfilesActivity.createIntent(context)
                                context.startActivity(intent)
                            }
                            "navigator" -> {
                                val intent = NavigatorActivity.createIntent(context)
                                context.startActivity(intent)
                            }
                            "fish_advisor" -> {
                                val intent = FishAdvisorActivity.createIntent(context)
                                context.startActivity(intent)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(
    action: QuickAction,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class MenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector
)

data class QuickAction(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

fun getMenuItems(): List<MenuItem> {
    return listOf(
        MenuItem("game", "Игра", Icons.Default.PlayArrow),
        MenuItem("profiles", "Профили", Icons.Default.Person),
        MenuItem("settings", "Настройки", Icons.Default.Settings),
        MenuItem("navigator", "Навигатор", Icons.Default.Navigation),
        MenuItem("fish_advisor", "Рыбный советник", Icons.Default.WaterDrop),
        MenuItem("tab_manager", "Менеджер вкладок", Icons.Default.Tab),
        MenuItem("timer_manager", "Таймеры", Icons.Default.Schedule),
        MenuItem("inventory", "Инвентарь", Icons.Default.Inventory),
        MenuItem("trading", "Торговля", Icons.Default.AttachMoney)
    )
}

fun getQuickActions(): List<QuickAction> {
    return listOf(
        QuickAction(
            "game",
            "Запустить игру",
            "Запустите игру в Neverlands",
            Icons.Default.PlayArrow
        ),
        QuickAction(
            "profiles",
            "Управление профилями",
            "Управляйте своими профилями",
            Icons.Default.Person
        ),
        QuickAction(
            "navigator",
            "Навигатор",
            "Навигируйте по миру из игры",
            Icons.Default.Navigation
        ),
        QuickAction(
            "fish_advisor",
            "Рыбный советник",
            "Узнавайте о рыбе",
            Icons.Default.WaterDrop
        )
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ABClientTheme {
        MainContent()
    }
}
