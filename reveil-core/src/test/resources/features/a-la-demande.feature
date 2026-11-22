# language: fr
Fonctionnalité: Mettre un morceau demandé à voix haute
  # « Mets tel morceau » : l'hôte cherche un titre à partir d'une phrase libre, puis le rend lisible.
  # La source (YouTube par NewPipe, yt-dlp sur un serveur…) est remplaçable.

  Scénario: Le premier titre trouvé est joué
    Soit la recherche "la mer trenet" trouve "mer1, mer2"
    Quand on demande "la mer trenet"
    Alors le morceau "mer1" est prêt à jouer

  Scénario: Un titre illisible est sauté pour le suivant
    Soit la recherche "la mer trenet" trouve "mer1, mer2"
    Et le morceau "mer1" ne peut pas être lu
    Quand on demande "la mer trenet"
    Alors le morceau "mer2" est prêt à jouer

  Scénario: Les espaces autour de la demande ne comptent pas
    Soit la recherche "la mer" trouve "mer1"
    Quand on demande "  la mer  "
    Alors le morceau "mer1" est prêt à jouer

  Scénario: Une demande vide ne cherche rien
    Quand on demande "   "
    Alors aucun morceau n'est trouvé
    Et la source n'a pas été interrogée

  Scénario: Rien trouvé
    Soit la recherche "zzzz" ne trouve rien
    Quand on demande "zzzz"
    Alors aucun morceau n'est trouvé

  Scénario: La source ne répond pas
    Soit la source ne répond pas
    Quand on demande "la mer"
    Alors la source est indisponible

  Scénario: Aucun des premiers titres ne peut être lu
    Soit la recherche "la mer" trouve "a, b, c, d"
    Et le morceau "a" ne peut pas être lu
    Et le morceau "b" ne peut pas être lu
    Et le morceau "c" ne peut pas être lu
    Quand on demande "la mer"
    Alors la source est indisponible
    Et le morceau "d" n'a pas été essayé
