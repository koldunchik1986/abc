# Authentication Flow Analysis for ABClient Android

## Overview
Based on the implemented network logging system, this document outlines the complete authentication flow and potential failure points where the login process might stop.

## Authentication Flow Steps

### 1. Initial Page Load
- **URL**: `http://www.neverlands.ru/` or `index.cgi`
- **Expected Log**: "Переход на главную страницу"
- **Notification**: "Загрузка главной страницы"
- **What to check**: Response should contain login form with `id="auth_form"`

### 2. Auto-Login Form Submission
- **URL**: `http://www.neverlands.ru/game.php`
- **Method**: POST
- **Parameters**:
  - `player_nick`: URL-encoded username (windows-1251)
  - `player_password`: URL-encoded password (windows-1251)
- **Expected Log**: "Отправка данных авторизации", "Логин и пароль"
- **Notification**: "Отправка логина и пароля"

### 3. Authentication Response Analysis
The system will analyze the response from `game.php` and log different scenarios:

#### Success Scenarios:
- **Flash Game Required**: 
  - Pattern: `flashvars="plid=(\d+)"`
  - Log: "Требуется Flash пароль для игрока ID: {playerId}"
  - Notification: "Требуется Flash пароль"
  - Next step: Flash password submission

- **HTML5 Game Success**:
  - Pattern: `<canvas` or content length > 10000
  - Log: "Успешный вход в игру - загружен игровой контент"
  - Notification: "✅ Успешный вход в игру!"

#### Failure Scenarios:
- **Login Error**:
  - Pattern: `show_warn("error_message")`
  - Log: "Ошибка авторизации: {error_message}"
  - Notification: "Ошибка: {error_message}"

- **Cookie Problems**:
  - Pattern: `Cookie...`
  - Log: "Проблема с cookies, требуется повторная загрузка"
  - Notification: "Проблема с cookies"

### 4. Flash Password Submission (if required)
- **URL**: `http://www.neverlands.ru/game.php`
- **Method**: POST
- **Parameters**:
  - `flcheck`: URL-encoded flash password (windows-1251)
  - `nid`: Player ID from previous response
- **Expected Log**: "Отправка Flash пароля"
- **Notification**: "Отправка Flash пароля"

### 5. Main Game Page Check
- **URL**: `http://www.neverlands.ru/main.php`
- **Expected Log**: "Проверка статуса авторизации"
- **Notification**: "Проверка статуса игры"

#### Success:
- Valid game content loaded
- No "Вход" or "Авторизация" text in response

#### Failure:
- Response contains "Вход" or "Авторизация"
- Log: "Сессия не активна - требуется авторизация"
- Notification: "Требуется авторизация"

## Common Failure Points & Solutions

### 1. Cookie Issues
**Symptoms**: 
- "Проблема с cookies" notification
- Repeated redirects to login page

**Solution**: 
- Check CookieJar implementation
- Verify `NeverNick` cookie is being set and transmitted
- Ensure cookie domain matching is correct

### 2. Encoding Problems
**Symptoms**:
- "Ошибка авторизации" with garbled text
- Invalid character errors

**Solution**:
- Verify all parameters use windows-1251 encoding
- Check URL encoding of username/password
- Ensure Content-Type headers are correct

### 3. Session Timeout
**Symptoms**:
- Initial login succeeds but main.php fails
- "Сессия не активна" message

**Solution**:
- Check timing between requests
- Verify session cookies persistence
- Look for server-side session expiration

### 4. Invalid Credentials
**Symptoms**:
- Specific error messages in `show_warn()`
- Examples: "Неверный пароль", "Пользователь не найден"

**Solution**:
- Verify username/password in profile
- Check for typos or case sensitivity
- Ensure flash password is correct if required

### 5. Server Response Issues
**Symptoms**:
- HTTP 500 errors
- Unexpected redirects
- Connection timeouts

**Solution**:
- Check server availability
- Verify proxy settings if used
- Look for network connectivity issues

## Log File Analysis

### Expected Log Format
```
================================================================================
🔄 ИСХОДЯЩИЙ ЗАПРОС
Время: 2024-12-19 15:30:45
Метод: POST
URL: http://www.neverlands.ru/game.php
Заголовки:
  Content-Type: application/x-www-form-urlencoded
  User-Agent: Mozilla/5.0 (compatible browser string)
  Cookie: NeverNick=username; other_cookies
Тело запроса:
player_nick=encoded_username&player_password=encoded_password
================================================================================

📥 ВХОДЯЩИЙ ОТВЕТ
Время: 2024-12-19 15:30:46
URL: http://www.neverlands.ru/game.php
Код ответа: 200
Длительность: 1250ms
Заголовки:
  Set-Cookie: session_id=abc123; Path=/
  Content-Type: text/html; charset=windows-1251
Тело ответа:
[HTML content with game response]
================================================================================

🔐 ЭТАП АВТОРИЗАЦИИ
Время: 2024-12-19 15:30:46
Этап: Отправка данных авторизации
Детали: Логин и пароль
----------------------------------------
```

### Key Indicators to Look For

1. **Request Headers**: Verify proper User-Agent and Cookie headers
2. **Response Codes**: 200 = success, 302 = redirect, 4xx/5xx = error
3. **Response Content**: Look for error patterns or success indicators
4. **Timing**: Check for unusual delays or timeouts
5. **Cookie Flow**: Verify cookies are being set and sent correctly

## Testing Recommendations

1. **Install the debug APK** and enable network logging
2. **Create a test profile** with known valid credentials
3. **Attempt login** and monitor notifications in real-time
4. **Access network debug screen** to view detailed logs
5. **Export log.txt** for detailed analysis
6. **Compare with Windows version** behavior if possible

## Network Debug Screen Features

- **Real-time log viewing**: See logs as they're generated
- **Log size monitoring**: Track file growth and rotation
- **Export functionality**: Share logs for analysis
- **Connection testing**: Verify basic connectivity
- **Clear logs**: Reset for new test sessions

This comprehensive logging system will capture every aspect of the authentication process, making it easy to identify exactly where the login process fails.