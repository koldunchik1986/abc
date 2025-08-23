package com.koldunchik1986.ANL.ui.tabs.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.koldunchik1986.ANL.ui.tabs.model.GameTab
import com.koldunchik1986.ANL.ui.tabs.model.TabType
import com.koldunchik1986.ANL.ui.tabs.viewmodel.TabViewModel

/**
 * Компонент системы вкладок
 * Эквивалент tabControl из Windows версии
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabSystem(
    modifier: Modifier = Modifier,
    viewModel: TabViewModel = hiltViewModel(),
    onTabContentChanged: (GameTab) -> Unit = {}
) {
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    
    // Синхронизация активной вкладки с pager
    LaunchedEffect(activeTabId) {
        val activeIndex = tabs.indexOfFirst { it.id == activeTabId }
        if (activeIndex >= 0 && activeIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(activeIndex)
        }
    }
    
    // Обновление активной вкладки при свайпе
    LaunchedEffect(pagerState.currentPage) {
        if (tabs.isNotEmpty() && pagerState.currentPage < tabs.size) {
            val currentTab = tabs[pagerState.currentPage]
            if (currentTab.id != activeTabId) {
                viewModel.setActiveTab(currentTab.id)
            }
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Панель вкладок
        TabBar(
            tabs = tabs,
            activeTabId = activeTabId ?: "",
            onTabClick = { tabId ->
                coroutineScope.launch {
                    val index = tabs.indexOfFirst { it.id == tabId }
                    if (index >= 0) {
                        pagerState.animateScrollToPage(index)
                    }
                }
            },
            onTabClose = { tabId ->
                viewModel.closeTab(tabId)
            },
            onAddTab = {
                viewModel.showAddTabDialog()
            }
        )
        
        // Контент вкладок
        if (tabs.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val tab = tabs[pageIndex]
                TabContent(
                    tab = tab,
                    onContentChanged = onTabContentChanged
                )
            }
        }
    }
}

/**
 * Панель вкладок
 */
@Composable
private fun TabBar(
    tabs: List<GameTab>,
    activeTabId: String,
    onTabClick: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onAddTab: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Список вкладок
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(tabs, key = { it.id }) { tab ->
                    TabItem(
                        tab = tab,
                        isActive = tab.id == activeTabId,
                        onClick = { onTabClick(tab.id) },
                        onClose = if (tab.isCloseable) { { onTabClose(tab.id) } } else null
                    )
                }
            }
            
            // Кнопка добавления вкладки
            IconButton(
                onClick = onAddTab,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить вкладку",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Элемент вкладки
 */
@Composable
private fun TabItem(
    tab: GameTab,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: (() -> Unit)? = null
) {
    val backgroundColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() },
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Иконка вкладки
            Icon(
                imageVector = getTabIcon(tab.type),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            
            // Заголовок
            Text(
                text = tab.title,
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp)
            )
            
            // Индикатор нового контента
            if (tab.hasNewContent && !isActive) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            Color.Red,
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }
            
            // Кнопка закрытия
            if (onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть вкладку",
                        modifier = Modifier.size(12.dp),
                        tint = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Получает иконку для типа вкладки
 */
private fun getTabIcon(tabType: TabType): ImageVector {
    return when (tabType) {
        TabType.GAME -> Icons.Default.Games
        TabType.FORUM -> Icons.Default.Forum
        TabType.PINFO -> Icons.Default.Person
        TabType.FIGHT_LOG -> Icons.Default.History
        TabType.CHAT -> Icons.AutoMirrored.Filled.Chat
        TabType.TODAY_CHAT -> Icons.Default.ChatBubble
        TabType.NOTEPAD -> Icons.Default.Edit
        TabType.CUSTOM -> Icons.Default.Language
    }
}

/**
 * Контент вкладки
 */
@Composable
private fun TabContent(
    tab: GameTab,
    onContentChanged: (GameTab) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (tab.type) {
            TabType.GAME -> {
                GameWebView(
                    tab = tab,
                    onContentChanged = onContentChanged
                )
            }
            TabType.FORUM, TabType.PINFO, TabType.FIGHT_LOG, TabType.CUSTOM -> {
                if (tab.url != null) {
                    GenericWebView(
                        tab = tab,
                        onContentChanged = onContentChanged
                    )
                } else {
                    Text("URL не указан")
                }
            }
            TabType.CHAT -> {
                ChatView(tab = tab)
            }
            TabType.TODAY_CHAT -> {
                TodayChatView(tab = tab)
            }
            TabType.NOTEPAD -> {
                NotepadView(tab = tab)
            }
        }
    }
}

/**
 * Игровой WebView
 */
@Composable
private fun GameWebView(
    tab: GameTab,
    onContentChanged: (GameTab) -> Unit
) {
    // TODO: Реализация игрового WebView
    Text("Игровой контент: ${tab.title}")
}

/**
 * Обычный WebView
 */
@Composable
private fun GenericWebView(
    tab: GameTab,
    onContentChanged: (GameTab) -> Unit
) {
    // TODO: Реализация обычного WebView
    Text("WebView: ${tab.url}")
}

/**
 * Чат
 */
@Composable
private fun ChatView(tab: GameTab) {
    // TODO: Реализация чата
    Text("Чат")
}

/**
 * Сегодняшний чат
 */
@Composable
private fun TodayChatView(tab: GameTab) {
    // TODO: Реализация сегодняшнего чата
    Text("Сегодняшний чат")
}

/**
 * Блокнот
 */
@Composable
private fun NotepadView(tab: GameTab) {
    // TODO: Реализация блокнота
    Text("Блокнот")
}
