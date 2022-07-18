package com.truv.models

sealed class TruvEvent {
    object Close : TruvEvent()
    object Error : TruvEvent()
    class Event(val payload: TruvEventPayload?) : TruvEvent()
    object Load : TruvEvent()
    class Success(val payload: TruvSuccessPayload?) : TruvEvent()
}