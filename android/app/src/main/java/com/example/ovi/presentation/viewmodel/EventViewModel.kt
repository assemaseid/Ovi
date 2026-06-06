package com.example.ovi.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ovi.data.api.LockService
import com.example.ovi.data.dto.event.EnrichedEventDto
import com.example.ovi.domain.model.EventType
import com.example.ovi.domain.model.LockEvent
import com.example.ovi.domain.model.UnlockMethod
import com.example.ovi.domain.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class EventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val lockService: LockService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val lockId: String = checkNotNull(savedStateHandle["lockId"])

    private val _serverEvents = MutableStateFlow<List<LockEvent>>(emptyList())

    val events: StateFlow<List<LockEvent>> = combine(
        eventRepository.getEventsForLock(lockId),
        _serverEvents
    ) { localEvents, serverEvents ->
        if (serverEvents.isNotEmpty()) serverEvents else localEvents
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    init {
        fetchServerEvents()
    }

    private fun fetchServerEvents() {
        viewModelScope.launch {
            try {
                val response = lockService.getEnrichedEvents(lockId)
                if (response.isSuccessful) {
                    _serverEvents.value = response.body()
                        ?.mapNotNull { it.toLockEvent() }
                        ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }
}

private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

private fun parseIso(iso: String): Long = try {
    isoFormat.parse(iso.substringBefore('.').trimEnd('Z'))?.time ?: System.currentTimeMillis()
} catch (_: Exception) { System.currentTimeMillis() }

private fun EnrichedEventDto.toLockEvent(): LockEvent? {
    val type = when (eventType) {
        "unlock_success" -> EventType.UNLOCK
        "lock_success"   -> EventType.LOCK
        "pin_rotation"   -> EventType.PIN_ROTATION
        "battery_low"    -> EventType.LOW_BATTERY
        "tamper_detected" -> EventType.TAMPER_DETECTED
        else -> return null
    }
    val method = when ((eventData["method"] as? String)?.uppercase()) {
        "BLE", "BLUETOOTH" -> UnlockMethod.BLUETOOTH
        "PIN"              -> UnlockMethod.PIN
        "FINGERPRINT"      -> UnlockMethod.FINGERPRINT
        "REMOTE", "MQTT"   -> UnlockMethod.REMOTE
        else               -> UnlockMethod.MANUAL
    }
    return LockEvent(
        id = eventUuid,
        lockId = deviceUuid,
        timestamp = parseIso(createdAt),
        type = type,
        success = true,
        method = method,
        userUuid = userUuid,
        userName = userName,
        fingerName = fingerName,
        msgId = msgId
    )
}
