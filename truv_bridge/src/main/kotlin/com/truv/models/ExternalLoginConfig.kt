package com.truv.models

data class ExternalLoginConfig(
    val url: String = "",
    val selector: String = "",
    val script: ResponseDto.Payload.Script? = null,
)