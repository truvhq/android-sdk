package com.truv

import com.truv.models.TruvEventPayload
import com.truv.models.TruvSuccessPayload

interface TruvEventsListener {

    fun onSuccess(payload: TruvSuccessPayload)

    fun onEvent(event: TruvEventPayload.EventType)

    fun onClose()

    fun onLoad()

    fun onError()

}