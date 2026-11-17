package fr.clrk.reveil.core

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Texts that tell the user when an alarm will ring and whether its music is ready offline. */
object WakeInfo {
    data class NextCard(val title: String, val detail: String)

    /** A download the user started by hand. */
    sealed interface Download {
        data class Running(val done: Int, val total: Int) : Download
        data object Waiting : Download
        data object Failed : Download
    }

    private val day = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.FRENCH)

    /** What to say right after saving or switching on an alarm; nothing when it will not ring. */
    fun announce(alarm: Alarm, now: ZonedDateTime): String? =
        NextTrigger.after(alarm, now)?.let { "Réveil dans ${Countdown.between(now, it)}" }

    fun next(next: Pair<Alarm, ZonedDateTime>?, now: ZonedDateTime): NextCard {
        val (alarm, at) = next ?: return NextCard("Aucun réveil prévu", "Appuie sur + pour en créer un")
        val detail = listOf("dans ${Countdown.between(now, at)}", alarm.label).filter { it.isNotBlank() }.joinToString(" · ")
        return NextCard(at.format(day), detail)
    }

    fun skipped(alarm: Alarm, zone: ZoneId): String? = alarm.skipped?.let { "Réveil de ${it.atZone(zone).format(day)} sauté" }

    fun download(state: Download): String = when (state) {
        is Download.Running -> running(state)
        Download.Waiting -> "En attente d'une connexion pour télécharger"
        Download.Failed -> "Téléchargement impossible pour l'instant, nouvel essai la veille"
    }

    private fun running(state: Download.Running) =
        if (state.total == 0) "Recherche des titres de la playlist…" else "Téléchargement en cours… ${state.done} / ${state.total}"

    fun offline(alarm: Alarm, cachedTracks: Int): String? = alarm.playlistId?.let { offline(cachedTracks) }

    fun offline(cachedTracks: Int): String = when {
        cachedTracks == 0 -> "Pas encore téléchargée, ce sera fait la veille"
        cachedTracks == 1 -> "1 titre prêt hors ligne"
        else -> "$cachedTracks titres prêts hors ligne"
    }
}
