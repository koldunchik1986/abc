package ru.neverlands.abclient.ui.tabmanager

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URI
import javax.inject.Inject

@HiltViewModel
class TabManagerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TabManagerUiState())
    val uiState: StateFlow<TabManagerUiState> = _uiState.asStateFlow()
    
    // Адреса из Resources - аналог Resources из Windows версии
    private val addressPInfo = "http://www.neverlands.ru/pinfo.php?name="
    private val addressPName = "http://www.neverlands.ru/pname.php?name="
    private val addressPBots = "http://www.neverlands.ru/pbots.php?name="
    private val addressFightLog = "http://www.neverlands.ru/fight_logs.php?log="
    private val addressForum = "http://www.neverlands.ru/forum/"
    
    fun loadClipboardText() {
        viewModelScope.launch {
            try {
                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = clipboardManager.primaryClip
                
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text?.toString()
                    if (!text.isNullOrEmpty()) {
                        setAddress(text)
                    }
                }
            } catch (e: Exception) {
                // Игнорируем ошибки работы с буфером обмена
            }
        }
    }
    
    fun setAddress(address: String) {
        val detectedType = detectAddressType(address)
        val isValid = validateAddress(address, detectedType)
        val previewAddress = generatePreviewAddress(address, detectedType)
        
        _uiState.value = _uiState.value.copy(
            addressInput = address,
            selectedAddressType = detectedType,
            isAddressValid = isValid,
            previewAddress = previewAddress
        )
    }
    
    fun setAddressType(type: AddressType) {
        val address = _uiState.value.addressInput
        val isValid = validateAddress(address, type)
        val previewAddress = generatePreviewAddress(address, type)
        
        _uiState.value = _uiState.value.copy(
            selectedAddressType = type,
            isAddressValid = isValid,
            previewAddress = previewAddress
        )
    }
    
    fun getFormattedAddress(): String? {
        val address = _uiState.value.addressInput
        val type = _uiState.value.selectedAddressType
        
        if (!_uiState.value.isAddressValid) return null
        
        return when (type) {
            AddressType.PLAYER_INFO -> addAddress(addressPInfo, address)
            AddressType.FIGHT_LOG -> addAddress(addressFightLog, address)
            AddressType.FORUM -> addAddress(addressForum, address)
            AddressType.URL -> addAddress("http://", address)
        }
    }
    
    /**
     * Определяет тип адреса автоматически - логика из textAddress_TextChanged
     */
    private fun detectAddressType(address: String): AddressType {
        if (address.isEmpty()) return AddressType.PLAYER_INFO
        
        // Проверяем на PInfo адреса
        if (address.startsWith(addressPInfo) && address.length > addressPInfo.length) {
            return AddressType.PLAYER_INFO
        }
        
        if (address.startsWith(addressPName) && address.length > addressPName.length) {
            return AddressType.PLAYER_INFO
        }
        
        if (address.startsWith(addressPBots) && address.length > addressPBots.length) {
            return AddressType.PLAYER_INFO
        }
        
        // Проверяем на FightLog адреса
        if (address.startsWith(addressFightLog) && address.length > addressFightLog.length) {
            return AddressType.FIGHT_LOG
        }
        
        // Проверяем на Forum адреса
        if (address.startsWith(addressForum) && address.length >= addressForum.length) {
            return AddressType.FORUM
        }
        
        // Проверяем на абсолютный URI
        try {
            URI.create(address)
            if (address.contains("://")) {
                return AddressType.URL
            }
        } catch (e: Exception) {
            // Не является валидным URI
        }
        
        // Проверяем на число (лог)
        if (address.toLongOrNull() != null) {
            return AddressType.FIGHT_LOG
        }
        
        // Проверяем на URL без протокола
        if (address.isNotEmpty() && 
            address.indexOf(' ') == -1 && 
            address.indexOf('.') != -1) {
            return AddressType.URL
        }
        
        // Проверяем на имя игрока
        if (address.isNotEmpty() && 
            !address.contains("  ") && 
            address.length < 40) {
            return AddressType.PLAYER_INFO
        }
        
        return AddressType.PLAYER_INFO
    }
    
    /**
     * Валидация адреса - логика из textAddress_TextChanged
     */
    private fun validateAddress(address: String, type: AddressType): Boolean {
        if (address.isEmpty()) return false
        
        return when (type) {
            AddressType.PLAYER_INFO -> {
                // Проверяем готовые адреса
                if (address.startsWith(addressPInfo) && address.length > addressPInfo.length) return true
                if (address.startsWith(addressPName) && address.length > addressPName.length) return true
                if (address.startsWith(addressPBots) && address.length > addressPBots.length) return true
                
                // Проверяем имя игрока
                address.isNotEmpty() && !address.contains("  ") && address.length < 40
            }
            
            AddressType.FIGHT_LOG -> {
                // Проверяем готовый адрес лога
                if (address.startsWith(addressFightLog) && address.length > addressFightLog.length) return true
                
                // Проверяем ID лога (число)
                address.toLongOrNull() != null
            }
            
            AddressType.FORUM -> {
                // Проверяем готовый адрес форума
                address.startsWith(addressForum) && address.length >= addressForum.length
            }
            
            AddressType.URL -> {
                try {
                    val uri = URI.create(address)
                    true
                } catch (e: Exception) {
                    // Проверяем URL без протокола
                    address.isNotEmpty() && 
                    address.indexOf(' ') == -1 && 
                    address.indexOf('.') != -1
                }
            }
        }
    }
    
    /**
     * Генерирует предварительный адрес для показа пользователю
     */
    private fun generatePreviewAddress(address: String, type: AddressType): String {
        if (address.isEmpty()) return ""
        
        return when (type) {
            AddressType.PLAYER_INFO -> {
                if (isAbsoluteUri(address)) address
                else addressPInfo + address
            }
            
            AddressType.FIGHT_LOG -> {
                if (isAbsoluteUri(address)) address
                else addressFightLog + address
            }
            
            AddressType.FORUM -> {
                if (isAbsoluteUri(address)) address
                else addressForum + address
            }
            
            AddressType.URL -> {
                if (isAbsoluteUri(address)) address
                else "http://$address"
            }
        }
    }
    
    /**
     * Добавляет префикс к адресу - аналог AddAddress из Windows версии
     */
    private fun addAddress(prefix: String, address: String): String? {
        return try {
            val fullAddress = prefix + address
            URI.create(fullAddress)
            fullAddress
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Проверяет, является ли адрес абсолютным URI
     */
    private fun isAbsoluteUri(address: String): Boolean {
        return try {
            val uri = URI.create(address)
            uri.isAbsolute
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * UI состояние менеджера вкладок
 */
data class TabManagerUiState(
    val addressInput: String = "",
    val selectedAddressType: AddressType = AddressType.PLAYER_INFO,
    val isAddressValid: Boolean = false,
    val previewAddress: String = ""
)

/**
 * Типы адресов - соответствуют радиокнопкам в Windows версии
 */
enum class AddressType(
    val title: String,
    val description: String
) {
    PLAYER_INFO(
        title = "Информация об игроке",
        description = "Показать профиль, статистику или ботов игрока"
    ),
    FIGHT_LOG(
        title = "Лог боя",
        description = "Открыть лог боя по ID или имени игрока"
    ),
    FORUM(
        title = "Форум",
        description = "Открыть тему форума"
    ),
    URL(
        title = "Произвольный URL",
        description = "Открыть любую веб-страницу"
    )
}