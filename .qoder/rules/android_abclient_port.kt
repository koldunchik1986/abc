// UserConfig.kt - Модель данных пользователя
data class UserConfig(
    var userNick: String = "",
    var userKey: String = "",
    var userPassword: String = "",
    var userPasswordFlash: String = "",
    var userAutoLogon: Boolean = false,
    var doProxy: Boolean = false,
    var proxyAddress: String = "",
    var proxyUserName: String = "",
    var proxyPassword: String = "",
    var configHash: String = "",
    var configPassword: String = "",
    var lastSaved: Long = System.currentTimeMillis()
) {
    fun isPasswordProtected(): Boolean = configHash.isNotEmpty()
    
    fun humanFormatConfigLastSaved(): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(Date(lastSaved))
    }
    
    override fun toString(): String = userNick
}

// AuthManager.kt - Основной класс авторизации
class AuthManager(private val context: Context) {
    companion object {
        private const val TAG = "AuthManager"
        private const val PREFS_NAME = "ab_game_auth"
        private const val BASE_URL = "https://www.neverlands.ru/"
        private const val LOGIN_URL = "${BASE_URL}main.pl"
        private const val GAME_URL = "${BASE_URL}game/"
        
        // User-Agent как в ПК версии
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val httpClient: OkHttpClient
    private val cookieJar = PersistentCookieJar()
    
    init {
        httpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate")
                    .header("Connection", "keep-alive")
                    .build()
                chain.proceed(request)
            }
            .build()
    }
    
    // Сохранение профилей
    fun saveProfiles(profiles: List<UserConfig>) {
        val json = gson.toJson(profiles)
        prefs.edit().putString("profiles", json).apply()
    }
    
    // Загрузка профилей
    fun loadProfiles(): List<UserConfig> {
        val json = prefs.getString("profiles", null) ?: return emptyList()
        val type = object : TypeToken<List<UserConfig>>() {}.type
        return gson.fromJson(json, type)
    }
    
    // Авторизация пользователя
    suspend fun login(userConfig: UserConfig, callback: AuthCallback) {
        withContext(Dispatchers.IO) {
            try {
                // Первый запрос - получение главной страницы
                val mainPageRequest = Request.Builder()
                    .url(BASE_URL)
                    .get()
                    .build()
                
                val mainPageResponse = httpClient.newCall(mainPageRequest).execute()
                if (!mainPageResponse.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        callback.onError("Ошибка загрузки главной страницы: ${mainPageResponse.code}")
                    }
                    return@withContext
                }
                mainPageResponse.close()
                
                // Авторизация
                val loginFormData = FormBody.Builder()
                    .add("login", userConfig.userNick)
                    .add("passwd", userConfig.userPassword)
                    .add("enter", "войти в игру")
                    .build()
                
                val loginRequest = Request.Builder()
                    .url(LOGIN_URL)
                    .post(loginFormData)
                    .header("Referer", BASE_URL)
                    .build()
                
                val loginResponse = httpClient.newCall(loginRequest).execute()
                val loginResponseBody = loginResponse.body?.string() ?: ""
                
                if (loginResponse.isSuccessful) {
                    // Проверка успешности входа
                    when {
                        loginResponseBody.contains("Неверный логин или пароль") -> {
                            withContext(Dispatchers.Main) {
                                callback.onError("Неверный логин или пароль")
                            }
                        }
                        loginResponseBody.contains("error") -> {
                            withContext(Dispatchers.Main) {
                                callback.onError("Ошибка авторизации")
                            }
                        }
                        loginResponseBody.contains("Введите код") -> {
                            // Требуется ввод капчи
                            handleCaptcha(loginResponseBody, userConfig, callback)
                        }
                        else -> {
                            // Успешная авторизация
                            userConfig.lastSaved = System.currentTimeMillis()
                            withContext(Dispatchers.Main) {
                                callback.onSuccess("Авторизация успешна")
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback.onError("Ошибка сервера: ${loginResponse.code}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Login error", e)
                withContext(Dispatchers.Main) {
                    callback.onError("Ошибка подключения: ${e.message}")
                }
            }
        }
    }
    
    // Обработка капчи
    private suspend fun handleCaptcha(html: String, userConfig: UserConfig, callback: AuthCallback) {
        // Извлечение URL капчи из HTML
        val captchaPattern = """<img[^>]*src="([^"]*captcha[^"]*)"[^>]*>""".toRegex()
        val captchaMatch = captchaPattern.find(html)
        
        if (captchaMatch != null) {
            val captchaUrl = BASE_URL + captchaMatch.groupValues[1].replace("&amp;", "&")
            
            // Загружаем изображение капчи
            val captchaRequest = Request.Builder()
                .url(captchaUrl)
                .get()
                .build()
            
            val captchaResponse = httpClient.newCall(captchaRequest).execute()
            if (captchaResponse.isSuccessful) {
                val captchaBytes = captchaResponse.body?.bytes()
                withContext(Dispatchers.Main) {
                    callback.onCaptchaRequired(captchaBytes, userConfig)
                }
            }
        }
    }
    
    // Отправка капчи
    suspend fun submitCaptcha(userConfig: UserConfig, captchaCode: String, callback: AuthCallback) {
        withContext(Dispatchers.IO) {
            try {
                val captchaFormData = FormBody.Builder()
                    .add("login", userConfig.userNick)
                    .add("passwd", userConfig.userPassword)
                    .add("captcha", captchaCode)
                    .add("enter", "войти в игру")
                    .build()
                
                val captchaRequest = Request.Builder()
                    .url(LOGIN_URL)
                    .post(captchaFormData)
                    .build()
                
                val captchaResponse = httpClient.newCall(captchaRequest).execute()
                val responseBody = captchaResponse.body?.string() ?: ""
                
                if (captchaResponse.isSuccessful && !responseBody.contains("error")) {
                    userConfig.lastSaved = System.currentTimeMillis()
                    withContext(Dispatchers.Main) {
                        callback.onSuccess("Авторизация успешна")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback.onError("Неверный код капчи")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Captcha submit error", e)
                withContext(Dispatchers.Main) {
                    callback.onError("Ошибка отправки капчи: ${e.message}")
                }
            }
        }
    }
    
    // Получение cookies для веб-интерфейса
    fun getCookies(): String {
        val cookies = cookieJar.loadForRequest(HttpUrl.parse(BASE_URL)!!)
        return cookies.joinToString("; ") { "${it.name}=${it.value}" }
    }
}

// AuthCallback.kt - Интерфейс обратных вызовов
interface AuthCallback {
    fun onSuccess(message: String)
    fun onError(message: String)
    fun onCaptchaRequired(captchaBytes: ByteArray?, userConfig: UserConfig)
}

// ProfileActivity.kt - Activity создания/редактирования профиля
class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private var userConfig: UserConfig? = null
    private var isEditMode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Получение профиля для редактирования (если есть)
        userConfig = intent.getParcelableExtra("user_config")
        isEditMode = userConfig != null
        
        if (isEditMode) {
            loadProfileData()
            title = userConfig?.userNick ?: "Редактирование профиля"
            binding.buttonSave.text = "Сохранить"
        } else {
            userConfig = UserConfig()
            title = "Новый персонаж"
            binding.buttonSave.text = "Вход в игру"
        }
        
        setupViews()
    }
    
    private fun loadProfileData() {
        userConfig?.let { config ->
            binding.editUsername.setText(config.userNick)
            binding.editUserKey.setText(config.userKey)
            binding.editPassword.setText(config.userPassword)
            binding.editFlashPassword.setText(config.userPasswordFlash)
            binding.checkAutoLogon.isChecked = config.userAutoLogon
            binding.checkUseProxy.isChecked = config.doProxy
            binding.editProxyAddress.setText(config.proxyAddress)
            binding.editProxyUsername.setText(config.proxyUserName)
            binding.editProxyPassword.setText(config.proxyPassword)
        }
    }
    
    private fun setupViews() {
        // Обработчики изменения полей
        binding.editUsername.addTextChangedListener { checkFormValidity() }
        binding.editPassword.addTextChangedListener { checkFormValidity() }
        binding.editProxyAddress.addTextChangedListener { checkFormValidity() }
        
        // Переключение видимости паролей
        binding.checkVisiblePasswords.setOnCheckedChangeListener { _, isChecked ->
            val inputType = if (isChecked) {
                InputType.TYPE_CLASS_TEXT
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            binding.editPassword.inputType = inputType
            binding.editFlashPassword.inputType = inputType
            binding.editProxyPassword.inputType = inputType
        }
        
        // Переключение прокси
        binding.checkUseProxy.setOnCheckedChangeListener { _, isChecked ->
            binding.editProxyAddress.isEnabled = isChecked
            binding.editProxyUsername.isEnabled = isChecked
            binding.editProxyPassword.isEnabled = isChecked
            checkFormValidity()
        }
        
        // Автоопределение прокси
        binding.buttonDetectProxy.setOnClickListener {
            detectProxy()
        }
        
        // Сохранение
        binding.buttonSave.setOnClickListener {
            saveProfile()
        }
        
        // Отмена
        binding.buttonCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        
        checkFormValidity()
    }
    
    private fun checkFormValidity() {
        val nickValid = binding.editUsername.text.toString().trim().isNotEmpty()
        val passwordValid = binding.editPassword.text.toString().trim().isNotEmpty()
        val proxyValid = !binding.checkUseProxy.isChecked || 
                        binding.editProxyAddress.text.toString().trim().isNotEmpty()
        
        binding.buttonSave.isEnabled = nickValid && passwordValid && proxyValid
        binding.checkAutoLogon.isEnabled = nickValid && passwordValid
    }
    
    private fun detectProxy() {
        // В Android сложно получить системные настройки прокси
        // Показываем диалог с инструкцией
        AlertDialog.Builder(this)
            .setTitle("Определение прокси")
            .setMessage("В Android настройки прокси можно найти в:\nНастройки → Wi-Fi → (имя сети) → Дополнительно → Прокси")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun saveProfile() {
        userConfig?.let { config ->
            config.userNick = binding.editUsername.text.toString().trim()
            config.userKey = binding.editUserKey.text.toString().trim()
            config.userPassword = binding.editPassword.text.toString().trim()
            config.userPasswordFlash = binding.editFlashPassword.text.toString().trim()
            config.userAutoLogon = binding.checkAutoLogon.isChecked
            config.doProxy = binding.checkUseProxy.isChecked
            config.proxyAddress = binding.editProxyAddress.text.toString().trim()
            config.proxyUserName = binding.editProxyUsername.text.toString().trim()
            config.proxyPassword = binding.editProxyPassword.text.toString().trim()
            config.lastSaved = System.currentTimeMillis()
            
            val intent = Intent().apply {
                putExtra("user_config", config)
                putExtra("is_new", !isEditMode)
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }
}

// MainActivity.kt - Главная активность выбора профилей
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var authManager: AuthManager
    private var profiles = mutableListOf<UserConfig>()
    private lateinit var profilesAdapter: ProfilesAdapter
    
    companion object {
        private const val REQUEST_CREATE_PROFILE = 1001
        private const val REQUEST_EDIT_PROFILE = 1002
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authManager = AuthManager(this)
        loadProfiles()
        setupViews()
    }
    
    private fun loadProfiles() {
        profiles.clear()
        profiles.addAll(authManager.loadProfiles())
        
        if (profiles.isEmpty()) {
            // Если профилей нет, создаем новый
            createNewProfile()
        }
    }
    
    private fun setupViews() {
        profilesAdapter = ProfilesAdapter(profiles) { position, action ->
            when (action) {
                ProfileAction.SELECT -> selectProfile(profiles[position])
                ProfileAction.EDIT -> editProfile(profiles[position], position)
                ProfileAction.DELETE -> deleteProfile(position)
            }
        }
        
        binding.recyclerProfiles.adapter = profilesAdapter
        
        binding.buttonCreateNew.setOnClickListener { createNewProfile() }
        binding.buttonLogin.setOnClickListener { 
            if (profiles.isNotEmpty()) {
                loginWithProfile(profiles[0])
            }
        }
        
        // Автологин если настроен
        checkAutoLogin()
    }
    
    private fun checkAutoLogin() {
        profiles.find { it.userAutoLogon }?.let { profile ->
            showAutoLoginDialog(profile)
        }
    }
    
    private fun showAutoLoginDialog(profile: UserConfig) {
        val dialog = AutoLoginDialog(this, profile.userNick) { shouldLogin ->
            if (shouldLogin) {
                loginWithProfile(profile)
            }
        }
        dialog.show()
    }
    
    private fun createNewProfile() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivityForResult(intent, REQUEST_CREATE_PROFILE)
    }
    
    private fun editProfile(profile: UserConfig, position: Int) {
        val intent = Intent(this, ProfileActivity::class.java).apply {
            putExtra("user_config", profile)
            putExtra("position", position)
        }
        startActivityForResult(intent, REQUEST_EDIT_PROFILE)
    }
    
    private fun selectProfile(profile: UserConfig) {
        loginWithProfile(profile)
    }
    
    private fun deleteProfile(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Удаление профиля")
            .setMessage("Удалить профиль ${profiles[position].userNick}?")
            .setPositiveButton("Удалить") { _, _ ->
                profiles.removeAt(position)
                profilesAdapter.notifyItemRemoved(position)
                authManager.saveProfiles(profiles)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun loginWithProfile(profile: UserConfig) {
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonLogin.isEnabled = false
        
        lifecycleScope.launch {
            authManager.login(profile, object : AuthCallback {
                override fun onSuccess(message: String) {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonLogin.isEnabled = true
                    
                    // Запускаем игровую активность
                    startGameActivity(profile)
                }
                
                override fun onError(message: String) {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonLogin.isEnabled = true
                    
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                }
                
                override fun onCaptchaRequired(captchaBytes: ByteArray?, userConfig: UserConfig) {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonLogin.isEnabled = true
                    
                    showCaptchaDialog(captchaBytes, userConfig)
                }
            })
        }
    }
    
    private fun showCaptchaDialog(captchaBytes: ByteArray?, userConfig: UserConfig) {
        val dialog = CaptchaDialog(this, captchaBytes) { captchaCode ->
            if (captchaCode != null) {
                binding.progressBar.visibility = View.VISIBLE
                binding.buttonLogin.isEnabled = false
                
                lifecycleScope.launch {
                    authManager.submitCaptcha(userConfig, captchaCode, object : AuthCallback {
                        override fun onSuccess(message: String) {
                            binding.progressBar.visibility = View.GONE
                            binding.buttonLogin.isEnabled = true
                            startGameActivity(userConfig)
                        }
                        
                        override fun onError(message: String) {
                            binding.progressBar.visibility = View.GONE
                            binding.buttonLogin.isEnabled = true
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                        }
                        
                        override fun onCaptchaRequired(captchaBytes: ByteArray?, userConfig: UserConfig) {
                            // Повторный запрос капчи
                            showCaptchaDialog(captchaBytes, userConfig)
                        }
                    })
                }
            }
        }
        dialog.show()
    }
    
    private fun startGameActivity(profile: UserConfig) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("user_config", profile)
            putExtra("cookies", authManager.getCookies())
        }
        startActivity(intent)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == Activity.RESULT_OK && data != null) {
            val profile = data.getParcelableExtra<UserConfig>("user_config")
            val isNew = data.getBooleanExtra("is_new", false)
            
            profile?.let {
                when (requestCode) {
                    REQUEST_CREATE_PROFILE -> {
                        profiles.add(it)
                        profilesAdapter.notifyItemInserted(profiles.size - 1)
                    }
                    REQUEST_EDIT_PROFILE -> {
                        val position = data.getIntExtra("position", -1)
                        if (position >= 0) {
                            profiles[position] = it
                            profilesAdapter.notifyItemChanged(position)
                        }
                    }
                }
                authManager.saveProfiles(profiles)
                
                if (isNew) {
                    // Сразу логинимся с новым профилем
                    loginWithProfile(it)
                }
            }
        }
    }
}

// AutoLoginDialog.kt - Диалог автовхода
class AutoLoginDialog(
    context: Context,
    private val username: String,
    private val callback: (Boolean) -> Unit
) : Dialog(context) {
    
    private var countDown = 3
    private var timer: CountDownTimer? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_auto_login)
        
        findViewById<TextView>(R.id.textUsername).text = username
        val buttonOk = findViewById<Button>(R.id.buttonOk)
        val buttonCancel = findViewById<Button>(R.id.buttonCancel)
        
        buttonOk.setOnClickListener {
            timer?.cancel()
            callback(true)
            dismiss()
        }
        
        buttonCancel.setOnClickListener {
            timer?.cancel()
            callback(false)
            dismiss()
        }
        
        startCountdown(buttonOk)
    }
    
    private fun startCountdown(button: Button) {
        timer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countDown = (millisUntilFinished / 1000).toInt() + 1
                button.text = "Автовход через $countDown сек"
            }
            
            override fun onFinish() {
                callback(true)
                dismiss()
            }
        }.start()
    }
    
    override fun onStop() {
        super.onStop()
        timer?.cancel()
    }
}