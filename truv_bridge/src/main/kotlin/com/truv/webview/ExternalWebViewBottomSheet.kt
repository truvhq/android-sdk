package com.truv.webview

import android.content.Context
import android.graphics.Color
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
                findTitle()?.text = getDomainFromUrl(value.url)
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

    var scriptFromUrl: String? = null

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
            try {
                val scriptText = URL(script.url).readText()
                delay(2000)
                withContext(Dispatchers.Main) {
                    evaluateScriptOnWebView(scriptText)
                }
            } catch (e: Exception) {
                Log.e("ExternalWebView", "Error applying script", e)
                withContext(Dispatchers.Main) {
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

    private suspend fun loadScriptUrlRequest(): String? {
        if (config?.scriptUrl == null) return null
        return scriptFromUrl ?: run { config?.scriptUrl?.let { performScriptRequest(it) } }
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

    var job: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val cookiesUpdateTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {

        override fun onTick(millisUntilFinished: Long) {
            job?.cancel()
            job = lifecycleScope.launch(Dispatchers.Default.limitedParallelism(1)) {
                val selectorScript = getSelectorScript() ?: return@launch
                withContext(Dispatchers.Main) {
                    findWebView()?.evaluateJavascript(selectorScript) { result ->
                        Log.d("ProviderWebView", "WebView evaluate status: $result")
                        if (result == "false") {
                            return@evaluateJavascript
                        }
                        val seenURLs = truvWebViewClient.getSeenPages().toList()
                        Log.d(
                            "ProviderWebView",
                            "Collecting cookies from seen urls: ${seenURLs.joinToString(", ")}"
                        )

                        val dashboardUrl = findWebView()?.url

                        val allCookies = seenURLs.fold(listOf<Cookie>()) { acc, url ->
                            val cookies =
                                kotlin.runCatching { CookieManager.getInstance().getCookie(url) }
                                    .getOrNull() ?: return@fold acc
                            Log.d("ProviderWebView", "cookies for $url: $cookies")
                            val cookieStrings =
                                cookies.split(";".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                            val domain = URL(url).host
                            val topLevelDomain = ".$domain"

                            val list = cookieStrings.mapNotNull { cookieString ->
                                val equalIndex = cookieString.indexOf('=')
                                if (equalIndex == -1) {
                                    return@mapNotNull null
                                }
                                val name = cookieString.substring(0, equalIndex).trim()
                                val value = cookieString.substring(equalIndex + 1).trim()

                                return@mapNotNull Cookie(
                                    name = name,
                                    value = value,
                                    domain = topLevelDomain,
                                    path = "/",
                                    secure = false,
                                    httpOnly = false,
                                )
                            }

                            return@fold acc.plus(list)
                        }
                            .distinctBy { Pair(it.domain, it.name) }

                        Log.d("ProviderWebView", "All cookies: $allCookies")

                        onCookie(allCookies, dashboardUrl ?: "")
                        dismiss()
                    }
                }
            }
        }

        override fun onFinish() {

        }
    }

    private suspend fun evaluateScriptOnWebView(script: String) = withContext(Dispatchers.Main) {
        try {
            if (findWebView()?.isVisible == true) {
                findWebView()?.evaluateJavascript(script, null)
            } else {
                Log.d("ExternalWebView", "WebView is not visible")
            }
        } catch (ex: Exception) {
            Log.e("ExternalWebView", "Error applying script", ex)
        }

    }

    suspend fun getSelectorScript(): String? {
        scriptFromUrl = loadScriptUrlRequest()
        return when {
            scriptFromUrl != null -> return scriptFromUrl
            config?.selector != null -> "window.document.evaluate(\"${config?.selector}\", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue != null"
            else -> {
                Log.e("ExternalWebView", "No selector or scriptUrl provided")
                null
            }
        }
    }

    override fun dismiss() {
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