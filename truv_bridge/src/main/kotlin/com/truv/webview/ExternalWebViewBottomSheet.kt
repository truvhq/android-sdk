package com.truv.webview

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.truv.R
import com.truv.models.ExternalLoginConfig
import com.truv.models.MiddleWareResponseDto
import com.truv.models.ResponseDto
import com.truv.network.HttpRequest
import com.truv.webview.models.Cookie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class ExternalWebViewBottomSheet(
    context: Context,
    @StyleRes styleRes: Int,
    private val eventListeners: Set<TruvEventsListener>,
    private val onCookie: (cookies: List<Cookie>, pageUrl: String) -> Unit,
) : BottomSheetDialog(context, styleRes) {

    var config: ExternalLoginConfig? = null
        set(value) {
            field = value
            if (value?.url != null) {
                findWebView()?.loadUrl(value.url)
                findErrorRetryButton()?.setOnClickListener {
                    findWebView()?.reload()
                }
                findRefresher()?.setOnClickListener {
                    findWebView()?.reload()
                }
                findTitle()?.text = value.url
                scriptTimer.start()
            }
        }

    val truvWebViewClient by lazy {
        TruvWebViewClient(context, eventListeners, onLoaded = {
            config?.script?.let { script ->
                lifecycleScope.launch {
                    applyScript(script)
                }
            }
        }, onLoading = { isLoading ->
            if (isLoading) {
                findErrorLoading()?.isVisible = false
            }
            findWebView()?.isVisible = !isLoading
            findProgressBar()?.isVisible = isLoading
        },
            onLoadingError = { isError ->
                findWebView()?.isVisible = !isError
                findErrorLoading()?.isVisible = isError
            })
    }

    private fun startProgressAnimation(context: Context) {
        val rotation = AnimationUtils.loadAnimation(context, R.anim.rotate)
        rotation.fillAfter = true
        findProgressBar()?.startAnimation(rotation)
    }

    private fun initWebView() = with(findWebView()!!) {
        findErrorLoading()?.findViewById<TextView>(R.id.tvErrorMessage)?.text =
            context.getString(R.string.error_message_connection_error)
        findErrorLoading()?.findViewById<TextView>(R.id.tvErrorDescription)?.text =
            context.getString(R.string.error_description)
        findErrorLoading()?.isVisible = false
        settings.javaScriptEnabled = true
        settings.allowContentAccess = true
        settings.domStorageEnabled = true
        setBackgroundColor(Color.WHITE)
        webViewClient = truvWebViewClient
        //https://www.whatismybrowser.com/guides/the-latest-user-agent/chrome
        val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.210 Mobile Safari/537.36"
        settings.userAgentString = USER_AGENT;
        addJavascriptInterface(WebAppInterface(eventListeners), Constants.CITADEL_INTERFACE)
        addJavascriptInterface(MiddleWareInterface {
            val responseDto = MiddleWareResponseDto.parse(JSONObject(it))
            lifecycleScope.launch {
                performRequest(it)
            }

        }, "callbackInterface")

        cookiesUpdateTimer.start()
    }

    private suspend fun performScriptRequest(scriptUrl: String): String? =
        withContext(Dispatchers.Default) {
            val response = HttpRequest(
                method = "GET",
                url = scriptUrl
            ).response()

            Log.d("ExternalWebView", "performRequest: $response")
            response?.body
        }

    private suspend fun applyScript(script: ResponseDto.Payload.Script) =
        withContext(Dispatchers.Default) {
            val scriptText = URL(script.url).readText()
            withContext(Dispatchers.Main) {
                try {
                    evaluateScriptOnWebView(scriptText)
                } catch (e: Exception) {
                    Log.e("ExternalWebView", "Error applying script", e)
                    findWebView()?.isVisible = false
                    findErrorLoading()?.isVisible = true
                }
            }
        }

    private suspend fun performRequest(responseString: String) {
        val response = HttpRequest(
            headers = mapOf(
                "Content-Type" to config?.script?.callbackHeaders?.contentType.orEmpty(),
                "X-Access-Token" to config?.script?.callbackHeaders?.xAccessToken.orEmpty(),
            ),
            body = responseString,
            url = config?.script?.callbackUrl.orEmpty()
        ).response()
        Log.d("ExternalWebView", "performRequest: $response")
    }

    private fun scriptRunner() {

        if (!config?.selector.isNullOrEmpty()) {
            selectorRequest()
        }
        if (!config?.scriptUrl.isNullOrEmpty()) {
            lifecycleScope.launch {
                scriptUrlRequest()
            }
        }
    }

    private fun selectorRequest() {
        val selector = config?.selector?.replace("\"", "'")
        val script = getSelectorScript(selector)
        evaluateScriptOnWebView(
            script
        )
    }

    private fun getSelectorScript(selector: String?) = """
    (function() {
        try {
            const getIsElementDisplayed = (element) => {
                const style = window.getComputedStyle(element);
                return style && style.visibility !== 'hidden' && hasVisibleBoundingBox();
                function hasVisibleBoundingBox() {
                    const rect = element.getBoundingClientRect();
                    return !!(rect.top || rect.bottom || rect.width || rect.height);
                }
            };

            const selector = "${selector}";
            const isXPath = /^\/[\s\S]*\//.test(selector);

            const element = isXPath ? document.evaluate(selector, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue : document.querySelector(selector);

            if (element && getIsElementDisplayed(element)) {
                window.ReactNativeWebView.postMessage(JSON.stringify({ "event": "logged_in" }));
            } else {
                console.log('Element not found or not displayed');
            }
        } catch (error) {
            console.error('Error in WebView script:', error);
        }
    })();
    """

    private suspend fun scriptUrlRequest() {
        performScriptRequest(config?.scriptUrl!!)?.let { script ->
            withContext(Dispatchers.Main) {
                findWebView()?.evaluateJavascript(script, null)
            }
        }
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
                bottomSheetBehavior.maxHeight = windowHeight
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
        startProgressAnimation(contentView.context)
    }

    private val scriptTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {

        override fun onTick(millisUntilFinished: Long) {
            scriptRunner()
        }

        override fun onFinish() {

        }
    }

    private val cookiesUpdateTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {

        override fun onTick(millisUntilFinished: Long) {
            findWebView()?.evaluateJavascript(getSelectorScript()) {
                Log.d("ProviderWebView", "WebView evaluate status: $it")
                if (it == "false") {
                    return@evaluateJavascript
                }
                val seenURLs = truvWebViewClient.getSeenPages()
                Log.d(
                    "ProviderWebView",
                    "Collecting cookies from seen urls: ${seenURLs.joinToString(", ")}"
                )

                val dashboardUrl = findWebView()?.url

                val allCookies = seenURLs.fold(listOf<Cookie>()) { acc, url ->
                    val cookies = CookieManager.getInstance().getCookie(url) ?: return@fold acc
                    val cookieStrings =
                        cookies.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val domain = URL(url).host
                    val topLevelDomain =
                        "." + domain.split(".").dropLastWhile { it.isEmpty() }.takeLast(2)
                            .joinToString(".")

                    val list = cookieStrings
                        .map {
                            it.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        }
                        .filter { it.size == 2 }
                        .map { split ->
                            return@map Cookie(
                                name = split[0].trim(),
                                value = split[1],
                                domain = topLevelDomain,
                                path = "/",
                                secure = false,
                                httpOnly = false,
                            )
                        }

                    return@fold acc.plus(list)
                }

                Log.d("ProviderWebView", "All cookies: $allCookies")

                onCookie(allCookies, dashboardUrl ?: "")
                this.cancel()
            }
        }

        override fun onFinish() {

        }
    }

    fun evaluateScriptOnWebView(script: String) {
        try {
            findWebView()?.evaluateJavascript(script, null)
        } catch (ex: Exception) {
            Log.e("ExternalWebView", "Error applying script", ex)
//            findWebView()?.isVisible = false
//            findErrorLoading()?.isVisible = true
        }

    }

    fun getSelectorScript(): String {
        return "window.document.evaluate(\"${config?.selector}\", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue != null"
    }

    override fun dismiss() {
        scriptTimer.cancel()
        cookiesUpdateTimer.cancel()
        super.dismiss()
    }

    fun findErrorLoading(): View? = findViewById(R.id.errorLoadingLayout)
    fun findErrorRetryButton(): View? = findViewById(R.id.btnTryAgain)
    fun findWebView(): WebView? = findViewById(R.id.webview)
    fun findProgressBar(): View? = findViewById(R.id.progress_bar)
    fun findTitle(): TextView? = findViewById(R.id.title)
    fun findRefresher(): ImageView? = findViewById(R.id.refresher)

    companion object {
        private const val DELAY_MILLIS = 2000L
    }

}