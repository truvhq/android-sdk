package com.truv.webview

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient


class TruvWebViewClient(
    private val context: Context,
    private val eventListeners: Set<TruvEventsListener>,
    private val onLoading: (Boolean) -> Unit = {},
    private val onLoaded: () -> Unit = {},
) : WebViewClient() {
    private val seenURLs = mutableSetOf<String>()
    private var loadingFinished = true
    private var redirect = false

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest
    ): WebResourceResponse? {
        if (request.requestHeaders.containsKey("X-Requested-With")) {
            request.requestHeaders.remove("X-Requested-With")
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        if (!loadingFinished) {
            redirect = true;
        }

        loadingFinished = false

        onLoading(true)
        request?.let {
            view?.loadUrl(it.url.toString())
        }

        seenURLs.add(request?.url.toString())
        return true
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        eventListeners.forEach { it.onClose() }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        loadingFinished = false;
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        super.onPageCommitVisible(view, url)
        onLoading(false)
        onLoaded()
    }

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        Log.d("TruvWebViewClient", "doUpdateVisitedHistory: $url")
        if (url != null) {
            seenURLs.add(url)
        }
        super.doUpdateVisitedHistory(view, url, isReload)
    }

    fun getSeenPages(): Set<String> {
        return seenURLs
    }

}