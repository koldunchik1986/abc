package com.koldunchik1986.ANL.ui.tabs.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.koldunchik1986.ANL.data.preferences.UserPreferencesManager
import com.koldunchik1986.ANL.ui.tabs.model.GameTab
import com.koldunchik1986.ANL.ui.tabs.model.TabType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Менеджер вкладок - эквивалент функциональности FormMainTabs из Windows версии
 * Управляет созданием, закрытием и переключением между вкладками
 */
@Singleton
class TabManager @Inject constructor(
    private val preferencesManager: UserPreferencesManager
) {
    
    companion object {
        private const val MAX_TABS = 10 // Максимальное количество вкладок
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _tabs = MutableStateFlow<List<GameTab>>(emptyList())
    val tabs: StateFlow<List<GameTab>> = _tabs.asStateFlow()
    
    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()
    
    init {
        // Инициализируем с главной игровой вкладкой
        initializeTabs()
    }
    
    /**
     * Инициализация вкладок при запуске приложения
     */
    private fun initializeTabs() {
        val gameTab = GameTab.createGameTab()
        _tabs.value = listOf(gameTab)
        _activeTabId.value = gameTab.id
        
        // Загружаем сохраненные вкладки
        loadSavedTabs()
    }
    
    /**
     * Добавляет новую вкладку
     */
    fun addTab(tab: GameTab): Boolean {
        val currentTabs = _tabs.value
        
        // Проверяем лимит вкладок
        if (currentTabs.size >= MAX_TABS) {
            return false
        }
        
        // Проверяем, не существует ли уже такая вкладка
        val existingTab = findExistingTab(tab)
        if (existingTab != null) {
            setActiveTab(existingTab.id)
            return true
        }
        
        // Деактивируем текущую активную вкладку
        val updatedTabs = currentTabs.map { it.copy(isActive = false) }
        
        // Добавляем новую активную вкладку
        val newTab = tab.copy(isActive = true)
        _tabs.value = updatedTabs + newTab
        _activeTabId.value = newTab.id
        
        // Сохраняем состояние
        saveTabs()
        return true
    }
    
    /**
     * Закрывает вкладку
     */
    fun closeTab(tabId: String): Boolean {
        val currentTabs = _tabs.value
        val tabToClose = currentTabs.find { it.id == tabId }
        
        // Нельзя закрыть главную игровую вкладку
        if (tabToClose == null || !tabToClose.isCloseable) {
            return false
        }
        
        val updatedTabs = currentTabs.filter { it.id != tabId }
        _tabs.value = updatedTabs
        
        // Если закрыли активную вкладку, активируем соседнюю
        if (_activeTabId.value == tabId) {
            val newActiveTab = updatedTabs.firstOrNull { it.type == TabType.GAME }
                ?: updatedTabs.firstOrNull()
            
            newActiveTab?.let { setActiveTab(it.id) }
        }
        
        saveTabs()
        return true
    }
    
    /**
     * Устанавливает активную вкладку
     */
    fun setActiveTab(tabId: String) {
        val currentTabs = _tabs.value
        val updatedTabs = currentTabs.map { tab ->
            tab.copy(
                isActive = tab.id == tabId,
                lastVisited = if (tab.id == tabId) System.currentTimeMillis() else tab.lastVisited
            )
        }
        
        _tabs.value = updatedTabs
        _activeTabId.value = tabId
        saveTabs()
    }
    
    /**
     * Получает активную вкладку
     */
    fun getActiveTab(): GameTab? {
        return _tabs.value.find { it.isActive }
    }
    
    /**
     * Получает вкладку по ID
     */
    fun getTab(tabId: String): GameTab? {
        return _tabs.value.find { it.id == tabId }
    }
    
    /**
     * Обновляет вкладку
     */
    fun updateTab(tabId: String, update: (GameTab) -> GameTab) {
        val currentTabs = _tabs.value
        val updatedTabs = currentTabs.map { tab ->
            if (tab.id == tabId) update(tab) else tab
        }
        _tabs.value = updatedTabs
        saveTabs()
    }
    
    /**
     * Добавляет вкладку форума
     */
    fun addForumTab(url: String): Boolean {
        return addTab(GameTab.createForumTab(url))
    }
    
    /**
     * Добавляет вкладку информации о персонаже
     */
    fun addPInfoTab(nick: String, url: String): Boolean {
        return addTab(GameTab.createPInfoTab(nick, url))
    }
    
    /**
     * Добавляет вкладку чата
     */
    fun addChatTab(): Boolean {
        return addTab(GameTab.createChatTab())
    }
    
    /**
     * Добавляет вкладку блокнота
     */
    fun addNotepadTab(): Boolean {
        return addTab(GameTab.createNotepadTab())
    }
    
    /**
     * Добавляет пользовательскую вкладку
     */
    fun addCustomTab(title: String, url: String): Boolean {
        return addTab(GameTab.createCustomTab(title, url))
    }
    
    /**
     * Отмечает вкладку как имеющую новый контент
     */
    fun markTabWithNewContent(tabId: String) {
        updateTab(tabId) { it.copy(hasNewContent = true) }
    }
    
    /**
     * Снимает отметку о новом контенте
     */
    fun clearNewContentMark(tabId: String) {
        updateTab(tabId) { it.copy(hasNewContent = false) }
    }
    
    /**
     * Находит существующую похожую вкладку
     */
    private fun findExistingTab(newTab: GameTab): GameTab? {
        return _tabs.value.find { existingTab ->
            when (newTab.type) {
                TabType.GAME -> existingTab.type == TabType.GAME
                TabType.CHAT -> existingTab.type == TabType.CHAT
                TabType.NOTEPAD -> existingTab.type == TabType.NOTEPAD
                TabType.FORUM, TabType.PINFO, TabType.FIGHT_LOG, TabType.CUSTOM -> {
                    existingTab.url == newTab.url
                }
                else -> false
            }
        }
    }
    
    /**
     * Сохраняет состояние вкладок
     */
    private fun saveTabs() {
        scope.launch {
            preferencesManager.saveTabs(_tabs.value)
        }
    }
    
    /**
     * Загружает сохраненные вкладки
     */
    private fun loadSavedTabs() {
        scope.launch {
            val savedTabs = preferencesManager.loadTabs()
            if (savedTabs.isNotEmpty()) {
                // Добавляем сохраненные вкладки, кроме игровой (она уже есть)
                val tabsToAdd = savedTabs.filter { it.type != TabType.GAME }
                if (tabsToAdd.isNotEmpty()) {
                    val currentTabs = _tabs.value
                    _tabs.value = currentTabs + tabsToAdd
                    
                    // Восстанавливаем активную вкладку
                    val activeTab = savedTabs.find { it.isActive }
                    if (activeTab != null) {
                        setActiveTab(activeTab.id)
                    }
                }
            }
        }
    }
    
    /**
     * Очищает все вкладки кроме игровой
     */
    fun clearAllTabs() {
        val gameTab = _tabs.value.find { it.type == TabType.GAME }
        if (gameTab != null) {
            _tabs.value = listOf(gameTab.copy(isActive = true))
            _activeTabId.value = gameTab.id
        }
        saveTabs()
    }
    
    /**
     * Получает количество вкладок
     */
    fun getTabCount(): Int = _tabs.value.size
    
    /**
     * Проверяет, можно ли добавить новую вкладку
     */
    fun canAddNewTab(): Boolean = _tabs.value.size < MAX_TABS
}
