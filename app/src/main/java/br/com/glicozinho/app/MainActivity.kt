package br.com.glicozinho.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.view.WindowInsets
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.widget.Toast
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var pendingBackupBytes: ByteArray? = null
    private var webReady = false
    private var navigationBarInsetDp = 0f

    private fun applyWebSafeArea() {
        if (!::webView.isInitialized) return
        val inset = navigationBarInsetDp.toString()
        webView.post {
            webView.evaluateJavascript("document.documentElement.style.setProperty('--android-nav-inset', '${inset}px')", null)
        }
    }

    companion object {
        private const val FILE_PICKER_REQUEST = 4101
        private const val SAVE_BACKUP_REQUEST = 4102
        private const val NOTIFICATION_PERMISSION_REQUEST = 4103
        private const val LOCAL_KEY_ALIAS = "glicozinho_local_data_aes_v1"
        private const val BACKUP_ITERATIONS = 210_000
        private const val MAX_DATA_BYTES = 2 * 1024 * 1024
        private val APPOINTMENT_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm").withResolverStyle(ResolverStyle.STRICT)
        private const val LOCAL_MAGIC = "GLC1"
    }

    inner class GlicozinhoBridge {
        @JavascriptInterface
        fun hasBundledTrack(path: String): Boolean {
            val allowed = setOf(
                "music/hino-oficial.mp3", "music/bando-de-loucos.mp3",
                "music/nunca-vou-te-abandonar.mp3", "music/louco-por-ti.mp3",
                "music/minha-vida-minha-historia.mp3"
            )
            if (path !in allowed) return false
            return try { assets.open(path).use { true } } catch (_: Exception) { false }
        }


        @JavascriptInterface
        fun loadData(): String = try {
            val file = File(filesDir, "glicozinho-data.enc")
            if (!file.exists()) JSONObject().put("ok", true).put("data", "").toString()
            else JSONObject().put("ok", true).put("data", decryptLocal(file.readBytes())).toString()
        } catch (_: Exception) {
            JSONObject().put("ok", false).put("data", "").toString()
        }

        @JavascriptInterface
        fun saveData(json: String): Boolean {
            val temp = File(filesDir, "glicozinho-data.enc.tmp")
            return try {
                if (json.toByteArray(StandardCharsets.UTF_8).size > MAX_DATA_BYTES) return false
                val root = JSONObject(json)
                val limits = mapOf("glucose" to 5000, "insulin" to 5000, "meds" to 1000,
                    "medEvents" to 10000, "routine" to 5000, "appointments" to 5000)
                for ((key, limit) in limits) {
                    val array = root.optJSONArray(key) ?: if (key == "medEvents") JSONArray() else return false
                    if (array.length() > limit) return false
                }
                if (!validAppointments(root.optJSONArray("appointments") ?: JSONArray())) return false
                val encrypted = encryptLocal(json)
                val target = File(filesDir, "glicozinho-data.enc")
                FileOutputStream(temp).use { out -> out.write(encrypted); out.fd.sync() }
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                true
            } catch (_: Exception) {
                runCatching { temp.delete() }
                false
            }
        }

        @JavascriptInterface
        fun saveBackup(json: String, passphrase: String): Boolean {
            if (passphrase.length !in 12..128 || json.toByteArray(StandardCharsets.UTF_8).size > MAX_DATA_BYTES) return false
            val bytes = try { encryptBackup(json, passphrase) } catch (_: Exception) { return false }
            runOnUiThread {
                pendingBackupBytes = bytes
                try {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/octet-stream"
                        putExtra(Intent.EXTRA_TITLE, "glicozinho-backup.glico")
                    }
                    startActivityForResult(intent, SAVE_BACKUP_REQUEST)
                } catch (_: Exception) {
                    pendingBackupBytes = null
                    Toast.makeText(this@MainActivity, "Não foi possível abrir o seletor para salvar o backup.", Toast.LENGTH_LONG).show()
                }
            }
            return true
        }

        @JavascriptInterface
        fun decryptBackup(encoded: String, passphrase: String): String {
            return try {
                if (passphrase.length !in 12..128 || encoded.length > 4 * 1024 * 1024) return ""
                val json = decryptBackupText(encoded, passphrase)
                JSONObject(json) // garante que a carga é JSON objeto antes de devolvê-la ao JavaScript
                json
            } catch (_: Exception) { "" }
        }

        @JavascriptInterface
        fun requestNotificationPermission(): String {
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                runOnUiThread { requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST) }
                return "permission_requested"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (!manager.areNotificationsEnabled() ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    manager.getNotificationChannel("glicozinho_routine_reminders")?.importance == android.app.NotificationManager.IMPORTANCE_NONE)) {
                runOnUiThread { openNotificationSettings() }
                return "settings_requested"
            }
            return "ready"
        }

        @JavascriptInterface
        fun areNotificationsEnabled(): Boolean {
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
            val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            return manager.areNotificationsEnabled() &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                    manager.getNotificationChannel("glicozinho_routine_reminders")?.importance != android.app.NotificationManager.IMPORTANCE_NONE)
        }

        @JavascriptInterface
        fun scheduleReminder(id: String, triggerAtMillis: Long, daily: Boolean): String {
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return "permission_required"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (!manager.areNotificationsEnabled() ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    manager.getNotificationChannel("glicozinho_routine_reminders")?.importance == android.app.NotificationManager.IMPORTANCE_NONE)) return "permission_required"
            return ReminderScheduler.schedule(
                this@MainActivity,
                ReminderScheduler.Reminder(id, triggerAtMillis, daily)
            )
        }

        @JavascriptInterface
        fun scheduleAppointmentReminder(id: String, triggerAtMillis: Long, exact: Boolean): String {
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return "permission_required"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (!manager.areNotificationsEnabled() ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    manager.getNotificationChannel("glicozinho_routine_reminders")?.importance == android.app.NotificationManager.IMPORTANCE_NONE)) {
                return "permission_required"
            }
            return ReminderScheduler.schedule(
                this@MainActivity,
                ReminderScheduler.Reminder(id, triggerAtMillis, daily = false, exact = exact)
            )
        }

        @JavascriptInterface
        fun requestExactAlarmAccess(): String {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return "granted"
            val manager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (manager.canScheduleExactAlarms()) return "granted"
            runOnUiThread {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).setData(Uri.parse("package:$packageName"))
                    startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(this@MainActivity, "Abra Configurações > Apps > Acesso especial > Alarmes e lembretes.", Toast.LENGTH_LONG).show()
                }
            }
            return "opened_settings"
        }
        @JavascriptInterface
        fun cancelReminder(id: String): Boolean = ReminderScheduler.cancel(this@MainActivity, id)

        @JavascriptInterface
        fun cancelAllReminders(): Boolean = runCatching {
            ReminderScheduler.cancelAll(this@MainActivity)
            true
        }.getOrDefault(false)

        @JavascriptInterface
        fun isReminderScheduled(id: String): Boolean = ReminderScheduler.isScheduled(this@MainActivity, id)

        @JavascriptInterface
        fun isReminderScheduledAt(id: String, triggerAtMillis: Long, exact: Boolean): Boolean =
            ReminderScheduler.isScheduledAt(this@MainActivity, id, triggerAtMillis, exact)

        @JavascriptInterface
        fun deleteAllData(): Boolean {
            return try {
                // Cancela alarmes antes da remoção dos registros para evitar estado parcialmente apagado.
                ReminderScheduler.cancelAll(this@MainActivity)
                val dataFile = File(filesDir, "glicozinho-data.enc")
                val tempFile = File(filesDir, "glicozinho-data.enc.tmp")
                if (dataFile.exists() && !dataFile.delete()) return false
                if (tempFile.exists() && !tempFile.delete()) return false
                val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                if (store.containsAlias(LOCAL_KEY_ALIAS)) store.deleteEntry(LOCAL_KEY_ALIAS)
                true
            } catch (_: Exception) { false }
        }

        @JavascriptInterface
        fun openNotificationSettings() {
            runOnUiThread {
                try {
                    val intent = if (Build.VERSION.SDK_INT >= 26) {
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                    startActivity(intent)
                } catch (_: Exception) {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barras do sistema sólidas, com ícones escuros e conteúdo respeitando a área segura.
        window.statusBarColor = android.graphics.Color.rgb(239, 250, 248)
        window.navigationBarColor = android.graphics.Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        webView = WebView(this)
        webView.setBackgroundColor(android.graphics.Color.rgb(243, 250, 249))
        setContentView(webView)
        createNotificationChannel()
        // Revalida e restaura somente metadados mínimos dos lembretes após abrir o app.
        ReminderScheduler.rescheduleAll(this)

        // O Android gerencia as áreas seguras. Padding manual no WebView alterava
        // o viewport dos elementos position:fixed, inclusive a navegação inferior.
        // Android 15+ força edge-to-edge para apps com targetSdk recente.
        // Passamos a altura real da barra de navegação ao CSS para manter a barra
        // do aplicativo acima dos botões/gestos do sistema, sem sobreposição.
        webView.setOnApplyWindowInsetsListener { view, insets ->
            val bottomPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            } else {
                insets.systemWindowInsetBottom
            }
            navigationBarInsetDp = bottomPx / resources.displayMetrics.density
            view.post { applyWebSafeArea() }
            insets
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.settings.allowFileAccessFromFileURLs = false
        webView.settings.allowUniversalAccessFromFileURLs = false
        webView.settings.javaScriptCanOpenWindowsAutomatically = false
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        webView.settings.blockNetworkLoads = true
        webView.settings.safeBrowsingEnabled = true
        webView.settings.setSupportZoom(false)
        webView.settings.builtInZoomControls = false
        webView.addJavascriptInterface(GlicozinhoBridge(), "GlicozinhoNative")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url ?: return true
                // O app só navega em seus arquivos locais empacotados; não abre páginas externas dentro do WebView.
                return !(url.scheme == "file" && (url.path ?: "").startsWith("/android_asset/"))
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webReady = true
                applyWebSafeArea()
                view?.evaluateJavascript("window.onGlicozinhoNativeResume && window.onGlicozinhoNativeResume();", null)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.fileChooserCallback?.onReceiveValue(null)
                this@MainActivity.fileChooserCallback = filePathCallback
                val accepted = fileChooserParams?.acceptTypes
                    ?.flatMap { it.split(",") }
                    ?.map { it.trim() }
                    ?.filter { it.contains("/") }
                    ?.distinct()
                    .orEmpty()
                val chooserMime = when {
                    accepted.isEmpty() -> "*/*"
                    accepted.size == 1 -> accepted.first()
                    else -> "*/*"
                }
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = chooserMime
                    if (accepted.size > 1) putExtra(Intent.EXTRA_MIME_TYPES, accepted.toTypedArray())
                }
                try {
                    startActivityForResult(intent, FILE_PICKER_REQUEST)
                } catch (_: Exception) {
                    fileChooserCallback?.onReceiveValue(null)
                    fileChooserCallback = null
                    Toast.makeText(this@MainActivity, "Não foi possível abrir os arquivos deste aparelho.", Toast.LENGTH_LONG).show()
                }
                return true
            }
        }

        if (savedInstanceState == null) webView.loadUrl("file:///android_asset/index.html")
        else webView.restoreState(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (webReady && ::webView.isInitialized) {
            webView.evaluateJavascript("window.onGlicozinhoNativeResume && window.onGlicozinhoNativeResume();", null)
        }
    }

    private fun validAppointments(items: JSONArray): Boolean {
        val ids = HashSet<String>()
        val allowedOffsets = setOf(-1, 0, 10, 30, 60, 1440)
        val allowedTypes = setOf("consultation", "exam", "other")
        val allowedPrecision = setOf("approximate", "exact")
        return try {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: return false
                if (item.opt("id") !is String || item.opt("title") !is String || item.opt("type") !is String ||
                    item.opt("when") !is String || item.opt("reminderMinutes") !is Number || item.opt("precision") !is String) return false
                val id = item.optString("id", "")
                val title = item.optString("title", "").trim()
                val type = item.optString("type", "")
                val whenText = item.optString("when", "")
                val reminderNumber = item.opt("reminderMinutes") as Number
                val reminderMinutes = reminderNumber.toInt()
                val precision = item.optString("precision", "")
                val reminderAtValue = item.opt("reminderAt")
                if (reminderAtValue != null && reminderAtValue !is String) return false
                val reminderAt = reminderAtValue as? String ?: ""
                if (reminderNumber.toDouble() != reminderMinutes.toDouble() || id.isBlank() || id.length > 100 ||
                    !ids.add(id) || title.isBlank() || title.length > 120 || type !in allowedTypes ||
                    reminderMinutes !in allowedOffsets || precision !in allowedPrecision ||
                    (item.has("professional") && item.opt("professional") !is String) ||
                    (item.has("location") && item.opt("location") !is String) ||
                    (item.has("notes") && item.opt("notes") !is String) ||
                    item.optString("professional", "").length > 120 || item.optString("location", "").length > 160 ||
                    item.optString("notes", "").length > 500 || (item.has("done") && item.opt("done") !is Boolean)) return false
                LocalDateTime.parse(whenText, APPOINTMENT_FORMATTER)
                if (reminderMinutes < 0) {
                    if (reminderAt.isNotEmpty()) return false
                } else {
                    Instant.parse(reminderAt)
                }
            }
            true
        } catch (_: Exception) { false }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.createNotificationChannel(
                android.app.NotificationChannel(
                    "glicozinho_routine_reminders", "Lembretes de rotina", android.app.NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Lembretes de rotina escolhidos pela pessoa usuária" }
            )
        }
    }

    private fun localKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(LOCAL_KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                LOCAL_KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encryptLocal(plain: String): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, localKey())
        val encrypted = cipher.doFinal(plain.toByteArray(StandardCharsets.UTF_8))
        val iv = cipher.iv
        val header = LOCAL_MAGIC.toByteArray(StandardCharsets.US_ASCII)
        return ByteBuffer.allocate(header.size + 1 + iv.size + encrypted.size)
            .put(header).put(iv.size.toByte()).put(iv).put(encrypted).array()
    }

    private fun decryptLocal(bytes: ByteArray): String {
        if (bytes.size < 4 + 1 + 12 + 16 || String(bytes, 0, 4, StandardCharsets.US_ASCII) != LOCAL_MAGIC) {
            throw IllegalArgumentException("Arquivo local inválido")
        }
        val buffer = ByteBuffer.wrap(bytes)
        buffer.position(4)
        val ivLength = buffer.get().toInt() and 0xff
        if (ivLength !in 12..16 || buffer.remaining() <= ivLength) throw IllegalArgumentException("IV inválido")
        val iv = ByteArray(ivLength).also { buffer.get(it) }
        val ciphertext = ByteArray(buffer.remaining()).also { buffer.get(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, localKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
    }

    private fun encryptBackup(json: String, passphrase: String): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val key = deriveBackupKey(passphrase, salt, BACKUP_ITERATIONS)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(json.toByteArray(StandardCharsets.UTF_8))
        val out = JSONObject().apply {
            put("format", "GLICozinho-backup-v1")
            put("iterations", BACKUP_ITERATIONS)
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("data", Base64.encodeToString(encrypted, Base64.NO_WRAP))
        }
        return out.toString().toByteArray(StandardCharsets.UTF_8)
    }

    private fun decryptBackupText(encoded: String, passphrase: String): String {
        val envelope = JSONObject(encoded)
        if (envelope.optString("format") != "GLICozinho-backup-v1") throw IllegalArgumentException("Formato inválido")
        val iterations = envelope.optInt("iterations", 0)
        if (iterations != BACKUP_ITERATIONS) throw IllegalArgumentException("Parâmetros inválidos")
        val salt = Base64.decode(envelope.getString("salt"), Base64.DEFAULT)
        val iv = Base64.decode(envelope.getString("iv"), Base64.DEFAULT)
        val data = Base64.decode(envelope.getString("data"), Base64.DEFAULT)
        if (salt.size != 16 || iv.size != 12 || data.size !in 17..(MAX_DATA_BYTES + 16)) {
            throw IllegalArgumentException("Backup inválido")
        }
        val key = deriveBackupKey(passphrase, salt, iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return String(cipher.doFinal(data), StandardCharsets.UTF_8)
    }

    private fun deriveBackupKey(passphrase: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterations, 256)
        return try {
            val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            SecretKeySpec(raw, "AES")
        } finally { spec.clearPassword() }
    }

    @Deprecated("Activity result API kept dependency-free for this starter project")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FILE_PICKER_REQUEST -> {
                val result = if (resultCode == RESULT_OK && data?.data != null) arrayOf(data.data!!) else null
                fileChooserCallback?.onReceiveValue(result)
                fileChooserCallback = null
            }
            SAVE_BACKUP_REQUEST -> {
                val uri = if (resultCode == RESULT_OK) data?.data else null
                val bytes = pendingBackupBytes
                pendingBackupBytes = null
                if (uri != null && bytes != null) {
                    try {
                        val output = contentResolver.openOutputStream(uri)
                            ?: throw IllegalStateException("Não foi possível abrir o arquivo de destino.")
                        output.use { it.write(bytes); it.flush() }
                        Toast.makeText(this, "Backup criptografado salvo.", Toast.LENGTH_LONG).show()
                    } catch (_: Exception) {
                        Toast.makeText(this, "Não foi possível salvar o backup.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Back navigation support")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        fileChooserCallback?.onReceiveValue(null)
        fileChooserCallback = null
        pendingBackupBytes = null
        if (::webView.isInitialized) {
            webView.removeJavascriptInterface("GlicozinhoNative")
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
