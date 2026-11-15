package fr.clrk.reveil.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import fr.clrk.reveil.core.YtMusicSession
import fr.clrk.reveil.music.YtMusicAccount

/** Lets the user sign in to music.youtube.com in a web page; keeps only the session cookies. */
class LoginActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookieManager.getInstance().removeAllCookies(null)
        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) = capture(url)
        }
        setContentView(web)
        web.loadUrl(SIGN_IN)
    }

    private fun capture(url: String) {
        if (!url.startsWith(YtMusicSession.ORIGIN)) return
        val cookies = CookieManager.getInstance().getCookie(YtMusicSession.ORIGIN) ?: return
        if (YtMusicAccount(this).signIn(cookies)) {
            setResult(RESULT_OK)
            finish()
        }
    }

    private companion object {
        const val SIGN_IN = "https://accounts.google.com/ServiceLogin?service=youtube&continue=https%3A%2F%2Fmusic.youtube.com%2F"
    }
}
