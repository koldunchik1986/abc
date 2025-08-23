// ProfilesAdapter.kt - Адаптер для списка профилей
class ProfilesAdapter(
    private val profiles: MutableList<UserConfig>,
    private val onProfileAction: (Int, ProfileAction) -> Unit
) : RecyclerView.Adapter<ProfilesAdapter.ProfileViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile, parent, false)
        return ProfileViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        holder.bind(profiles[position], position)
    }
    
    override fun getItemCount(): Int = profiles.size
    
    inner class ProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textUsername: TextView = itemView.findViewById(R.id.textUsername)
        private val textLastLogin: TextView = itemView.findViewById(R.id.textLastLogin)
        private val buttonEdit: ImageButton = itemView.findViewById(R.id.buttonEdit)
        private val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDelete)
        
        fun bind(profile: UserConfig, position: Int) {
            textUsername.text = profile.userNick
            textLastLogin.text = "Последний вход: ${profile.humanFormatConfigLastSaved()}"
            
            itemView.setOnClickListener {
                onProfileAction(position, ProfileAction.SELECT)
            }
            
            buttonEdit.setOnClickListener {
                onProfileAction(position, ProfileAction.EDIT)
            }
            
            buttonDelete.setOnClickListener {
                onProfileAction(position, ProfileAction.DELETE)
            }
            
            // Выделение автологин профиля
            itemView.setBackgroundColor(
                if (profile.userAutoLogon) {
                    ContextCompat.getColor(itemView.context, R.color.auto_login_background)
                } else {
                    ContextCompat.getColor(itemView.context, android.R.color.transparent)
                }
            )
        }
    }
}

enum class ProfileAction {
    SELECT, EDIT, DELETE
}

// CaptchaDialog.kt - Диалог ввода капчи
class CaptchaDialog(
    context: Context,
    private val captchaBytes: ByteArray?,
    private val onResult: (String?) -> Unit
) : AlertDialog(context) {
    
    private lateinit var captchaImage: ImageView
    private lateinit var codeInput: EditText
    private lateinit var submitButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_captcha, null)
        setView(view)
        
        captchaImage = view.findViewById(R.id.captchaImage)
        codeInput = view.findViewById(R.id.codeInput)
        submitButton = view.findViewById(R.id.submitButton)
        
        setupViews()
        loadCaptcha()
        
        setTitle("Введите код с картинки")
        setCancelable(true)
        
        setButton(BUTTON_POSITIVE, "Отправить") { _, _ ->
            val code = codeInput.text.toString().trim()
            if (code.isNotEmpty() && code.all { it.isDigit() }) {
                onResult(code)
            }
        }
        
        setButton(BUTTON_NEGATIVE, "Отмена") { _, _ ->
            onResult(null)
        }
        
        setButton(BUTTON_NEUTRAL, "Обновить") { _, _ ->
            onResult("REFRESH")
        }
    }
    
    private fun setupViews() {
        // Автофокус на поле ввода
        codeInput.requestFocus()
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        
        // Валидация ввода - только цифры
        codeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s.toString()
                val isValid = text.isNotEmpty() && text.all { it.isDigit() } && text.length <= 5
                
                getButton(BUTTON_POSITIVE)?.isEnabled = isValid
                
                // Автоотправка при вводе 5 цифр
                if (text.length == 5 && isValid) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isShowing) {
                            onResult(text)
                            dismiss()
                        }
                    }, 500)
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Обработка Enter
        codeInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val code = codeInput.text.toString().trim()
                if (code.isNotEmpty() && code.all { it.isDigit() }) {
                    onResult(code)
                    dismiss()
                }
                true
            } else {
                false
            }
        }
    }
    
    private fun loadCaptcha() {
        captchaBytes?.let { bytes ->
            try {
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                captchaImage.setImageBitmap(bitmap)
                
                // Масштабирование для лучшей видимости
                captchaImage.scaleType = ImageView.ScaleType.MATRIX
                val matrix = Matrix()
                matrix.postScale(3f, 3f) // Увеличиваем в 3 раза
                captchaImage.imageMatrix = matrix
                
            } catch (e: Exception) {
                Log.e("CaptchaDialog", "Error loading captcha image", e)
                captchaImage.setImageResource(R.drawable.ic_error)
            }
        }
    }
}

// GameActivity.kt - Активность с игрой в WebView
class GameActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var userConfig: UserConfig
    
    companion object {
        private const val GAME_URL = "https://www.neverlands.ru/game/"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        
        userConfig = intent.getParcelableExtra("user_config") ?: return
        val cookies = intent.getStringExtra("cookies") ?: ""
        
        title = "AB Game - ${userConfig.userNick}"
        
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        
        setupWebView()
        setupCookies(cookies)
        loadGame()
    }
    
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = USER_AGENT
            
            // Для совместимости со старыми сайтами
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }
            
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                progressBar.visibility = View.GONE
                
                // Показываем ошибку пользователю
                Toast.makeText(this@GameActivity, "Ошибка загрузки игры", Toast.LENGTH_LONG).show()
            }
            
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                // Позволяем переходы только в пределах игрового домена
                val url = request?.url.toString()
                return if (url.contains("neverlands.ru")) {
                    false // Загружаем в WebView
                } else {
                    // Внешние ссылки открываем в браузере
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    true
                }
            }
        }
        
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
            }
            
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                this@GameActivity.title = title ?: "AB Game"
            }
        }
    }
    
    private fun setupCookies(cookies: String) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        
        // Устанавливаем cookies для домена игры
        cookies.split(";").forEach { cookie ->
            val cleanCookie = cookie.trim()
            if (cleanCookie.isNotEmpty()) {
                cookieManager.setCookie("www.neverlands.ru", cleanCookie)
                cookieManager.setCookie(".neverlands.ru", cleanCookie)
            }
        }
        
        cookieManager.flush()
    }
    
    private fun loadGame() {
        try {
            webView.loadUrl(GAME_URL)
        } catch (e: Exception) {
            Log.e("GameActivity", "Error loading game", e)
            Toast.makeText(this, "Ошибка загрузки игры: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.game_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                webView.reload()
                true
            }
            R.id.action_show_cookies -> {
                showCookiesDialog()
                true
            }
            R.id.action_clear_cache -> {
                clearCache()
                true
            }
            R.id.action_exit -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showCookiesDialog() {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie("www.neverlands.ru") ?: "Cookies не найдены"
        
        AlertDialog.Builder(this)
            .setTitle("Cookies игры")
            .setMessage(cookies)
            .setPositiveButton("Копировать") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Game Cookies", cookies)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Cookies скопированы", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }
    
    private fun clearCache() {
        webView.clearCache(true)
        webView.clearHistory()
        CookieManager.getInstance().removeAllCookies(null)
        
        Toast.makeText(