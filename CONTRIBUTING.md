# Contribuer

Les contributions passent par des pull requests sur `main`. Personne ne pousse directement sur `main` : chaque fusion peut publier une version. Seule exception, la PR automatique de `newpipe.yml`, qui ne fusionne qu'après le test réel contre YouTube et toute la vérification.

Avant de coder quelque chose d'important, ouvrez une issue pour en parler : ça évite une PR refusée pour une question de direction. Pour un bug, donnez le modèle du téléphone, la version d'Android et celle de l'app.

1. **Une PR = un changement.** Une fonctionnalité, un bug ou un refactor, pas les trois. Une petite PR se relit en dix minutes ; au-delà de ~400 lignes modifiées, découpez.
2. **Partez d'une branche à jour** de `main`, nommée par le sujet (`fix/sonnerie-secours`, `feat/meteo-vent`).
3. **Le comportement d'abord, en scénario.** Toute règle nouvelle ou corrigée s'écrit en `.feature` français (Soit / Quand / Alors), sans parler du code. Lancez-le et regardez-le échouer avant d'écrire le code. Pas d'étape vide ni « en attente ».
4. **Un bug = un test qui le reproduit.** Expliquez la cause dans la PR, ajoutez le scénario qui échoue pour la bonne raison, puis corrigez la cause, pas le symptôme.
5. **La logique va dans `core`**, testable sans téléphone. `app` ne fait que brancher Android.
6. **Tout est vert en local avant d'ouvrir la PR** :
   ```sh
   ./gradlew detekt :core:test :core:koverVerify :app:lintDebug :app:verifyPaparazziDebug
   ```
   Couverture ≥ 95 % en lignes et en branches, aucun avertissement detekt ou lint, fichiers sous 5 000 tokens (le hook `scripts/hooks` le vérifie).
7. **Un changement visible montre son résultat.** Si l'écran change, enregistrez les captures (`:app:recordPaparazziDebug`), commitez-les et mettez un avant / après (capture ou GIF) dans la PR.
8. **Textes de l'app en français, au vouvoiement**, sans tiret cadratin.
9. **Décrivez la PR** avec le modèle proposé : pourquoi, ce qui change, comment c'est testé, captures. Liez l'issue (`Fixes #12`).
10. **Ne changez pas la version** dans une PR de contribution : le mainteneur s'en charge au moment de publier.
11. **Aucun secret dans le dépôt** : ni keystore, ni mot de passe, ni cookie. La signature se fait uniquement dans `release.yml`, avec les secrets GitHub.

La relecture regarde, dans l'ordre : le contexte, la justesse, la sécurité, les performances, la lisibilité, l'architecture, les tests. Répondez à chaque commentaire (corrigé, ou pourquoi pas) plutôt que de les résoudre en silence. La CI (`check.yml`) relance linters, tests et couverture sur chaque PR. La PR est fusionnée en *squash* quand la CI est verte et qu'une relecture l'a approuvée.

## Architecture

| Module | Rôle |
| --- | --- |
| `core` | Kotlin pur, sans Android : planification, snooze, lien et recherche de playlist, file aléatoire, sécurité de la sonnerie (délai, secours, arrêt), pré-téléchargement, lumière et météo de l'écran de réveil, mises à jour et santé de YouTube. Testé en BDD. |
| `app` | Adaptateurs Android fins : AlarmManager, service de premier plan (Media3/ExoPlayer, `USAGE_ALARM`), NewPipeExtractor, WorkManager, UI Compose. |

L'extraction passe par [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) derrière l'interface `PlaylistSource` : on peut la remplacer par yt-dlp (youtubedl-android) si YouTube casse l'extracteur.

## Tests et qualité

- Scénarios Gherkin en français dans `core/src/test/resources/features`, exécutés par un petit runner maison (`core/src/test/kotlin/.../bdd`). Une étape non définie ou ambiguë fait échouer le scénario.
- Couverture Kover sur `core` : minimum 95 % en lignes et en branches (`./gradlew :core:koverVerify`).
- detekt avec complexité cyclomatique ≤ 6, Android lint avec avertissements bloquants.
- Captures d'écran Paparazzi de chaque écran dans `app/src/test/snapshots` (`:app:verifyPaparazziDebug`).
- Test réel contre YouTube (`YouTubeSmokeTest`, lancé seulement avec `-Psmoke`), joué chaque matin par `newpipe.yml`.
- `check.yml` rejoue detekt, tests, couverture et lint sur chaque PR.
- Hook pré-commit : `git config core.hooksPath scripts/hooks` (refuse les fichiers > 5 000 tokens, lance linters, tests et couverture).

```sh
./gradlew detekt :core:test :core:koverVerify :app:lintDebug :app:verifyPaparazziDebug :app:assembleDebug
```

## Limites connues

- La couche Android n'a que des captures d'écran automatisées (Paparazzi) : le reste se vérifie sur un téléphone (voir la recette ci-dessous).
- YouTube change régulièrement son site. Chaque matin, le workflow [`newpipe.yml`](.github/workflows/newpipe.yml) teste l'app contre le vrai YouTube et passe à la dernière version de NewPipeExtractor dès qu'elle sort : si les tests passent, la version est publiée et les téléphones la proposent ; sinon une issue est ouverte. Dans l'app, un message prévient quand YouTube ne répond plus comme prévu, et la sonnerie de secours garantit que le réveil sonne quand même.
- Les alarmes ne sonnent pas entre un redémarrage et le premier déverrouillage du téléphone.

## Recette sur téléphone

1. Installer l'APK, autoriser les notifications et l'affichage plein écran (bannière en haut de la liste).
2. Créer une alarme avec un lien de playlist, appuyer sur « Tester le réveil » : la musique démarre, l'écran de réveil s'affiche.
3. Appuyer sur « Préparer maintenant », attendre une minute, passer en mode avion, puis « Tester le réveil » : un morceau en cache est joué.
4. Mode avion sans cache (nouvelle playlist) : la sonnerie système retentit.
5. Régler une alarme dans 2 minutes, verrouiller l'écran : l'écran de réveil s'allume.

[← Retour](README.md)
