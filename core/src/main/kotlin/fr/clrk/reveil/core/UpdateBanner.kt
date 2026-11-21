package fr.clrk.reveil.core

/** Where the app's own download of a new version stands. */
sealed interface Download {
    /** [total] is negative when the server did not say the size. */
    data class Running(val done: Long, val total: Long) : Download
    data object Done : Download
    data object Failed : Download
}

/** The line at the top of the list about a new version, and its button, if any. */
data class UpdateBanner(val text: String, val button: String?) {
    companion object {
        fun of(version: String, download: Download?) = when (download) {
            null -> UpdateBanner("Nouvelle version $version disponible", "Installer")
            is Download.Running -> UpdateBanner("Téléchargement de la version $version${progress(download)}", null)
            Download.Done -> UpdateBanner("Version $version prête à installer", "Ouvrir")
            Download.Failed -> UpdateBanner("Le téléchargement de la version $version a échoué", "Réessayer")
        }

        /** The APK's name in the phone's Downloads folder. */
        fun fileName(version: String) = "reveil-playlist-$version.apk"

        private fun progress(download: Download.Running) =
            if (download.total > 0) " : ${download.done * PERCENT / download.total} %" else "…"

        private const val PERCENT = 100
    }
}
