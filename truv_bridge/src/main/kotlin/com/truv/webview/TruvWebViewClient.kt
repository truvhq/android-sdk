package com.truv.webview

import android.content.Context
import android.graphics.Bitmap
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

    private var loadingFinished = true
    private var redirect = false

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest
    ): WebResourceResponse? {
        if (request.requestHeaders.containsKey("X-Requested-With")) {
            request.requestHeaders.remove("X-Requested-With")
        }
        return super.shouldInterceptRequest(view,request)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        if (!loadingFinished) {
            redirect = true;
        }

        loadingFinished = false;
        onLoading(true)
        request?.let {
            view?.loadUrl(it.url.toString())
        }
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

//    override fun onPageFinished(view: WebView?, url: String?) {
//        super.onPageFinished(view, url)
//        if (!redirect) {
//            loadingFinished = true;
//        }
//        if (loadingFinished && !redirect) {
//            onLoaded()
//        } else {
//            redirect = false;
//        }
//
//    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        super.onPageCommitVisible(view, url)
        onLoading(false)
        onLoaded()
    }


}