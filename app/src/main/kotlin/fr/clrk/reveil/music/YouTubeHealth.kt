package fr.clrk.reveil.music

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import fr.clrk.reveil.core.SourceEvent
import fr.clrk.reveil.core.SourceHealth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException

/** Remembers whether YouTube still answers in a way NewPipeExtractor can read; the list warns when not. */
object YouTubeHealth {
    private var prefs: SharedPreferences? = null
    private val state = MutableStateFlow(false)
    val broken: StateFlow<Boolean> = state

    fun init(context: Context) {
        prefs = context.getSharedPreferences("youtube", Context.MODE_PRIVATE)
        state.value = health().broken
    }

    /** Runs a call to YouTube and records what its outcome says about YouTube. */
    fun <T> watch(call: () -> T): T = runCatching(call).also { record(it.exceptionOrNull()) }.getOrThrow()

    private fun record(error: Throwable?) {
        val prefs = prefs ?: return
        val health = health().after(event(error))
        prefs.edit { putInt(UNREADABLE, health.unreadable) }
        state.value = health.broken
    }

    private fun health() = SourceHealth(prefs?.getInt(UNREADABLE, 0) ?: 0)

    /** A removed video or a captcha says nothing about the extractor; a page it cannot parse does. */
    private fun event(error: Throwable?) = when (error) {
        null -> SourceEvent.WORKED
        is ContentNotAvailableException, is ReCaptchaException -> SourceEvent.OFFLINE
        is ExtractionException -> SourceEvent.UNREADABLE
        else -> SourceEvent.OFFLINE
    }

    private const val UNREADABLE = "unreadable"
}
