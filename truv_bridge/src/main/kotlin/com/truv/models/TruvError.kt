package com.truv.models

import org.json.JSONObject

data class TruvError(
    val type: String,
    val code: ErrorCode,
    val message: String
) {

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("type", type)
            put("code", code.error)
            put("message", message)
        }
    }

    enum class ErrorCode(val error: String) {
        // No data
        LINK_ERROR("LINK_ERROR"),
        UNAVAILABLE("UNAVAILABLE"),
        MFA_ERROR("MFA_ERROR"),
        LOGIN_ERROR("LOGIN_ERROR"),
        ERROR("ERROR")
    }

}
