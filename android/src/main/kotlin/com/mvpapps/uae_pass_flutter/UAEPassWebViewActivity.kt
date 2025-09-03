package com.mvpapps.uae_pass_flutter

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

class UAEPassWebViewActivity : Activity() {

    private lateinit var webView: WebView
    private var authUrl: String? = null
    private var redirectUri: String? = null
    private var scheme: String? = null

    companion object {
        const val EXTRA_AUTH_URL = "auth_url"
        const val EXTRA_REDIRECT_URI = "redirect_uri"
        const val EXTRA_SCHEME = "scheme"
        const val RESULT_CODE_SUCCESS = "success_code"
        const val RESULT_CODE_ERROR = "error_message"
        const val RESULT_CODE_CANCELLED = "cancelled"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up action bar with back button only - no logo
        actionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setDisplayUseLogoEnabled(false)
            setDisplayShowCustomEnabled(false)
            setIcon(null)
            setLogo(null)
            title = "UAE PASS"
            setHomeButtonEnabled(true)
        }

        // Get data from intent
        authUrl = intent.getStringExtra(EXTRA_AUTH_URL)
        redirectUri = intent.getStringExtra(EXTRA_REDIRECT_URI)
        scheme = intent.getStringExtra(EXTRA_SCHEME)

        // Set up WebView
        setupWebView()

        // Load the authentication URL
        authUrl?.let { webView.loadUrl(it) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Handle back button press
                setResult(RESULT_CANCELED, Intent().apply {
                    putExtra(RESULT_CODE_CANCELLED, "Authentication Process Canceled By User")
                })
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            // Same as toolbar back button
            setResult(RESULT_CANCELED, Intent().apply {
                putExtra(RESULT_CODE_CANCELLED, "Authentication Process Canceled By User")
            })
            super.onBackPressed()
        }
    }

    private fun setupWebView() {
        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowContentAccess = true
            allowFileAccess = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let { checkUrlForRedirect(it) }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    if (checkUrlForRedirect(it)) {
                        return true
                    }
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { checkUrlForRedirect(it) }
            }
        }
    }

    private fun checkUrlForRedirect(url: String): Boolean {
        // Check for success redirect
        if (url.contains(redirectUri ?: "") && url.contains("code=")) {
            val uri = Uri.parse(url)
            val code = uri.getQueryParameter("code")
            if (!code.isNullOrEmpty()) {
                setResult(RESULT_OK, Intent().apply {
                    putExtra(RESULT_CODE_SUCCESS, code)
                })
                finish()
                return true
            }
        }

        // Check for error or cancellation
        if (url.contains("error=access_denied") || url.contains("error=cancelled")) {
            setResult(RESULT_CANCELED, Intent().apply {
                putExtra(RESULT_CODE_CANCELLED, "Authentication Process Canceled By User")
            })
            finish()
            return true
        }

        // Check for custom scheme (UAE Pass app launch)
        if (url.startsWith("uaepass://")) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "UAE Pass app not found", Toast.LENGTH_SHORT).show()
            }
            return true
        }

        return false
    }

    fun forceReload() {
        webView.reload()
    }

    fun foreceStop() {
        webView.stopLoading()
    }
}
