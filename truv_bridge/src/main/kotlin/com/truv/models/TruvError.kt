package com.truv.models

data class TruvError(
    val type: String,
    val code: ErrorCode,
    val message: String
) {

    enum class ErrorCode(error: String) {
        // No data
        LINK_ERROR("LINK_ERROR"),
        UNAVAILABLE("UNAVAILABLE"),
        MFA_ERROR("MFA_ERROR"),
        LOGIN_ERROR("LOGIN_ERROR"),
        ERROR("ERROR")
    }

}
