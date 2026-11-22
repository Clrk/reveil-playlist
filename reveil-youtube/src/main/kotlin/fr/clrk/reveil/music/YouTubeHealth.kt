package fr.clrk.reveil.music

import fr.clrk.reveil.core.SourceEvent
import fr.clrk.reveil.core.SourceHealth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException

/** Where the host keeps the count of unreadable answers in a row (SharedPreferences on Android). */
interface HealthStore {
    var unreadable: Int
}

/** Remembers whether YouTube still answers in a way NewPipeExtractor can read; the host warns when not. */
object YouTubeHealth {
    private var store: HealthStore? = null
    private val state = MutableStateFlow(false)
    val broken: StateFlow<Boolean> = state

    fun init(store: HealthStore) {
        this.store = store
        state.value = health().broken
    }

    /** Runs a call to YouTube and records what its outcome says about YouTube. */
    fun <T> watch(call: () -> T): T = runCatching(call).also { record(it.exceptionOrNull()) }.getOrThrow()

    private fun record(error: Throwable?) {
        val store = store ?: return
        val health = health().after(event(error))
        store.unreadable = health.unreadable
        state.value = health.broken
    }

    private fun health() = SourceHealth(store?.unreadable ?: 0)

    /** A removed video or a captcha says nothing about the extractor; a page it cannot parse does. */
    private fun event(error: Throwable?) = when (error) {
        null -> SourceEvent.WORKED
        is ContentNotAvailableException, is ReCaptchaException -> SourceEvent.OFFLINE
        is ExtractionException -> SourceEvent.UNREADABLE
        else -> SourceEvent.OFFLINE
    }
}
