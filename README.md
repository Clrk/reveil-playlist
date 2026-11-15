# Réveil Playlist

Application Android de réveil qui joue une playlist YouTube Music en aléatoire, sans abonnement.

## Ce que fait l'app

- Alarmes multiples : heure, jours de répétition, nom, durée de snooze, activation par interrupteur.
- Déclenchement exact même en veille profonde (`AlarmManager.setAlarmClock`), reprogrammation après redémarrage ou changement d'heure.
- Écran plein écran au-dessus de l'écran verrouillé, avec Snooze et Arrêter (aussi dans la notification).
- Playlist : coller un lien YouTube Music / YouTube, ou utiliser « Partager » depuis l'app YouTube Music vers Réveil Playlist.
- Lecture aléatoire, sans recommencer deux réveils de suite par le même morceau.
- Fiabilité :
  - la veille au soir (9 h avant le réveil), 15 morceaux sont pré-téléchargés et renouvelés au fil des réveils ;
  - au réveil : morceaux en cache d'abord, puis streaming ;
  - un morceau qui ne démarre pas en 10 s est sauté ; après 3 échecs de suite, sonnerie d'alarme du système ;
  - arrêt automatique au bout de 30 minutes.

## Architecture

| Module | Rôle |
| --- | --- |
| `core` | Kotlin pur, sans Android : planification, gestion, snooze, lien de playlist, file aléatoire, supervision de lecture, politique de pré-téléchargement. Testé en BDD. |
| `app` | Adaptateurs Android fins : AlarmManager, service de premier plan (Media3/ExoPlayer, `USAGE_ALARM`), NewPipeExtractor, WorkManager, UI Compose. |

L'extraction passe par [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) derrière l'interface `PlaylistSource` : on peut la remplacer par yt-dlp (youtubedl-android) si YouTube casse l'extracteur.

## Tests et qualité

- Scénarios Gherkin en français dans `core/src/test/resources/features`, exécutés par un petit runner maison (`core/src/test/kotlin/.../bdd`). Une étape non définie ou ambiguë fait échouer le scénario.
- Couverture Kover sur `core` : minimum 95 % en lignes et en branches (`./gradlew :core:koverVerify`).
- detekt avec complexité cyclomatique ≤ 5, Android lint avec avertissements bloquants.
- Hook pré-commit : `git config core.hooksPath scripts/hooks` (refuse les fichiers > 5 000 tokens, lance linters, tests et couverture).

```sh
./gradlew detekt :core:test :core:koverVerify :app:lintDebug :app:assembleDebug
```

## Limites connues

- La couche Android n'a pas de tests automatisés : elle se vérifie sur un téléphone (voir la checklist de recette ci-dessous).
- YouTube change régulièrement son site ; si l'extraction casse, mettre à jour NewPipeExtractor. La sonnerie de secours garantit que le réveil sonne quand même.
- Les alarmes ne sonnent pas entre un redémarrage et le premier déverrouillage du téléphone.

## Recette sur téléphone

1. Installer l'APK, autoriser les notifications et l'affichage plein écran (bannière en haut de la liste).
2. Créer une alarme avec un lien de playlist, appuyer sur « Tester » : la musique démarre, l'écran de réveil s'affiche.
3. Appuyer sur « Télécharger maintenant », attendre une minute, passer en mode avion, puis « Tester » : un morceau en cache est joué.
4. Mode avion sans cache (nouvelle playlist) : la sonnerie système retentit.
5. Régler une alarme dans 2 minutes, verrouiller l'écran : l'écran de réveil s'allume.
