package com.truv

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.truv.models.TruvEventPayload
import com.truv.models.TruvSuccessPayload
import org.json.JSONException

class TruvBridgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val eventListeners = mutableSetOf<TruvEventsListener>()
    private val baseUrl = "https://cdn.truv.com/mobile.html"

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
        setOnKeyListener { _, keyCode, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                evaluateJavascript("window.bridge?.back();") {
                    Log.d(TAG, "On Back pressed")
                }
                return@setOnKeyListener true
            }
            return@setOnKeyListener super.onKeyDown(keyCode, keyEvent)
        }
    }

    init {
        addView(webView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    val currentUrl: String?
        get() = webView.url

    fun hasBridgeToken(token: String): Boolean = webView.url?.contains(token) == true

    fun loadBridgeTokenUrl(bridgeToken: String) {
        webView.loadUrl("$baseUrl?bridge_token=$bridgeToken")
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
            
            try {
                val successPayload = TruvSuccessPayload.fromJson(payload)
                eventListeners.forEach { it.onSuccess(successPayload) }
            } catch (e: JSONException) {
                Log.e(TAG, "Json exception at onSuccess invoked $payload", e)
                eventListeners.forEach { it.onError() }
            }
        }

        @JavascriptInterface
        fun onEvent(event: String) {
            Log.d(TAG, "onEvent invoked $event")

            try {
                val eventPayload = TruvEventPayload.fromJson(event)
                eventListeners.forEach { it.onEvent(eventPayload) }
            } catch (e: JSONException) {
                Log.e(TAG, "Json exception at onEvent invoked $event", e)
            }
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