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
                bottomSheetWebView.dismiss()

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

    private val bottomSheetWebView: ExternalWebViewBottomSheet by lazy {
        ExternalWebViewBottomSheet(context, R.style.BottomSheetDialogHandleOutside, setOf(bottomSheetEventListener)).apply {
            setOnDismissListener {
                webView.evaluateJavascript(Constants.SCRIPT_EXTERNAL_LOGIN_CANCEL) { result ->
                    Log.d(TAG, "On External closed: $result")
                }
            }
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
            bottomSheetWebView.setContentView()
            bottomSheetWebView.show()
            bottomSheetWebView.config = config
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