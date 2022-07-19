package com.truv

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.truv.models.TruvEventPayload
import org.json.JSONObject

class TruvBridgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val eventListeners = mutableSetOf<TruvEventsListener>()

    fun addEventListener(listener: TruvEventsListener) {
        eventListeners.add(listener)
    }

    fun removeEventListener(listener: TruvEventsListener): Boolean {
        return eventListeners.remove(listener)
    }

    private val webView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.allowContentAccess = true
        settings.domStorageEnabled = true
        webViewClient = TruvWebViewClient()
        addJavascriptInterface(WebAppInterface(), "citadelInterface")
    }

    init {
        addView(webView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    val currentUrl: String?
        get() = webView.url

    fun hasBridgeToken(token: String): Boolean = webView.url?.contains(token) == true

    fun loadBridgeTokenUrl(bridgeToken: String) {
        webView.loadUrl("https://cdn.truv.com/mobile.html?bridge_token=$bridgeToken")
    }

    private inner class TruvWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = request?.url

            context.startActivity(i)
            return true
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            eventListeners.forEach { it.onClose() }
        }

    }

    private inner class WebAppInterface {

        @JavascriptInterface
        fun onSuccess(payload: String) {
            Log.d(TAG, "onSuccess invoked $payload")
            eventListeners.forEach { it.onSuccess() }
        }

        @JavascriptInterface
        fun onEvent(event: String) {
            Log.d(TAG, "onEvent invoked $event")

            val json = JSONObject(event)
            val type = json.getString("event_type")

            eventListeners.forEach { it.onEvent(TruvEventPayload.EventType.valueOf(type)) }
        }

        @JavascriptInterface
        fun onClose() {
            Log.d(TAG, "onClose invoked")
            eventListeners.forEach { it.onClose() }
        }

        @JavascriptInterface
        fun onLoad() {
            Log.d(TAG, "onLoad invoked")
            eventListeners.forEach { it.onLoad() }
        }

        @JavascriptInterface
        fun onError() {
            Log.d(TAG, "onError invoked")
            eventListeners.forEach { it.onError() }
        }

    }

    companion object {
        const val TAG = "TruvBridgeView"
    }

}