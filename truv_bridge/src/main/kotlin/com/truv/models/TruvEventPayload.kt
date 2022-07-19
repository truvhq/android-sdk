package com.truv.models

data class TruvEventPayload(
    val payload: Payload?,
    val eventType: EventType
) {

    inner class Payload(
        val bridgeToken: String?,
        val productType: String?,
        val viewName: String?,
        val employer: TruvEmployer?,
        val publicToken: String?,
        val taskId: String?,
        val providerId: String?,
        val error: TruvError?
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
        CLOSE("CLOSE")
    }

}