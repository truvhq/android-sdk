package com.truv.webview

import android.util.Log
import android.webkit.JavascriptInterface
import com.truv.models.ExternalLoginConfig
import com.truv.models.TruvEventPayload
import com.truv.models.TruvSuccessPayload
import org.json.JSONException

class WebAppInterface(
    private val eventListeners: Set<TruvEventsListener>,
    private val onShowExternalWebView: ((ExternalLoginConfig) -> Unit)? = null
) {

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
            if (eventPayload.eventType == TruvEventPayload.EventType.START_EXTERNAL_LOGIN
                && onShowExternalWebView != null && eventPayload.payload?.externalLoginConfig != null) {

                val config = eventPayload.payload.externalLoginConfig
                onShowExternalWebView.invoke(config)
            } else {
                eventListeners.forEach { it.onEvent(eventPayload) }
            }
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

    companion object {
        const val TAG = "TruvBridgeView"
    }

}