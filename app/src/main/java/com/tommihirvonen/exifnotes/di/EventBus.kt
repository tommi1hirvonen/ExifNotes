package com.tommihirvonen.exifnotes.di

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@ActivityRetainedScoped
class EventBus @Inject constructor() {

    private val _frameCountRefreshEvents = MutableSharedFlow<Long>(extraBufferCapacity = 1)

    val frameCountRefreshEvents = _frameCountRefreshEvents.asSharedFlow()

    suspend fun sendFrameCountRefreshEvent(rollId: Long) {
        _frameCountRefreshEvents.emit(rollId)
    }
}