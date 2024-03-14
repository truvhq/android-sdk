package com.truv.webview

import com.truv.BuildConfig

object Constants {
    const val CDN_URL = "https://cdn.truv.com"
    const val BASE_URL = "$CDN_URL/mobile.html"
    const val BRIDGE_TOKEN = "bridge_token"
    const val EXTERNAL_APP_LOGIN = "__TruvExternalFeatures=external_app_login"
    const val BRIDGE_URL = "$BASE_URL?$BRIDGE_TOKEN=%s&$EXTERNAL_APP_LOGIN&sdk=android:${BuildConfig.VERSION_NAME}"
    const val CITADEL_INTERFACE = "citadelInterface"
    // Scripts
    const val SCRIPT_BACK_PRESS = "window.bridge?.back();"
    const val SCRIPT_EXTERNAL_LOGIN_CANCEL = "window.bridge?.onExternalLoginCancel();"
    const val SCRIPT_EXTERNAL_LOGIN_SUCCESS = "window.bridge?.onExternalLoginSuccess(%s);"
}