# language: fr
Fonctionnalité: Trouver une playlist YouTube Music sans coller de lien

  Scénario: Chercher par nom propose les playlists YouTube Music trouvées
    Soit YouTube Music trouve "Chill matin, Réveil en douceur, Morning Jazz" pour "matin"
    Quand je cherche "matin"
    Alors on me propose "Chill matin, Réveil en douceur, Morning Jazz"

  Scénario: Une recherche trop courte ne lance rien
    Quand je cherche " a "
    Alors aucune recherche n'est lancée
    Et on ne me propose rien

  Scénario: Coller un lien choisit directement la playlist
    Quand je cherche "https://music.youtube.com/playlist?list=PLabc123"
    Alors aucune recherche n'est lancée
    Et on me propose la playlist "PLabc123"

  Scénario: Sans réseau, la recherche l'indique
    Soit le téléphone est hors-ligne
    Quand je cherche "matin"
    Alors on m'indique "Pas de connexion à YouTube Music"

  Scénario: Une recherche sans résultat l'indique
    Quand je cherche "zzzz"
    Alors on m'indique "Aucune playlist trouvée pour « zzzz »"

  Scénario: Les résultats en double ou qui ne sont pas des playlists sont écartés
    Soit YouTube Music trouve "Chill matin, Chill matin, Une chaîne" pour "chill"
    Et le résultat "Une chaîne" n'est pas une playlist
    Quand je cherche "chill"
    Alors on me propose "Chill matin"
