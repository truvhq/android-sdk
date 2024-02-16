package com.truv.models

import org.json.JSONException
import org.json.JSONObject

data class TruvEventPayload(
    val payload: Payload?,
    val eventType: EventType
) {

    data class Payload(
        val bridgeToken: String?,
        val productType: String?,
        val viewName: String?,
        val employer: TruvEmployer?,
        val publicToken: String?,
        val taskId: String?,
        val providerId: String?,
        val error: TruvError?,
        val externalLoginConfig: ExternalLoginConfig?
    )

    enum class EventType(event: String) {
        LOAD("LOAD"),
        OPEN("OPEN"),
        SCREEN_VIEW("SCREEN_VIEW"),
        EMPLOYER_SELECTED("EMPLOYER_SELECTED"),
        LINK_CREATED("LINK_CREATED"),
        LOGIN_COMPLETE("LOGIN_COMPLETE"),
        SUCCESS("SUCCESS"),
        ERROR("ERROR"),
        UNSUPPORTED_BROWSER("UNSUPPORTED_BROWSER"),
        START_EXTERNAL_LOGIN("START_EXTERNAL_LOGIN"),
        CLOSE("CLOSE")
    }

    companion object {

        @Throws(JSONException::class)
        fun fromJson(event: String): TruvEventPayload {
            val json = JSONObject(event)
            val responseDto = ResponseDto.parse(json)
            val payloadOut = with(responseDto) {
                val externalLoginConfig =
                    if (!payload?.isLoggedIn?.selector.isNullOrEmpty() && !payload?.url.isNullOrEmpty()) {
                        ExternalLoginConfig(
                            url = payload?.url!!,
                            selector = payload.isLoggedIn?.selector!!,
                            script = payload.script
                        )
                    } else null

                val error = payload?.error?.let {
                    TruvError(
                        type = it.type!!,
                        code = TruvError.ErrorCode.valueOf(it.code!!),
                        message = it.message!!
                    )
                }
                val truvEmployer = if (payload?.employer?.name != null) {
                    TruvEmployer(payload.employer.name)
                } else null

                Payload(
                    bridgeToken = payload?.bridgeToken,
                    productType = payload?.productType,
                    viewName = payload?.viewName,
                    employer = truvEmployer,
                    publicToken = payload?.publicToken,
                    taskId = payload?.taskId,
                    providerId = payload?.providerId,
                    error = error,
                    externalLoginConfig = externalLoginConfig
                )
            }

            return TruvEventPayload(
                eventType = EventType.valueOf(responseDto.eventType!!),
                payload = payloadOut
            )
        }

    }

}