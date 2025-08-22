package ru.neverlands.abclient.ui.tabs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.neverlands.abclient.ui.tabs.manager.TabManager
import ru.neverlands.abclient.ui.tabs.model.GameTab
import ru.neverlands.abclient.ui.tabs.model.TabType
import javax.inject.Inject

/**
 * ViewModel для управления вкладками
 */
@HiltViewModel
class TabViewModel @Inject constructor(
    private val tabManager: TabManager
) : ViewModel() {
    
    // Состояния UI
    private val _isAddTabDialogVisible = MutableStateFlow(false)
    val isAddTabDialogVisible: StateFlow<Boolean> = _isAddTabDialogVisible.asStateFlow()
    
    private val _selectedTabType = MutableStateFlow(TabType.CUSTOM)
    val selectedTabType: StateFlow<TabType> = _selectedTabType.asStateFlow()
    
    private val _customUrl = MutableStateFlow("")
    val customUrl: StateFlow<String> = _customUrl.asStateFlow()
    
    private val _customTitle = MutableStateFlow("")
    val customTitle: StateFlow<String> = _customTitle.asStateFlow()
    
    // Данные из TabManager
    val tabs: StateFlow<List<GameTab>> = tabManager.tabs
    val activeTabId: StateFlow<String?> = tabManager.activeTabId
    
    /**
     * Устанавливает активную вкладку
     */
    fun setActiveTab(tabId: String) {
        tabManager.setActiveTab(tabId)
    }
    
    /**
     * Закрывает вкладку
     */
    fun closeTab(tabId: String) {
        tabManager.closeTab(tabId)
    }
    
    /**
     * Показывает диалог добавления вкладки
     */
    fun showAddTabDialog() {
        _isAddTabDialogVisible.value = true
        _selectedTabType.value = TabType.CUSTOM
        _customUrl.value = ""
        _customTitle.value = ""
    }
    
    /**
     * Скрывает диалог добавления вкладки
     */
    fun hideAddTabDialog() {
        _isAddTabDialogVisible.value = false
    }
    
    /**
     * Устанавливает тип новой вкладки
     */
    fun setSelectedTabType(tabType: TabType) {
        _selectedTabType.value = tabType
        
        // Автоматически заполняем поля для предустановленных типов
        when (tabType) {
            TabType.FORUM -> {
                _customTitle.value = "Форум"
                _customUrl.value = "http://forum.neverlands.ru/"
            }
            TabType.CHAT -> {
                _customTitle.value = "Чат"
                _customUrl.value = ""
            }
            TabType.NOTEPAD -> {
                _customTitle.value = "Блокнот"
                _customUrl.value = ""
            }
            else -> {
                // Для пользовательских вкладок оставляем поля пустыми
            }
        }
    }
    
    /**
     * Устанавливает URL для пользовательской вкладки
     */
    fun setCustomUrl(url: String) {
        _customUrl.value = url
    }
    
    /**
     * Устанавливает заголовок для пользовательской вкладки
     */
    fun setCustomTitle(title: String) {
        _customTitle.value = title
    }
    
    /**
     * Создает новую вкладку
     */
    fun createTab() {
        viewModelScope.launch {
            val success = when (_selectedTabType.value) {
                TabType.FORUM -> {
                    tabManager.addForumTab(_customUrl.value.ifEmpty { "http://forum.neverlands.ru/" })
                }
                TabType.CHAT -> {
                    tabManager.addChatTab()
                }
                TabType.NOTEPAD -> {
                    tabManager.addNotepadTab()
                }
                TabType.CUSTOM -> {
                    if (_customTitle.value.isNotBlank() && _customUrl.value.isNotBlank()) {
                        tabManager.addCustomTab(_customTitle.value, _customUrl.value)
                    } else {
                        false
                    }
                }
                else -> false
            }
            
            if (success) {
                hideAddTabDialog()
            }
        }
    }
    
    /**
     * Добавляет вкладку форума с URL
     */
    fun addForumTab(url: String) {
        tabManager.addForumTab(url)
    }
    
    /**
     * Добавляет вкладку информации о персонаже
     */
    fun addPInfoTab(nick: String, url: String) {
        tabManager.addPInfoTab(nick, url)
    }
    
    /**
     * Добавляет вкладку лога боя
     */
    fun addFightLogTab(fightId: String) {
        val url = "http://www.neverlands.ru/logs.fcg?fid=$fightId"
        val title = "Лог боя #$fightId"
        tabManager.addCustomTab(title, url)
    }
    
    /**
     * Обновляет заголовок вкладки
     */
    fun updateTabTitle(tabId: String, title: String) {
        tabManager.updateTab(tabId) { it.copy(title = title) }
    }
    
    /**
     * Отмечает вкладку как имеющую новый контент
     */
    fun markTabWithNewContent(tabId: String) {
        tabManager.markTabWithNewContent(tabId)
    }
    
    /**
     * Снимает отметку о новом контенте с активной вкладки
     */
    fun clearNewContentMarkForActiveTab() {
        activeTabId.value?.let { tabId ->
            tabManager.clearNewContentMark(tabId)
        }
    }
    
    /**
     * Получает активную вкладку
     */
    fun getActiveTab(): GameTab? {
        return tabManager.getActiveTab()
    }
    
    /**
     * Проверяет, можно ли добавить новую вкладку
     */
    fun canAddNewTab(): Boolean {
        return tabManager.canAddNewTab()
    }
    
    /**
     * Очищает все вкладки кроме игровой
     */
    fun clearAllTabs() {
        tabManager.clearAllTabs()
    }
    
    /**
     * Проверяет валидность данных для создания новой вкладки
     */
    fun isNewTabDataValid(): Boolean {
        return when (_selectedTabType.value) {
            TabType.FORUM, TabType.CHAT, TabType.NOTEPAD -> true
            TabType.CUSTOM -> _customTitle.value.isNotBlank() && _customUrl.value.isNotBlank()
            else -> false
        }
    }
}