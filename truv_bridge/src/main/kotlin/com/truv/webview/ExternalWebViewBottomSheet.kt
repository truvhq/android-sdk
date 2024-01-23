package com.truv.webview

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.truv.R
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
    @StyleRes styleRes: Int,
    private val eventListeners: Set<TruvEventsListener>
) : BottomSheetDialog(context, styleRes) {

    var config: ExternalLoginConfig? = null
        set(value) {
            field = value
            if (value != null) {
                findWebView()?.loadUrl(value.url)
                findRefresher()?.setOnClickListener {
                    findWebView()?.reload()
                }
                findTitle()?.text = value.url
                startRefreshTimer()
            }
        }

    private fun initWebView() = with(findWebView()!!) {
        settings.javaScriptEnabled = true
        settings.allowContentAccess = true
        settings.domStorageEnabled = true
        setBackgroundColor(Color.WHITE)
        webViewClient = TruvWebViewClient(context, eventListeners, onLoaded = {
//            TODO: uncomment when script will be ready
//            config?.script?.let { script ->
//                runBlocking {
//                    applyScript(script)
//                }
//            }
        }, onLoading = {
            findWebView()?.isVisible = !it
            findProgressBar()?.isVisible = it
        })
        //https://www.whatismybrowser.com/guides/the-latest-user-agent/chrome
        val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.210 Mobile Safari/537.36"
        settings.userAgentString = USER_AGENT;
        addJavascriptInterface(WebAppInterface(eventListeners), Constants.CITADEL_INTERFACE)
        addJavascriptInterface(MiddleWareInterface {
            val responseDto = MiddleWareResponseDto.parse(JSONObject(it))
            performRequest(it)
        }, "callbackInterface")
    }

    private fun performRequest(responseString: String) {
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
            findWebView()?.handler?.post {
                findWebView()?.evaluateJavascript(scriptText, null)
            }
        }
    }

    private val timerRunnable = Runnable {
        val selector = config?.selector?.replace("\"", "'")

        findWebView()?.evaluateJavascript(
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
        findWebView()?.handler?.removeCallbacks(timerRunnable)
        findWebView()?.handler?.postDelayed(timerRunnable, DELAY_MILLIS)
    }

    fun setContentView() {
        val contentView = layoutInflater.inflate(R.layout.external_webview_bottom_sheet, null)
        this.setOnShowListener { dialog ->
            val bottomSheet = (dialog as? BottomSheetDialog)?.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )

            if (bottomSheet != null) {
                val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
                val windowHeight =
                    (contentView.resources.displayMetrics.heightPixels * 0.90).toInt()
                bottomSheetBehavior.peekHeight = windowHeight
                contentView.layoutParams.height = windowHeight
                bottomSheetBehavior.addBottomSheetCallback(object :
                    BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            dismiss()
                        }

                        if (newState != BottomSheetBehavior.STATE_EXPANDED
                            && newState != BottomSheetBehavior.STATE_DRAGGING
                            && newState != BottomSheetBehavior.STATE_SETTLING
                        ) {
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {

                    }
                })
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        super.setContentView(contentView)
        initWebView()
    }

    override fun dismiss() {
        findWebView()?.handler?.removeCallbacks(timerRunnable)
        super.dismiss()
    }

    fun findWebView(): WebView? = findViewById(R.id.webview)
    fun findProgressBar(): View? = findViewById(R.id.progress_bar)
    fun findTitle(): TextView? = findViewById(R.id.title)
    fun findRefresher(): ImageView? = findViewById(R.id.refresher)

    companion object {
        private const val DELAY_MILLIS = 2000L
    }

}