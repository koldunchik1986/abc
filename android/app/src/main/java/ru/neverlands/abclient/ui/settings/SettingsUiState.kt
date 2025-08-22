package ru.neverlands.abclient.ui.settings

import ru.neverlands.abclient.data.model.UserProfile

/**
 * UI состояние для экрана настроек
 * Содержит все настройки как в FormSettingsGeneral
 */
data class SettingsUiState(
    // Общие настройки
    val doPromptExit: Boolean = true,
    val doTray: Boolean = true,
    val showTrayBaloons: Boolean = true,
    
    // Настройки чата
    val chatKeepMoving: Boolean = true,
    val chatKeepGame: Boolean = true,
    val chatKeepLog: Boolean = true,
    val chatSizeLog: Int = 1000,
    val doChatLevels: Boolean = false,
    
    // Настройки карты
    val mapShowExtended: Boolean = true,
    val mapBigWidth: Int = 800,
    val mapBigHeight: Int = 600,
    val mapBigScale: Float = 1.0f,
    val mapBigTransparency: Float = 1.0f,
    val mapShowBackColorWhite: Boolean = false,
    val mapDrawRegion: Boolean = true,
    val mapShowMiniMap: Boolean = true,
    val mapMiniWidth: Int = 200,
    val mapMiniHeight: Int = 150,
    val mapMiniScale: Float = 0.5f,
    
    // Настройки рыбалки
    val fishTiedHigh: Int = 60,
    val fishTiedZero: Boolean = false,
    val fishStopOverWeight: Boolean = false,
    val fishAutoWear: Boolean = false,
    val fishHandOne: String = "",
    val fishHandTwo: String = "",
    val fishChatReport: Boolean = false,
    val fishChatReportColor: Boolean = false,
    
    // Настройки звука
    val soundEnabled: Boolean = true,
    val doPlayDigits: Boolean = true,
    val doPlayAttack: Boolean = true,
    val doPlaySndMsg: Boolean = true,
    val doPlayRefresh: Boolean = true,
    val doPlayAlarm: Boolean = true,
    val doPlayTimer: Boolean = true,
    
    // Настройки лечения
    val cureNV: List<Int> = listOf(0, 0, 0, 0),
    val cureEnabled: List<Boolean> = listOf(false, false, false, false),
    val cureDisabledLowLevels: Boolean = false,
    
    // Настройки автоответчика
    val doAutoAnswer: Boolean = false,
    val autoAnswer: String = "",
    
    // Настройки торговли
    val torgTabl: String = "",
    val torgMessageAdv: String = "",
    val torgAdvTime: Int = 600,
    val torgMessageNoMoney: String = "",
    val torgMessageTooExp: String = "",
    val torgMessageThanks: String = "",
    val torgMessageLess90: String = "",
    val torgSliv: Boolean = false,
    val torgMinLevel: Int = 1,
    val torgEx: String = "",
    val torgDeny: String = "",
    
    // Настройки инвентаря
    val doInvPack: Boolean = true,
    val doInvPackDolg: Boolean = true,
    val doInvSort: Boolean = true,
    
    // Настройки быстрой атаки
    val doShowFastAttack: Boolean = false,
    val doShowFastAttackBlood: Boolean = true,
    val doShowFastAttackUltimate: Boolean = true,
    val doShowFastAttackClosedUltimate: Boolean = true,
    val doShowFastAttackClosed: Boolean = true,
    val doShowFastAttackFist: Boolean = false,
    val doShowFastAttackClosedFist: Boolean = true,
    val doShowFastAttackOpenNevid: Boolean = true,
    val doShowFastAttackPoison: Boolean = true,
    val doShowFastAttackStrong: Boolean = true,
    val doShowFastAttackNevid: Boolean = true,
    val doShowFastAttackFog: Boolean = true,
    val doShowFastAttackZas: Boolean = true,
    val doShowFastAttackTotem: Boolean = true
) {
    companion object {
        /**
         * Создает SettingsUiState из профиля пользователя
         */
        fun fromProfile(profile: UserProfile): SettingsUiState {
            return SettingsUiState(
                // Общие настройки
                doPromptExit = profile.doPromptExit,
                doTray = profile.doTray,
                showTrayBaloons = profile.showTrayBalloons,
                
                // Настройки чата
                chatKeepMoving = profile.chatKeepMoving,
                chatKeepGame = profile.chatKeepGame,
                chatKeepLog = profile.chatKeepLog,
                chatSizeLog = profile.chatSizeLog,
                doChatLevels = profile.doChatLevels,
                
                // Настройки карты
                mapShowExtended = profile.mapShowExtended,
                mapBigWidth = profile.mapBigWidth,
                mapBigHeight = profile.mapBigHeight,
                mapBigScale = profile.mapBigScale,
                mapBigTransparency = profile.mapBigTransparency,
                mapShowBackColorWhite = profile.mapShowBackColorWhite,
                mapDrawRegion = profile.mapDrawRegion,
                mapShowMiniMap = profile.mapShowMiniMap,
                mapMiniWidth = profile.mapMiniWidth,
                mapMiniHeight = profile.mapMiniHeight,
                mapMiniScale = profile.mapMiniScale,
                
                // Настройки рыбалки
                fishTiedHigh = profile.fishTiedHigh,
                fishTiedZero = profile.fishTiedZero,
                fishStopOverWeight = profile.fishStopOverWeight,
                fishAutoWear = profile.fishAutoWear,
                fishHandOne = profile.fishHandOne,
                fishHandTwo = profile.fishHandTwo,
                fishChatReport = profile.fishChatReport,
                fishChatReportColor = profile.fishChatReportColor,
                
                // Настройки звука
                soundEnabled = profile.soundEnabled,
                doPlayDigits = profile.doPlayDigits,
                doPlayAttack = profile.doPlayAttack,
                doPlaySndMsg = profile.doPlaySndMsg,
                doPlayRefresh = profile.doPlayRefresh,
                doPlayAlarm = profile.doPlayAlarm,
                doPlayTimer = profile.doPlayTimer,
                
                // Настройки лечения
                cureNV = profile.cureNV,
                cureEnabled = profile.cureEnabled,
                cureDisabledLowLevels = profile.cureDisabledLowLevels,
                
                // Настройки автоответчика
                doAutoAnswer = profile.doAutoAnswer,
                autoAnswer = profile.autoAnswer,
                
                // Настройки торговли
                torgTabl = profile.torgTabl,
                torgMessageAdv = profile.torgMessageAdv,
                torgAdvTime = profile.torgAdvTime,
                torgMessageNoMoney = profile.torgMessageNoMoney,
                torgMessageTooExp = profile.torgMessageTooExp,
                torgMessageThanks = profile.torgMessageThanks,
                torgMessageLess90 = profile.torgMessageLess90,
                torgSliv = profile.torgSliv,
                torgMinLevel = profile.torgMinLevel,
                torgEx = profile.torgEx,
                torgDeny = profile.torgDeny,
                
                // Настройки инвентаря
                doInvPack = profile.doInvPack,
                doInvPackDolg = profile.doInvPackDolg,
                doInvSort = profile.doInvSort,
                
                // Настройки быстрой атаки
                doShowFastAttack = profile.doShowFastAttack,
                doShowFastAttackBlood = profile.doShowFastAttackBlood,
                doShowFastAttackUltimate = profile.doShowFastAttackUltimate,
                doShowFastAttackClosedUltimate = profile.doShowFastAttackClosedUltimate,
                doShowFastAttackClosed = profile.doShowFastAttackClosed,
                doShowFastAttackFist = profile.doShowFastAttackFist,
                doShowFastAttackClosedFist = profile.doShowFastAttackClosedFist,
                doShowFastAttackOpenNevid = profile.doShowFastAttackOpenNevid,
                doShowFastAttackPoison = profile.doShowFastAttackPoison,
                doShowFastAttackStrong = profile.doShowFastAttackStrong,
                doShowFastAttackNevid = profile.doShowFastAttackNevid,
                doShowFastAttackFog = profile.doShowFastAttackFog,
                doShowFastAttackZas = profile.doShowFastAttackZas,
                doShowFastAttackTotem = profile.doShowFastAttackTotem
            )
        }
    }
    
    /**
     * Преобразует настройки обратно в профиль
     */
    fun toProfile(baseProfile: UserProfile): UserProfile {
        return baseProfile.copy(
            // Общие настройки
            doPromptExit = doPromptExit,
            doTray = doTray,
            showTrayBalloons = showTrayBaloons,
            
            // Настройки чата
            chatKeepMoving = chatKeepMoving,
            chatKeepGame = chatKeepGame,
            chatKeepLog = chatKeepLog,
            chatSizeLog = chatSizeLog,
            doChatLevels = doChatLevels,
            
            // Настройки карты
            mapShowExtended = mapShowExtended,
            mapBigWidth = mapBigWidth,
            mapBigHeight = mapBigHeight,
            mapBigScale = mapBigScale,
            mapBigTransparency = mapBigTransparency,
            mapShowBackColorWhite = mapShowBackColorWhite,
            mapDrawRegion = mapDrawRegion,
            mapShowMiniMap = mapShowMiniMap,
            mapMiniWidth = mapMiniWidth,
            mapMiniHeight = mapMiniHeight,
            mapMiniScale = mapMiniScale,
            
            // Настройки рыбалки
            fishTiedHigh = fishTiedHigh,
            fishTiedZero = fishTiedZero,
            fishStopOverWeight = fishStopOverWeight,
            fishAutoWear = fishAutoWear,
            fishHandOne = fishHandOne,
            fishHandTwo = fishHandTwo,
            fishChatReport = fishChatReport,
            fishChatReportColor = fishChatReportColor,
            
            // Настройки звука
            soundEnabled = soundEnabled,
            doPlayDigits = doPlayDigits,
            doPlayAttack = doPlayAttack,
            doPlaySndMsg = doPlaySndMsg,
            doPlayRefresh = doPlayRefresh,
            doPlayAlarm = doPlayAlarm,
            doPlayTimer = doPlayTimer,
            
            // Настройки лечения
            cureNV = cureNV,
            cureEnabled = cureEnabled,
            cureDisabledLowLevels = cureDisabledLowLevels,
            
            // Настройки автоответчика
            doAutoAnswer = doAutoAnswer,
            autoAnswer = autoAnswer,
            
            // Настройки торговли
            torgTabl = torgTabl,
            torgMessageAdv = torgMessageAdv,
            torgAdvTime = torgAdvTime,
            torgMessageNoMoney = torgMessageNoMoney,
            torgMessageTooExp = torgMessageTooExp,
            torgMessageThanks = torgMessageThanks,
            torgMessageLess90 = torgMessageLess90,
            torgSliv = torgSliv,
            torgMinLevel = torgMinLevel,
            torgEx = torgEx,
            torgDeny = torgDeny,
            
            // Настройки инвентаря
            doInvPack = doInvPack,
            doInvPackDolg = doInvPackDolg,
            doInvSort = doInvSort,
            
            // Настройки быстрой атаки
            doShowFastAttack = doShowFastAttack,
            doShowFastAttackBlood = doShowFastAttackBlood,
            doShowFastAttackUltimate = doShowFastAttackUltimate,
            doShowFastAttackClosedUltimate = doShowFastAttackClosedUltimate,
            doShowFastAttackClosed = doShowFastAttackClosed,
            doShowFastAttackFist = doShowFastAttackFist,
            doShowFastAttackClosedFist = doShowFastAttackClosedFist,
            doShowFastAttackOpenNevid = doShowFastAttackOpenNevid,
            doShowFastAttackPoison = doShowFastAttackPoison,
            doShowFastAttackStrong = doShowFastAttackStrong,
            doShowFastAttackNevid = doShowFastAttackNevid,
            doShowFastAttackFog = doShowFastAttackFog,
            doShowFastAttackZas = doShowFastAttackZas,
            doShowFastAttackTotem = doShowFastAttackTotem,
            
            // Обновляем временную метку
            updatedAt = System.currentTimeMillis()
        )
    }
}