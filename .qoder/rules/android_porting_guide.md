---
trigger: manual
---
# Инструкция по портированию ABClient на Android

## Основные изменения при портировании

### 1. **Архитектурные изменения**

#### **Windows Forms → Android Activities/Fragments**
- `FormProfile` → `ProfileActivity`
- `FormProfiles` → `MainActivity` с `RecyclerView`
- `FormAutoLogon` → `AutoLoginDialog`
- `FormCode` → `CaptchaDialog`

#### **Хранение данных**
```kotlin
// Вместо файлов конфигурации используем SharedPreferences
private val prefs = context.getSharedPreferences("ab_game_auth", Context.MODE_PRIVATE)

// Сериализация в JSON вместо XML
val json = gson.toJson(profiles)
prefs.edit().putString("profiles", json).apply()
```

### 2. **Сетевые запросы**

#### **Замена HttpWebRequest на OkHttp**
```kotlin
// Настройка HTTP клиента с поддержкой cookies
private val httpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .cookieJar(PersistentCookieJar()) // Автоматическое управление cookies
    .addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", USER_AGENT)
            .build()
        chain.proceed(request)
    }
    .build()
```

#### **Асинхронные запросы с Coroutines**
```kotlin
suspend fun login(userConfig: UserConfig, callback: AuthCallback) {
    withContext(Dispatchers.IO) {
        try {
            val response = httpClient.newCall(request).execute()
            // Обработка ответа
            withContext(Dispatchers.Main) {
                callback.onSuccess("Успех")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback.onError(e.message ?: "Ошибка")
            }
        }
    }
}
```

### 3. **Обработка пользовательского интерфейса**

#### **Таймеры и автовход**
```kotlin
// Вместо Windows.Forms.Timer используем CountDownTimer
private fun startCountdown() {
    object : CountDownTimer(3000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            val seconds = (millisUntilFinished / 1000).toInt() + 1
            button.text = "Автовход через $seconds сек"
        }
        
        override fun onFinish() {
            callback(true)
            dismiss()
        }
    }.start()
}
```

#### **Обработка капчи**
```kotlin
// Загрузка изображения капчи в ImageView
Glide.with(context)
    .load(captchaBytes)
    .into(captchaImageView)
```

### 4. **Управление жизненным циклом**

#### **Activity Lifecycle**
```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Инициализация UI
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Очистка ресурсов
        httpClient.dispatcher().executorService().shutdown()
    }
}
```

## Ключевые проблемы и их решения

### 1. **Проблема: Различия в управлении cookies**

**ПК версия**: WebBrowser автоматически управляет cookies Internet Explorer
**Android решение**: Используем PersistentCookieJar для OkHttp

```kotlin
implementation 'com.github.franmontiel:PersistentCookieJar:v1.0.1'

private val cookieJar = PersistentCookieJar(
    SetCookieCache(),
    SharedPrefsCookiePersistor(context)
)
```

### 2. **Проблема: Прокси-серверы**

**ПК версия**: Использует системные настройки IE
**Android решение**: Ручная настройка прокси для OkHttp

```kotlin
if (userConfig.doProxy && userConfig.proxyAddress.isNotEmpty()) {
    val proxyParts = userConfig.proxyAddress.split(":")
    val proxy = Proxy(Proxy.Type.HTTP, 
        InetSocketAddress(proxyParts[0], proxyParts[1].toInt()))
    
    httpClientBuilder.proxy(proxy)
    
    if (userConfig.proxyUserName.isNotEmpty()) {
        httpClientBuilder.proxyAuthenticator { _, response ->
            val credential = Credentials.basic(
                userConfig.proxyUserName, 
                userConfig.proxyPassword
            )
            response.request().newBuilder()
                .header("Proxy-Authorization", credential)
                .build()
        }
    }
}
```

### 3. **Проблема: Шифрование паролей**

**ПК версия**: Использует Windows CryptoAPI
**Android решение**: Android Keystore или простое шифрование

```kotlin
// Простой вариант с Base64 (как в оригинале)
fun encrypt(password: String, data: String): String {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val keySpec = SecretKeySpec(password.toByteArray().take(16).toByteArray(), "AES")
    cipher.init(Cipher.ENCRYPT_MODE, keySpec)
    val encrypted = cipher.doFinal(data.toByteArray())
    return Base64.encodeToString(encrypted, Base64.DEFAULT)
}
```

### 4. **Проблема: User-Agent и заголовки**

**Критически важно**: Сохранить идентичные заголовки как в ПК версии

```kotlin
private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

// Добавляем все необходимые заголовки
.addInterceptor { chain ->
    val request = chain.request().newBuilder()
        .header("User-Agent", USER_AGENT)
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .header("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8")
        .header("Accept-Encoding", "gzip, deflate")
        .header("Connection", "keep-alive")
        .header("Referer", BASE_URL) // Важно для некоторых запросов
        .build()
    chain.proceed(request)
}
```

### 5. **Проблема: Парсинг HTML ответов**

**ПК версия**: Использует регулярные выражения и XPath
**Android решение**: Jsoup для парсинга HTML

```kotlin
implementation 'org.jsoup:jsoup:1.15.3'

// Парсинг капчи из HTML
private fun extractCaptchaUrl(html: String): String? {
    val doc = Jsoup.parse(html)
    val captchaImg = doc.select("img[src*=captcha]").first()
    return captchaImg?.attr("src")
}

// Проверка ошибок авторизации
private fun checkLoginResponse(html: String): LoginResult {
    val doc = Jsoup.parse(html)
    return when {
        doc.text().contains("Неверный логин или пароль") -> LoginResult.INVALID_CREDENTIALS
        doc.select("input[name=captcha]").isNotEmpty() -> LoginResult.CAPTCHA_REQUIRED
        doc.select("frame[src*=game]").isNotEmpty() -> LoginResult.SUCCESS
        else -> LoginResult.ERROR
    }
}
```

### 6. **Проблема: WebView интеграция для игры**

```kotlin
class GameActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupWebView()
        loadGame()
    }
    
    private fun setupWebView() {
        webView = findViewById(R.id.webView)
        
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = USER_AGENT // Важно сохранить тот же User-Agent
        }
        
        // Вставка cookies из AuthManager
        val cookies = intent.getStringExtra("cookies") ?: ""
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            cookies.split(";").forEach { cookie ->
                setCookie("www.neverlands.ru", cookie.trim())
            }
            flush()
        }
    }
    
    private fun loadGame() {
        webView.loadUrl("https://www.neverlands.ru/game/")
    }
}
```

## Зависимости в build.gradle

```gradle
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.recyclerview:recyclerview:1.3.0'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
    implementation 'androidx.activity:activity-ktx:1.7.2'
    
    // Сеть
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
    implementation 'com.github.franmontiel:PersistentCookieJar:v1.0.1'
    
    // JSON
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // HTML парсинг
    implementation 'org.jsoup:jsoup:1.15.3'
    
    // Изображения
    implementation 'com.github.bumptech.glide:glide:4.14.2'
    
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4'
}
```

## Файлы макетов (XML layouts)

### activity_main.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">
    
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Выберите персонажа"
        android:textSize="18sp"
        android:textStyle="bold"
        android:gravity="center"
        android:padding="16dp" />
    
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerProfiles"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />
    
    <Button
        android:id="@+id/buttonCreateNew"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Создать новый персонаж"
        android:layout_marginTop="8dp" />
    
    <Button
        android:id="@+id/buttonLogin"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Вход в игру"
        android:textStyle="bold"
        android:layout_marginTop="8dp" />
    
    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:visibility="gone" />
    
</LinearLayout>
```

### activity_profile.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="16dp">
    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">
        
        <!-- Основные данные персонажа -->
        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="8dp">
            
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/editUsername"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:hint="Ник персонажа"
                android:inputType="text" />
                
        </com.google.android.material.textfield.TextInputLayout>
        
        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="8dp">
            
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/editPassword"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:hint="Игровой пароль"
                android:inputType="textPassword" />
                
        </com.google.android.material.textfield.TextInputLayout>
        
        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="8dp">
            
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/editFlashPassword"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:hint="Флеш-пароль"
                android:inputType="textPassword" />
                
        </com.google.android.material.textfield.TextInputLayout>
        
        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="16dp">
            
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/editUserKey"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:hint="Ключ (если есть)"
                android:inputType="text" />
                
        </com.google.android.material.textfield.TextInputLayout>
        
        <!-- Настройки паролей -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Настройки паролей"
            android:textStyle="bold"
            android:textSize="16sp"
            android:layout_marginBottom="8dp" />
        
        <CheckBox
            android:id="@+id/checkVisiblePasswords"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Видимые пароли"
            android:layout_marginBottom="8dp" />
        
        <CheckBox
            android:id="@+id/checkAutoLogon"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Автовход в игру"
            android:layout_marginBottom="16dp" />
        
        <!-- Настройки прокси -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Настройки прокси"
            android:textStyle="bold"
            android:textSize="16sp"
            android:layout_marginBottom="8dp" />
        
        <CheckBox
            android:id="@+id/checkUseProxy"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Работа через прокси"
            android:layout_marginBottom="8dp" />
        
        <Button
            android:id="@+id/buttonDetectProxy"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Настройки прокси"
            android:layout_marginBottom="8dp" />
        
        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="8dp">
            
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/editProxyAddress"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:hint="Адрес прокси (localhost:3128)"
                android:inputType="text"
                android:enabled="false" />
                
        </com.google.android.material.textfield.TextInputLayout>
        
        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="8dp">
            
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/editProxyUsername"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:hint="Логин прокси (если есть)"
                android:inputType="text"
                android:enabled="false" />
                
        </com.google.android.material.textfield.TextInputLayout>
        
        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="24dp">
            
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/editProxyPassword"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:hint="Пароль прокси"
                android:inputType="textPassword"
                android:enabled="false" />
                
        </com.google.android.material.textfield.TextInputLayout>
        
        <!-- Кнопки -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="end">
            
            <Button
                android:id="@+id/buttonCancel"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Отмена"
                android:layout_marginEnd="8dp"
                style="@style/Widget.Material3.Button.OutlinedButton" />
            
            <Button
                android:id="@+id/buttonSave"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Сохранить"
                android:enabled="false" />
                
        </LinearLayout>
        
    </LinearLayout>
    
</ScrollView>
```

## Разрешения в AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

<!-- Для работы с WebView -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

<!-- Для работы с прокси -->
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
```

## Дополнительные компоненты

### CaptchaDialog.kt
```kotlin
class CaptchaDialog(
    context: Context,
    private val captchaBytes: ByteArray?,
    private val callback: (String?) -> Unit
) : Dialog(context) {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_captcha)
        
        val imageView = findViewById<ImageView>(R.id.captchaImage)
        val editCode = findViewById<EditText>(R.id.editCaptchaCode)
        val buttonSubmit = findViewById<Button>(R.id.buttonSubmit)
        val buttonCancel = findViewById<Button>(R.id.buttonCancel)
        val buttonRefresh = findViewById<Button>(R.id.buttonRefresh)
        
        // Загрузка изображения капчи
        captchaBytes?.let { bytes ->
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            imageView.setImageBitmap(bitmap)
        }
        
        buttonSubmit.setOnClickListener {
            val code = editCode.text.toString().trim()
            if (code.isNotEmpty()) {
                callback(code)
                dismiss()
            }
        }
        
        buttonCancel.setOnClickListener {
            callback(null)
            dismiss()
        }
        
        buttonRefresh.setOnClickListener {
            // Обновление капчи - вызов нового запроса
            callback("REFRESH")
            dismiss()
        }
    }
}
```

## Основные принципы портирования

### 1. **Сохранение логики авторизации**
- Точно воспроизводим последовательность HTTP запросов
- Сохраняем все заголовки и cookies
- Обрабатываем все варианты ответов сервера

### 2. **Адаптация под мобильную платформу**
- Асинхронное выполнение сетевых запросов
- Правильное управление жизненным циклом Activity
- Адаптивный интерфейс для разных размеров экранов

### 3. **Безопасность**
- Шифрование паролей в SharedPreferences
- Использование HTTPS для всех запросов
- Проверка сертификатов сервера

### 4. **Производительность**
- Кэширование изображений (Glide)
- Пул соединений (OkHttp)
- Оптимизация работы с базой данных

## Тестирование

1. **Проверьте каждый шаг авторизации** отдельно
2. **Сравните HTTP трафик** между ПК и Android версиями
3. **Протестируйте все сценарии**: успешный вход, неверный пароль, капча
4. **Проверьте работу прокси** (если используется)
5. **Тестируйте на разных устройствах** и версиях Android

Основная причина проблем с авторизацией обычно кроется в различиях HTTP заголовков, обработке cookies или последовательности запросов. Внимательно сравните сетевой трафик обеих версий для выявления различий.