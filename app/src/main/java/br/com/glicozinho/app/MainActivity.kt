package br.com.glicozinho.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import java.io.OutputStreamWriter

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var pendingBackupJson: String? = null

    companion object {
        private const val FILE_PICKER_REQUEST = 4101
        private const val SAVE_BACKUP_REQUEST = 4102
    }

    inner class GlicozinhoBridge {
        @JavascriptInterface
        fun saveBackup(json: String) {
            runOnUiThread {
                pendingBackupJson = json
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, "glicozinho-backup.json")
                }
                startActivityForResult(intent, SAVE_BACKUP_REQUEST)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.settings.setSupportZoom(false)
        webView.settings.builtInZoomControls = false
        webView.addJavascriptInterface(GlicozinhoBridge(), "GlicozinhoNative")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url ?: return false
                return url.scheme != "file" && url.scheme != "about"
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
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                }
                startActivityForResult(intent, FILE_PICKER_REQUEST)
                return true
            }
        }

        if (savedInstanceState == null) {
            webView.loadUrl("file:///android_asset/index.html")
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    @Deprecated("Activity result API kept dependency-free for this starter project")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FILE_PICKER_REQUEST -> {
                val result = if (resultCode == RESULT_OK && data?.data != null) {
                    arrayOf(data.data!!)
                } else null
                fileChooserCallback?.onReceiveValue(result)
                fileChooserCallback = null
            }
            SAVE_BACKUP_REQUEST -> {
                val uri = if (resultCode == RESULT_OK) data?.data else null
                val json = pendingBackupJson
                pendingBackupJson = null
                if (uri != null && json != null) {
                    try {
                        contentResolver.openOutputStream(uri)?.use { stream ->
                            OutputStreamWriter(stream, Charsets.UTF_8).use { it.write(json) }
                        }
                        Toast.makeText(this, "Backup salvo.", Toast.LENGTH_LONG).show()
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
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.removeJavascriptInterface("GlicozinhoNative")
            webView.destroy()
        }
        super.onDestroy()
    }
}
