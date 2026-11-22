# Réutiliser les modules de reveil-playlist

reveil-playlist reste une app de réveil pour téléphone. Ses modules peuvent servir à une autre app, comme le Bonjour (réveil-enceinte sous Android 7). Rien n'est publié : on les prend par submodule git et `includeBuild`.

## Les modules

| Module | Type | Ce qu'il contient |
| --- | --- | --- |
| `reveil-core` | Kotlin pur | Alarmes, récurrence, planification, snooze, playlist, file de lecture, règles de préchargement, santé de la source. Ni Android, ni NewPipe. |
| `reveil-youtube` | Kotlin pur | NewPipeExtractor derrière les ports de `reveil-core`, `YouTubeHealth`. |
| `reveil-ring` | Bibliothèque Android | `RingService`, `RingMusic`, receivers, `VolumeFader`, préchargement du soir, cache des morceaux. |
| `app-logic` | Kotlin pur | Logique d'affichage propre au téléphone (platine, lumière, pluie, météo, mises à jour). Pas fait pour être réutilisé. |
| `app` | Application | L'app reveil-playlist. |

Tous les modules ont le groupe `fr.clrk.reveil`.

## Brancher les modules dans une autre app

```sh
git submodule add https://github.com/Clrk/reveil-playlist reveil
git -C reveil checkout <commit épinglé>
```

```kotlin
// settings.gradle.kts de l'app hôte
includeBuild("reveil")

// build.gradle.kts du module de l'app hôte
dependencies {
    implementation("fr.clrk.reveil:reveil-core")
    implementation("fr.clrk.reveil:reveil-youtube")
    implementation("fr.clrk.reveil:reveil-ring")
}
```

Gradle remplace ces coordonnées par les modules du submodule. L'app hôte ajoute aussi `maven("https://jitpack.io")` à ses dépôts (NewPipeExtractor vient de JitPack).

### YouTube : donner son propre client HTTP

`reveil-youtube` ne crée jamais de client HTTP. L'hôte lui donne le sien, avec ses certificats racines et ses délais :

```kotlin
YouTube.init(monClientOkHttp)
YouTubeHealth.init(monStockage) // HealthStore : garde le nombre de pages illisibles de suite
```

### « Mets tel morceau » : recherche et résolution à la demande

`reveil-core` définit le port `MusicSource` : la lecture d'une playlist (`PlaylistSource`), la recherche d'un titre en texte libre (`TrackSearch`) et la résolution d'un morceau en flux audio (`StreamResolver`). `OnDemand(source).play("la mer trenet")` cherche le titre, saute un morceau illisible (3 essais au plus) et rend `Playable(track, streamUrl)`, `NotFound` ou `Unavailable`.

`reveil-youtube` fournit `YouTubeSource()`. Une autre source (yt-dlp côté serveur) implémente simplement `MusicSource`.

### Sonnerie : implémenter `RingHost`

L'`Application` de l'hôte implémente `fr.clrk.reveil.alarm.RingHost` : les alarmes (`AlarmService`), la source des playlists, la résolution des morceaux en flux, le client qui télécharge les morceaux, l'écran de sonnerie, l'écran d'accueil, l'icône des notifications, et un `RingDisplay` qui suit la sonnerie pour l'écran.

L'hôte crée les canaux de notification `RingChannels.RING` et `RingChannels.REMINDER`. Le service, les receivers et leurs permissions arrivent par le manifeste de `reveil-ring`.

## Réponses pour le projet Bonjour V2

### Pourquoi minSdk 28 ?

Aucune raison n'est écrite : `minSdk = 28` est là depuis le premier commit (15 novembre 2026), sans explication. Un essai avec `minSdk = 24` (lint, 22 novembre 2026) montre ce qui casse en dessous :

| API | Ce qui casse | Où |
| --- | --- | --- |
| 28 | `Ringtone.setLooping` : la toute dernière sonnerie de secours ne boucle plus | `reveil-ring`, `RingService` |
| 27 | `showWhenLocked` et `turnScreenOn` du manifeste sont ignorés : l'écran de sonnerie ne s'allume pas sur l'écran verrouillé | `app`, `RingActivity` |
| 26 | `NotificationChannel` : les canaux de sonnerie et de rappel n'existent pas | `app`, `ReveilApp` |
| 26 | Icône adaptative (`<adaptive-icon>`) : la compilation échoue | `app`, `mipmap-anydpi` |

Le reste passe déjà sous l'API 24 : `ContextCompat.startForegroundService` et `ServiceCompat.startForeground` choisissent la bonne méthode, `java.time` est fourni par `coreLibraryDesugaring` (déjà activé), `setAlarmClock` existe depuis l'API 21 et `setExactAndAllowWhileIdle` depuis l'API 23. Le seul blocage dans `reveil-ring` est donc `Ringtone.setLooping`. Le passage de `reveil-ring` en minSdk 24 est le point 5 de la demande ; l'app reveil-playlist reste en minSdk 28.

### Quels clients YouTube NewPipeExtractor utilise-t-il, et faut-il un PO token ?

Lu dans NewPipeExtractor v0.27.1, la version actuelle du dépôt :

- **Les flux audio** viennent du client InnerTube **visionOS** (`fetchVisionOsClient`, `getVisionOsPlayerResponse`).
- **Les métadonnées** de la vidéo viennent du client **WEB** (`getWebMetadataPlayerResponse`).
- **La recherche et les playlists** passent par les pages WEB de YouTube et YouTube Music (`WEB`, `WEB_REMIX`).
- **PO token** : NewPipeExtractor propose une interface `PoTokenProvider` (`getWebClientPoToken`), facultative. reveil-playlist n'en fournit pas, et les téléphones lisent pourtant les flux : avec cette version, le client visionOS se passe de PO token. Si YouTube l'exige un jour, le test quotidien `newpipe.yml` échouera et l'hôte devra fournir un `PoTokenProvider` (par exemple via une WebView BotGuard), ou passer par yt-dlp côté serveur.

Compte YouTube Music (facultatif, dans `app`) : requêtes `browse` du client `WEB_REMIX`, signées avec les cookies de l'utilisateur (SAPISIDHASH).

Ces clients changent à chaque version de NewPipeExtractor : relire cette section à chaque montée de version.

### Les écrans d'édition d'alarme s'extraient-ils en module d'interface ?

Mieux vaut les réécrire dans le compagnon, en réutilisant `reveil-core`. Les écrans d'édition (`AlarmEditor`, `NewAlarmFlow`, `PlaylistPicker`) sont liés à l'identité du téléphone : palette de nuit, pochettes qui volent entre les écrans (`Flight`), apparitions en cascade (`Reveal`), métaphore du vinyle. Ils lisent aussi des données propres au téléphone : le cache des morceaux, le compte YouTube Music par WebView, la ville de la météo.

Ce qui se réutilise tel quel vient de `reveil-core` : le modèle `Alarm`, `Recurrence` et ses raccourcis, `AlarmService` (créer, modifier, sauter, snooze), `NextTrigger`, les textes de `WakeInfo` (« Réveil dans 8 h 12 min »), `PlaylistLink` et `PlaylistFinder` pour la recherche. Le sélecteur d'heure `TimeWheel` (65 lignes de Compose) peut être copié s'il plaît.
