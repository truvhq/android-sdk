package com.truv.webview

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.WebView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.truv.BuildConfig
import com.truv.R
import com.truv.models.ExternalLoginConfig
import com.truv.models.TruvEventPayload
import com.truv.models.TruvSuccessPayload
import com.truv.webview.models.Cookie
import org.json.JSONArray
import org.json.JSONObject

class TruvBridgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : CoordinatorLayout(context, attrs) {

    private val eventListeners = mutableSetOf<TruvEventsListener>()

    private val bottomSheetEventListener: TruvEventsListener by lazy {
        object : TruvEventsListener {

            override fun onSuccess(payload: TruvSuccessPayload) {
                externalWebViewBottomSheet.dismiss()

                val dataWithTags = JSONObject(payload.json)
                val tags = JSONArray().apply {
                    put("platform:android")
                    put("sdk-version:${BuildConfig.VERSION_NAME}")
                    put("source:android-sdk")
                }

                dataWithTags.put("tags", tags)

                webView.evaluateJavascript(
                    String.format(
                        Constants.SCRIPT_EXTERNAL_LOGIN_SUCCESS,
                        dataWithTags.toString()
                    )
                ) { result ->
                    Log.d(TAG, "On External success: $result")
                }

                externalWebViewBottomSheet.dismiss()
            }

            override fun onEvent(event: TruvEventPayload) {
                Log.d(TAG, "On External event: ${event.eventType}")
            }

            override fun onClose() {
                Log.d(TAG, "On External load")
            }

            override fun onLoad() {
                Log.d(TAG, "On External load")
            }

            override fun onError() {
                Log.d(TAG, "On External error")
            }

        }
    }

    private val externalWebViewBottomSheet: ExternalWebViewBottomSheet by lazy {
        ExternalWebViewBottomSheet(
            context = context,
            styleRes = R.style.BottomSheetDialogHandleOutside,
            eventListeners = setOf(bottomSheetEventListener),
            onCookie = { cookies, pageUrl ->
                sendCookies(cookies, pageUrl)
            }).apply {
            setOnDismissListener {
                webView.evaluateJavascript(Constants.SCRIPT_EXTERNAL_LOGIN_CANCEL) { result ->
                    Log.d(TAG, "On External closed: $result")
                }
            }
        }
    }

    private fun sendCookies(
        cookies: List<Cookie>,
        pageUrl: String
    ) {
        val data = JSONObject().apply {
            put("cookies", JSONArray().apply {
                cookies.forEach {
                    put(it.toJson())
                }
            })
            put("dashboard_url", pageUrl)
        }
        webView.evaluateJavascript(
            String.format(
                Constants.SCRIPT_EXTERNAL_LOGIN_SUCCESS,
                data.toString()
            )
        ) { result ->
            Log.d(TAG, "On External cookie: $result")
        }
    }

    private val webView = WebView(context).apply {
        id = View.generateViewId()
        settings.javaScriptEnabled = true
        settings.allowContentAccess = true
        settings.domStorageEnabled = true
        webViewClient = TruvWebViewClient(context, eventListeners)
        addJavascriptInterface(WebAppInterface(eventListeners) {
            Log.d("TruvBridgeView", "Open external login")
            showExternalWebView(config = it)
        }, Constants.CITADEL_INTERFACE)
        setOnKeyListener { _, keyCode, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                evaluateJavascript(Constants.SCRIPT_BACK_PRESS) {
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

    private fun showExternalWebView(config: ExternalLoginConfig) {
        post {
            externalWebViewBottomSheet.setContentView()
            externalWebViewBottomSheet.show()
            externalWebViewBottomSheet.config = config
        }
    }

    val currentUrl: String?
        get() = webView.url

    fun addEventListener(listener: TruvEventsListener) {
        eventListeners.add(listener)
    }

    fun removeEventListener(listener: TruvEventsListener): Boolean {
        return eventListeners.remove(listener)
    }

    fun hasBridgeToken(token: String): Boolean = webView.url?.contains(token) == true

    fun loadBridgeTokenUrl(bridgeToken: String) {
        webView.loadUrl(String.format(Constants.BRIDGE_URL, bridgeToken))
    }

    companion object {
        const val TAG = "TruvBridgeView"
    }

}