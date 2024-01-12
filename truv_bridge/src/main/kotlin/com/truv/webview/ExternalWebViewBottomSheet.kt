package com.truv.webview

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.truv.models.ExternalLoginConfig
import com.truv.models.MiddleWareResponseDto
import com.truv.models.ResponseDto
import com.truv.network.HttpRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class ExternalWebViewBottomSheet(
    context: Context,
    private val eventListeners: Set<TruvEventsListener>
) : BottomSheetDialog(context) {

    var config: ExternalLoginConfig? = null
        set(value) {
            field = value
            if (value != null) {
                webView.loadUrl(value.url)
                startRefreshTimer()
            }
        }

    private val webView = WebView(context).apply {
        id = View.generateViewId()
        settings.javaScriptEnabled = true
        settings.allowContentAccess = true
        settings.domStorageEnabled = true
        setBackgroundColor(Color.WHITE)
        webViewClient = TruvWebViewClient(context, eventListeners, onLoaded = {
            config?.script?.let { script ->
                runBlocking {
                    applyScript(script)
                }
            }
        })
        addJavascriptInterface(WebAppInterface(eventListeners), Constants.CITADEL_INTERFACE)
        addJavascriptInterface(MiddleWareInterface {
            val responseDto = MiddleWareResponseDto.parse(JSONObject(it))
            performRequest(it)
        }, "callbackInterface")
    }

    private fun performRequest(responseString : String) {
        HttpRequest(
            headers = mapOf(
                "Content-Type" to config?.script?.callbackHeaders?.contentType.orEmpty(),
                "X-Access-Token" to config?.script?.callbackHeaders?.xAccessToken.orEmpty(),
            ),
            body = responseString,
            url = config?.script?.callbackUrl.orEmpty()
        ).json<String> { t, httpResponse ->
            Log.d("ExternalWebView", "performRequest: $t")
        }
    }

    private suspend fun applyScript(script: ResponseDto.Payload.Script) {
        withContext(Dispatchers.IO) {
            val scriptText = URL(script.url).readText()
            webView.handler?.post {
                webView.evaluateJavascript(scriptText, null)
            }
        }
    }

    private val timerRunnable = Runnable {
        val selector = config?.selector?.replace("\"", "'")

        webView.evaluateJavascript(
            "          (function() {\n" +
                    "            const getIsElementDisplayed = (element) => {\n" +
                    "              const style = window.getComputedStyle(element);\n" +
                    "              return style && style.visibility !== 'hidden' && hasVisibleBoundingBox();\n" +
                    "              function hasVisibleBoundingBox() {\n" +
                    "                  const rect = element.getBoundingClientRect();\n" +
                    "                  return !!(rect.top || rect.bottom || rect.width || rect.height);\n" +
                    "                  }\n" +
                    "            }\n" +
                    "\n" +
                    "            const selector = \"${selector}\";\n" +
                    "            const isXPath = /^\\\\(*\\\\.*\\\\//.test(selector);\n" +
                    "\n" +
                    "            const element = isXPath ? document.evaluate(selector, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE).singleNodeValue : document.querySelector(selector);\n" +
                    "\n" +
                    "            if (element && getIsElementDisplayed(element)) {\n" +
                    "              window.ReactNativeWebView.postMessage(JSON.stringify({ \"event\": \"logged_in\" }));\n" +
                    "            }\n" +
                    "          })();\n" +
                    "        ",
            null
        )
        startRefreshTimer()
    }

    private fun startRefreshTimer() {
        webView.handler?.removeCallbacks(timerRunnable)
        webView.handler?.postDelayed(timerRunnable, DELAY_MILLIS)
    }

    fun setContentView() {
        this.setOnShowListener { dialog ->
            val bottomSheet = (dialog as? BottomSheetDialog)?.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )?.apply {
                layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
                layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
            }

            if (bottomSheet != null) {
                BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        (webView.parent as? ViewGroup)?.removeAllViews()
        super.setContentView(webView)
    }

    override fun dismiss() {
        webView.handler?.removeCallbacks(timerRunnable)
        super.dismiss()
    }

    companion object {
        private const val DELAY_MILLIS = 2000L
    }

}