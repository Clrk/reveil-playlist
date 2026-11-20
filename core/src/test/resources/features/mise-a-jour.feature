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
