# language: fr
Fonctionnalité: Mise à jour depuis GitHub
  # L'app n'est pas sur le Play Store : elle regarde elle-même la dernière version publiée sur GitHub
  # et propose de l'installer quand elle est plus récente que celle du téléphone.

  Scénario: L'app demande la dernière version publiée du dépôt
    Alors l'adresse de vérification est "https://api.github.com/repos/Clrk/reveil-playlist/releases/latest"

  Scénario: Une version plus récente est proposée avec son APK
    Soit la version installée "0.9.8"
    Et GitHub publie "v0.9.9" avec l'APK "reveil-playlist-0.9.9.apk"
    Alors la mise à jour proposée est "0.9.9"
    Et elle se télécharge depuis "https://example.org/reveil-playlist-0.9.9.apk"

  Scénario: Les numéros se comparent un par un, 0.10 vient après 0.9
    Soit la version installée "0.9.12"
    Et GitHub publie "v0.10.0" avec l'APK "reveil-playlist-0.10.0.apk"
    Alors la mise à jour proposée est "0.10.0"

  Scénario: Un correctif vient après la version qu'il corrige, 0.9.1 après 0.9
    Soit la version installée "0.9"
    Et GitHub publie "v0.9.1" avec l'APK "reveil-playlist-0.9.1.apk"
    Alors la mise à jour proposée est "0.9.1"

  Scénario: La même version n'est pas proposée
    Soit la version installée "0.9.9"
    Et GitHub publie "v0.9.9" avec l'APK "reveil-playlist-0.9.9.apk"
    Alors aucune mise à jour n'est proposée

  Scénario: Une version plus ancienne n'est pas proposée
    Soit la version installée "1.0"
    Et GitHub publie "v0.9.9" avec l'APK "reveil-playlist-0.9.9.apk"
    Alors aucune mise à jour n'est proposée

  Scénario: Une version publiée sans APK n'est pas proposée
    Soit la version installée "0.9.8"
    Et GitHub publie "v0.9.9" sans APK
    Alors aucune mise à jour n'est proposée

  Scénario: Une réponse illisible ne propose rien
    Soit la version installée "0.9.8"
    Et GitHub répond "{pas du json"
    Alors aucune mise à jour n'est proposée

  # Sans permission d'installer (Play Protect bloque les apps qui l'ont), l'app télécharge elle-même
  # le fichier, puis Android l'installe quand on l'ouvre depuis les téléchargements.

  Scénario: Une nouvelle version se propose avant tout téléchargement
    Soit la nouvelle version "0.9.12"
    Alors le bandeau dit "Nouvelle version 0.9.12 disponible" avec le bouton "Installer"

  Scénario: Le téléchargement montre où il en est
    Soit la nouvelle version "0.9.12"
    Quand 3 Mo sur 6 Mo sont téléchargés
    Alors le bandeau dit "Téléchargement de la version 0.9.12 : 50 %" sans bouton

  Scénario: Un téléchargement de taille inconnue montre seulement qu'il avance
    Soit la nouvelle version "0.9.12"
    Quand 3 Mo d'une taille inconnue sont téléchargés
    Alors le bandeau dit "Téléchargement de la version 0.9.12…" sans bouton

  Scénario: Le fichier téléchargé s'ouvre pour l'installer
    Soit la nouvelle version "0.9.12"
    Quand le téléchargement est terminé
    Alors le bandeau dit "Version 0.9.12 prête à installer" avec le bouton "Ouvrir"

  Scénario: Un téléchargement raté se relance
    Soit la nouvelle version "0.9.12"
    Quand le téléchargement échoue
    Alors le bandeau dit "Le téléchargement de la version 0.9.12 a échoué" avec le bouton "Réessayer"

  Scénario: Le fichier porte le numéro de la version
    Alors le fichier de la version "0.9.12" s'appelle "reveil-playlist-0.9.12.apk"
