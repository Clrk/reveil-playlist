# language: fr
Fonctionnalité: Une bibliothèque locale réglée par l'app hôte
  # Le téléphone garde 15 morceaux, renouvelés chaque soir. Un autre hôte (le Bonjour) peut en
  # garder bien plus, sur plusieurs semaines, dans une taille maximale.
  # Un morceau trop ancien n'est remplacé que lorsqu'un nouveau est arrivé, comme un morceau déjà entendu.

  Scénario: Par défaut, rien ne change pour le téléphone
    Soit les règles par défaut de la bibliothèque
    Alors la bibliothèque garde "15" morceaux
    Et aucun morceau n'expire avec l'âge
    Et la taille de la bibliothèque n'est pas limitée

  Scénario: Un hôte garde autant de morceaux qu'il veut
    Soit une bibliothèque de "80" morceaux
    Et la playlist en ligne contient "100" morceaux
    Quand le pré-téléchargement choisit les morceaux
    Alors il télécharge "80" morceaux

  Scénario: Un morceau gardé trop longtemps est à renouveler
    Soit une bibliothèque qui garde les morceaux "21" jours
    Et nous sommes le "2026-12-30 20:00"
    Et le morceau "A" est arrivé le "2026-12-01 21:00"
    Et le morceau "B" est arrivé le "2026-12-20 21:00"
    Alors le morceau "A" est à renouveler
    Et le morceau "B" est gardé

  Scénario: Sans durée de conservation, un vieux morceau est gardé
    Soit les règles par défaut de la bibliothèque
    Et nous sommes le "2027-06-30 20:00"
    Et le morceau "A" est arrivé le "2026-12-01 21:00"
    Alors le morceau "A" est gardé

  Scénario: Au-delà de la taille maximale, les plus anciens morceaux partent
    Soit une bibliothèque de "10" Mo au plus
    Et le morceau "A" de "4" Mo est arrivé le "2026-12-01 21:00"
    Et le morceau "B" de "4" Mo est arrivé le "2026-12-02 21:00"
    Et le morceau "C" de "4" Mo est arrivé le "2026-12-03 21:00"
    Alors la bibliothèque retire "A"

  Scénario: Sous la taille maximale, rien ne part
    Soit une bibliothèque de "10" Mo au plus
    Et le morceau "A" de "4" Mo est arrivé le "2026-12-01 21:00"
    Et le morceau "B" de "4" Mo est arrivé le "2026-12-02 21:00"
    Alors la bibliothèque ne retire rien
