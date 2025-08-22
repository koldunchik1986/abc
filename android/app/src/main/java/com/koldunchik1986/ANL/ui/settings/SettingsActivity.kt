package com.koldunchik1986.ANL.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import com.koldunchik1986.ANL.ui.settings.sections.*
import com.koldunchik1986.ANL.ui.theme.ABClientTheme

/**
 * Экран настроек - аналог формы FormSettingsGeneral из Windows клиента
 * Предоставляет интерфейс для изменения настроек приложения
 */
@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    
    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
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
                    SettingsScreen(
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveSettings() }) {
                        Icon(Icons.Default.Save, contentDescription = "Сохранить")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Основные настройки
            GeneralSettingsSection(
                uiState = uiState,
                onPromptExitChanged = viewModel::setPromptExit,
                onTrayChanged = viewModel::setUseTray,
                onTrayBalloonsChanged = viewModel::setShowTrayBalloons
            )
            
            // Настройки чата
            ChatSettingsSection(
                uiState = uiState,
                onKeepMovingChanged = viewModel::setChatKeepMoving,
                onKeepGameChanged = viewModel::setChatKeepGame,
                onKeepLogChanged = viewModel::setChatKeepLog,
                onChatSizeChanged = viewModel::setChatSize,
                onChatLevelsChanged = viewModel::setChatLevels
            )
            
            // Настройки карты
            MapSettingsSection(
                uiState = uiState,
                onExtendedMapChanged = viewModel::setMapExtended,
                onBigMapWidthChanged = viewModel::setBigMapWidth,
                onBigMapHeightChanged = viewModel::setBigMapHeight,
                onBigMapScaleChanged = viewModel::setBigMapScale,
                onBigMapTransparencyChanged = viewModel::setBigMapTransparency,
                onBackColorWhiteChanged = viewModel::setMapBackColorWhite,
                onDrawRegionChanged = viewModel::setMapDrawRegion,
                onShowMiniMapChanged = viewModel::setShowMiniMap,
                onMiniMapWidthChanged = viewModel::setMiniMapWidth,
                onMiniMapHeightChanged = viewModel::setMiniMapHeight,
                onMiniMapScaleChanged = viewModel::setMiniMapScale
            )
            
            // Настройки рыбалки
            FishingSettingsSection(
                uiState = uiState,
                onFishTiedHighChanged = viewModel::setFishTiedHigh,
                onFishTiedZeroChanged = viewModel::setFishTiedZero,
                onStopOverWeightChanged = viewModel::setStopOverWeight,
                onAutoWearChanged = viewModel::setFishAutoWear,
                onHandOneChanged = viewModel::setFishHandOne,
                onHandTwoChanged = viewModel::setFishHandTwo,
                onChatReportChanged = viewModel::setFishChatReport,
                onChatReportColorChanged = viewModel::setFishChatReportColor
            )
            
            // Настройки звука
            SoundSettingsSection(
                uiState = uiState,
                onSoundEnabledChanged = viewModel::setSoundEnabled,
                onPlayDigitsChanged = viewModel::setPlayDigits,
                onPlayAttackChanged = viewModel::setPlayAttack,
                onPlaySndMsgChanged = viewModel::setPlaySndMsg,
                onPlayRefreshChanged = viewModel::setPlayRefresh,
                onPlayAlarmChanged = viewModel::setPlayAlarm,
                onPlayTimerChanged = viewModel::setPlayTimer
            )
            
            // Настройки лечения
            CureSettingsSection(
                uiState = uiState,
                onCureNV1Changed = { viewModel.setCureNV(0, it) },
                onCureNV2Changed = { viewModel.setCureNV(1, it) },
                onCureNV3Changed = { viewModel.setCureNV(2, it) },
                onCureNV4Changed = { viewModel.setCureNV(3, it) },
                onCureEnabled1Changed = { viewModel.setCureEnabled(0, it) },
                onCureEnabled2Changed = { viewModel.setCureEnabled(1, it) },
                onCureEnabled3Changed = { viewModel.setCureEnabled(2, it) },
                onCureEnabled4Changed = { viewModel.setCureEnabled(3, it) },
                onCureDisabledLowLevelsChanged = viewModel::setCureDisabledLowLevels
            )
            
            // Настройки автоответчика
            AutoAnswerSettingsSection(
                uiState = uiState,
                onAutoAnswerChanged = viewModel::setAutoAnswer,
                onAutoAnswerTextChanged = viewModel::setAutoAnswerText
            )
            
            // Настройки торговли
            TradingSettingsSection(
                uiState = uiState,
                onTorgTableChanged = viewModel::setTorgTable,
                onTorgMessageAdvChanged = viewModel::setTorgMessageAdv,
                onTorgAdvTimeChanged = viewModel::setTorgAdvTime,
                onTorgMessageNoMoneyChanged = viewModel::setTorgMessageNoMoney,
                onTorgMessageTooExpChanged = viewModel::setTorgMessageTooExp,
                onTorgMessageThanksChanged = viewModel::setTorgMessageThanks,
                onTorgMessageLess90Changed = viewModel::setTorgMessageLess90,
                onTorgSlivChanged = viewModel::setTorgSliv,
                onTorgMinLevelChanged = viewModel::setTorgMinLevel,
                onTorgExChanged = viewModel::setTorgEx,
                onTorgDenyChanged = viewModel::setTorgDeny
            )
            
            // Настройки инвентаря
            InventorySettingsSection(
                uiState = uiState,
                onInvPackChanged = viewModel::setInvPack,
                onInvPackDolgChanged = viewModel::setInvPackDolg,
                onInvSortChanged = viewModel::setInvSort
            )
            
            // Настройки быстрой атаки
            FastAttackSettingsSection(
                uiState = uiState,
                onShowFastAttackChanged = viewModel::setShowFastAttack,
                onShowBloodChanged = viewModel::setShowFastAttackBlood,
                onShowUltimateChanged = viewModel::setShowFastAttackUltimate,
                onShowClosedUltimateChanged = viewModel::setShowFastAttackClosedUltimate,
                onShowClosedChanged = viewModel::setShowFastAttackClosed,
                onShowFistChanged = viewModel::setShowFastAttackFist,
                onShowClosedFistChanged = viewModel::setShowFastAttackClosedFist,
                onShowOpenNevidChanged = viewModel::setShowFastAttackOpenNevid,
                onShowPoisonChanged = viewModel::setShowFastAttackPoison,
                onShowStrongChanged = viewModel::setShowFastAttackStrong,
                onShowNevidChanged = viewModel::setShowFastAttackNevid,
                onShowFogChanged = viewModel::setShowFastAttackFog,
                onShowZasChanged = viewModel::setShowFastAttackZas,
                onShowTotemChanged = viewModel::setShowFastAttackTotem
            )
        }
    }
}