package de.jscoding.bacheckin.core.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class InactivityDetector @Inject constructor() {
    companion object {
        private const val INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000L
        private const val TICK_MS = 1000L
    }

    private val _lastInteraction = MutableStateFlow(System.currentTimeMillis())
    private val _remaining = MutableStateFlow(INACTIVITY_TIMEOUT_MS)
    val remainingFlow: StateFlow<Long> = _remaining.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            while (true) {
                val last = _lastInteraction.value
                val elapsed = System.currentTimeMillis() - last
                val rem = (INACTIVITY_TIMEOUT_MS - elapsed).coerceAtLeast(0L)
                _remaining.value = rem
                delay(TICK_MS.milliseconds)
            }
        }
    }

    fun markInteraction() {
        val now = System.currentTimeMillis()
        _lastInteraction.value = now
        _remaining.value = INACTIVITY_TIMEOUT_MS
    }
}
