package com.truv

import com.truv.models.TruvEventPayload

interface TruvEventsListener {

    fun onSuccess()

    fun onEvent(event: TruvEventPayload.EventType)

    fun onClose()

    fun onLoad()

    fun onError()

}