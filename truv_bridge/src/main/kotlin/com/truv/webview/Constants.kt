package com.truv.webview

object Constants {
    const val BASE_URL = "https://cdn.truv.com/mobile.html"
    const val BRIDGE_TOKEN = "bridge_token"
    const val EXTERNAL_APP_LOGIN = "__TruvExternalFeatures=external_app_login"
    const val BRIDGE_URL = "$BASE_URL?$BRIDGE_TOKEN=%s&$EXTERNAL_APP_LOGIN"
    const val CITADEL_INTERFACE = "citadelInterface"
    // Scripts
    const val SCRIPT_BACK_PRESS = "window.bridge?.back();"
    const val SCRIPT_EXTERNAL_LOGIN_CANCEL = "window.bridge?.onExternalLoginCancel();"
    const val SCRIPT_EXTERNAL_LOGIN_SUCCESS = "window.bridge?.onExternalLoginSuccess(%s);"
}