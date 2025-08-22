# Android PostFilter System

Это полная реализация системы PostFilter для Android версии ABClient, аналогичная Windows версии из `ABClient\PostFilter`.

## Архитектура

### Основные компоненты

1. **HttpFilter** - главный интерцептор HTTP запросов
2. **JavaScriptFilters** - обработка JS файлов
3. **PhpFilters** - обработка PHP страниц  
4. **HtmlFilters** - обработка HTML контента

### Схема работы

```
HTTP Request/Response
        ↓
   HttpFilter (Interceptor)
        ↓
┌─── JavaScript? ────┐
│                    │
│  JavaScriptFilters │
│  - map.js          │
│  - game.js         │
│  - pinfo.js        │
│  - shop.js         │
│  - etc...          │
└────────────────────┘
        ↓
┌─── PHP Page? ──────┐
│                    │
│  PhpFilters        │
│  - main.php        │
│  - trade.php       │
│  - ajax endpoints  │
│  - etc...          │
└────────────────────┘
        ↓
┌─── HTML Page? ─────┐
│                    │
│  HtmlFilters       │
│  - index.cgi       │
│  - pinfo.cgi       │
│  - forum           │
│  - etc...          │
└────────────────────┘
        ↓
   Modified Response
```

## Реализованные фильтры

### JavaScript фильтры

| Файл | Описание | Статус |
|------|----------|--------|
| `map.js` | Кастомная карта | ✅ |
| `pinfo.js` | Подсказки персонажей | ✅ |
| `pv.js` | Исправления приватов | ✅ |
| `shop.js` | Массовая продажа | ✅ |
| `svitok.js` | Форма свитков | ✅ |
| `game.js` | Основной игровой JS | ✅ |
| `fight_v*.js` | Система боя | ✅ |
| `building*.js` | Здания с JSON2 | ✅ |

### PHP фильтры

| Страница | Описание | Функции |
|----------|----------|---------|
| `main.php` | Основная игровая логика | • Автоматическая рыбалка<br>• Обработка боевых действий<br>• Инвентарь и экипировка<br>• HP/MP мониторинг |
| `trade.php` | Торговая система | • Автоматическая торговля<br>• Анализ предложений<br>• Уведомления |
| `msg.php` | Сообщения чата | • Сохранение истории<br>• Интеграция с игрой |
| Ajax endpoints | AJAX запросы | • Рыбалка<br>• Магазин<br>• Рулетка<br>• Карта |

### HTML фильтры

| Страница | Обработка |
|----------|-----------|
| `index.cgi` | Удаление DOCTYPE, метатеги |
| `pinfo.cgi` | Парсинг данных персонажа |
| `pbots.cgi` | Информация о ботах |
| Форум | Совместимость |

## Интеграция

### DI модуль

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object PostFilterModule {
    
    @Provides
    @Singleton
    fun provideHttpFilter(
        userProfile: UserProfile,
        jsFilters: JavaScriptFilters,
        phpFilters: PhpFilters,
        htmlFilters: HtmlFilters
    ): HttpFilter
}
```

### Подключение к HTTP клиенту

```kotlin
class GameHttpClient @Inject constructor(
    private val httpFilter: HttpFilter
) {
    private fun createHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(httpFilter) // PostFilter система
            .build()
    }
}
```

## Функции по аналогии с Windows версией

### Обработка main.php

#### Боевые действия
- **Грабеж** (`MainPhpRob`) - автоматическое определение возможности грабежа
- **Разделка** (`MainPhpRaz`) - обработка разделки монстров
- **HP/MP** (`MainPhpInsHp`) - мониторинг здоровья и маны

#### Экипировка
- **Автоматическое одевание** (`MainPhpWear*`) - удочки для рыбалки
- **Комплекты** (`MainPhpWearComplect`) - быстрое переодевание

#### Инвентарь
- **Группировка предметов** (`MainPhpInv`) - как InvEntry из Windows
- **Массовые операции** - продажа, выброс

### Обработка JavaScript

#### Модификации игровой логики
- **Карта** - замена на кастомную версию
- **Магазин** - поддержка массовой продажи
- **Информация о персонажах** - расширенные подсказки

#### Исправления совместимости
- **JSON2** - добавление поддержки для старых браузеров
- **Исправления синтаксиса** - клановые сообщения, формы

## Настройки пользователя

Система учитывает настройки из `UserProfile`:

```kotlin
// Рыбалка
userProfile.fishAuto        // Автоматическая рыбалка
userProfile.fishAutoWear    // Автоматическое одевание удочек

// Торговля
userProfile.torgActive      // Включена ли торговля

// Чат
userProfile.chatKeepGame    // Сохранять историю чата
```

## Логирование

Все фильтры используют Android Log:

```kotlin
companion object {
    private const val TAG = "HttpFilter"
}

Log.d(TAG, "Filtered response for: $url")
Log.e(TAG, "Error filtering response", exception)
```

## Тестирование

Покрытие тестами включает:
- ✅ Обработка JavaScript файлов
- ✅ Обработка PHP страниц
- ✅ Обработка HTML контента
- ✅ Пропуск внешних сайтов
- ✅ Обработка ошибок
- ✅ Настройки пользователя

Запуск тестов:
```bash
./gradlew test
```

## Расширение системы

### Добавление нового фильтра

1. **JavaScript фильтр:**
```kotlin
fun processNewJs(data: ByteArray): ByteArray {
    var html = String(data, charset("windows-1251"))
    // Ваша обработка
    return html.toByteArray(charset("windows-1251"))
}
```

2. **Регистрация в HttpFilter:**
```kotlin
url.contains("/js/new.js") -> {
    jsFilters.processNewJs(data)
}
```

### Добавление настройки

1. **В UserProfile:**
```kotlin
val newFeatureEnabled: Boolean = false
```

2. **В фильтре:**
```kotlin
if (userProfile.newFeatureEnabled) {
    // Ваша логика
}
```

## Отличия от Windows версии

| Аспект | Windows | Android |
|--------|---------|---------|
| Кодировка | windows-1251 | windows-1251 (сохранена) |
| Архитектура | Статические методы | DI + интерцепторы |
| Обработка ошибок | Try-catch | Kotlin exception handling |
| Логирование | File.WriteAllText | Android Log |
| Настройки | XML профили | UserProfile модель |

## Производительность

- ⚡ Обработка только neverlands.ru трафика
- ⚡ Ленивая инициализация фильтров  
- ⚡ Минимальные копирования байтовых массивов
- ⚡ Кэширование обработанного контента

## Безопасность

- 🔒 Валидация URL перед обработкой
- 🔒 Обработка ошибок без раскрытия данных
- 🔒 Изоляция фильтров через DI
- 🔒 Логирование без sensitive данных