# Network Traffic Popup Notifications Test Verification

## Overview
The network traffic monitoring system includes comprehensive popup notifications that will appear during authentication attempts. This document outlines what notifications to expect.

## Notification Types Implemented

### 1. Toast Notifications (Short-lived popups)

#### HTTP Request Notifications
- **Format**: `🔄 {METHOD} {shortened_url}`
- **Duration**: Short (2-3 seconds)
- **Examples**: 
  - `🔄 GET neverlands.ru/`
  - `🔄 POST game.php`
  - `🔄 GET main.php`

#### HTTP Response Notifications  
- **Format**: `{status_icon} {response_code} {shortened_url} ({duration}ms)`
- **Duration**: Short (2-3 seconds)
- **Status Icons**:
  - ✅ Success (200-299)
  - ❌ Client Error (400-499) 
  - 💥 Server Error (500-599)
  - ❓ Other codes
- **Examples**:
  - `✅ 200 game.php (1250ms)`
  - `❌ 404 nonexistent.php (500ms)`
  - `💥  500 main.php (2000ms)`

#### Authentication Step Notifications
- **Format**: `🔐 {step_description}`
- **Duration**: Long (4-5 seconds)
- **Examples**:
  - `🔐 Загрузка главной страницы`
  - `🔐 Отправка логина и пароля`
  - `🔐 Отправка Flash пароля`
  - `🔐 Требуется Flash пароль`
  - `🔐 ✅ Успешный вход в игру!`
  - `🔐 Проверка статуса игры`

#### Error Notifications
- **Format**: `❌ {error_description}`
- **Duration**: Long (4-5 seconds)
- **Examples**:
  - `❌ Ошибка соединения: Connection timeout`
  - `❌ HTTP 500`
  - `❌ Ошибка: Неверный пароль`
  - `❌ Проблема с cookies`

#### Info Notifications
- **Format**: `ℹ️ {message}`
- **Duration**: Long (4-5 seconds)
- **Examples**:
  - `ℹ️ Лог файл очищен`
  - `ℹ️ Тестирование соединения`

### 2. Persistent Notifications (Notification panel)

#### Authentication Progress
- **Title**: "Авторизация"
- **Content**: Step description
- **Auto-cancel**: Yes
- **Examples**:
  - Title: "Авторизация", Content: "Отправка логина и пароля"
  - Title: "Авторизация", Content: "Требуется Flash пароль"

#### Error States
- **Title**: "Ошибка авторизации"  
- **Content**: Error description
- **Auto-cancel**: Yes
- **Examples**:
  - Title: "Ошибка авторизации", Content: "Неверный пароль"
  - Title: "Ошибка авторизации", Content: "Проблема с cookies"

## Expected Authentication Flow Notifications

### Scenario 1: Successful Login (No Flash Password)
1. `🔄 GET neverlands.ru/` - Loading main page
2. `✅ 200 neverlands.ru/ (800ms)` - Page loaded
3. `🔐 Загрузка главной страницы` - Page analysis
4. `🔄 POST game.php` - Sending credentials  
5. `🔐 Отправка логина и пароля` - Auth step notification
6. `✅ 200 game.php (1200ms)` - Auth response
7. `🔐 ✅ Успешный вход в игру!` - Success notification

### Scenario 2: Flash Password Required
1. `🔄 GET neverlands.ru/` - Loading main page
2. `✅ 200 neverlands.ru/ (800ms)` - Page loaded
3. `🔐 Загрузка главной страницы` - Page analysis
4. `🔄 POST game.php` - Sending credentials
5. `🔐 Отправка логина и пароля` - Auth step notification
6. `✅ 200 game.php (1200ms)` - Auth response  
7. `🔐 Требуется Flash пароль` - Flash required
8. `🔄 POST game.php` - Sending flash password
9. `🔐 Отправка Flash пароля` - Flash step notification
10. `✅ 200 game.php (1000ms)` - Flash response
11. `🔐 ✅ Успешный вход в игру!` - Success notification

### Scenario 3: Login Error
1. `🔄 GET neverlands.ru/` - Loading main page
2. `✅ 200 neverlands.ru/ (800ms)` - Page loaded
3. `🔐 Загрузка главной страницы` - Page analysis
4. `🔄 POST game.php` - Sending credentials
5. `🔐 Отправка логина и пароля` - Auth step notification
6. `✅ 200 game.php (1200ms)` - Auth response
7. `❌ Ошибка: Неверный пароль` - Error notification

### Scenario 4: Session Check
1. Previous authentication steps...
2. `🔄 GET main.php` - Checking session
3. `🔐 Проверка статуса игры` - Status check notification
4. `✅ 200 main.php (600ms)` - Status response
5. Either:
   - `🔐 ✅ Игрок авторизован` - Success
   - `🔐 Требуется авторизация` - Session expired

## Testing Instructions

### Prerequisites
1. Install the debug APK with network logging
2. Ensure notification permissions are granted (Android 13+)
3. Have a test profile configured

### Test Steps
1. **Open the app** and go to main menu
2. **Create/select a profile** with login credentials
3. **Start game login** and watch for notifications
4. **Observe both**:
   - Toast messages appearing at bottom of screen
   - Persistent notifications in notification panel
5. **Access Network Debug** menu to see detailed logs
6. **Try different scenarios**:
   - Valid credentials
   - Invalid credentials  
   - Flash password required profiles
   - Network connectivity issues

### What to Watch For
- **Notification timing**: Should appear immediately with each network action
- **Notification content**: Should match the authentication flow
- **Error handling**: Failed requests should show error notifications
- **Persistence**: Auth steps should also create persistent notifications
- **Icon visibility**: Notification icon should appear in status bar

### Troubleshooting

#### No Notifications Appearing
- Check notification permissions in device settings
- Verify app has POST_NOTIFICATIONS permission
- Ensure device notification volume is not muted
- Check Do Not Disturb settings

#### Missing Toast Messages
- Verify accessibility services are not blocking toasts
- Check if another app is overlaying the screen
- Test on different Android versions

#### Persistent Notifications Not Showing
- Check notification channel settings
- Verify notification importance level
- Look for notification grouping issues

## Technical Details

### Notification Channel
- **ID**: `network_debug_channel`
- **Name**: "Отладка сети"
- **Importance**: Default
- **Sound**: Disabled
- **Vibration**: Disabled

### Implementation Notes
- All notifications run on Main dispatcher for UI thread safety
- Toast messages use coroutines for proper context handling
- Persistent notifications auto-cancel when tapped
- Notification service is injected as singleton via Hilt

This comprehensive notification system provides real-time feedback during the authentication process, making it easy to track exactly what's happening during login attempts.