# language: fr
Fonctionnalité: Pré-télécharger des morceaux la veille pour un réveil fiable

  Scénario: Le pré-téléchargement a lieu la veille au soir
    Soit nous sommes le "2026-11-16 12:00"
    Et le prochain réveil est le "2026-11-17 07:00"
    Alors le pré-téléchargement est prévu le "2026-11-16 22:00"

  Scénario: Si la veille au soir est déjà passée, il a lieu tout de suite
    Soit nous sommes le "2026-11-16 23:30"
    Et le prochain réveil est le "2026-11-17 07:00"
    Alors le pré-téléchargement est prévu le "2026-11-16 23:30"

  Scénario: Seuls des morceaux absents du cache sont téléchargés
    Soit la playlist en ligne contient "A,B,C,D,E,F"
    Et les morceaux en cache "A,B"
    Et le cache garde "5" morceaux
    Quand le pré-téléchargement choisit les morceaux
    Alors il télécharge "3" morceaux parmi "C,D,E,F"
    Et il ne supprime aucun morceau

  Scénario: Les morceaux retirés de la playlist sont supprimés du cache
    Soit la playlist en ligne contient "A,C"
    Et les morceaux en cache "A,B"
    Et le cache garde "5" morceaux
    Quand le pré-téléchargement choisit les morceaux
    Alors il supprime "B"
    Et il télécharge "1" morceaux parmi "C"

  Scénario: Les morceaux déjà entendus au réveil sont renouvelés
    Soit la playlist en ligne contient "A,B,C,D"
    Et les morceaux en cache "A,B"
    Et le morceau "A" a déjà été joué au réveil
    Et le cache garde "2" morceaux
    Quand le pré-téléchargement choisit les morceaux
    Alors il supprime "A"
    Et il télécharge "1" morceaux parmi "C,D"

  Scénario: Une playlist plus petite que le cache est téléchargée en entier
    Soit la playlist en ligne contient "A,B"
    Et le cache garde "5" morceaux
    Quand le pré-téléchargement choisit les morceaux
    Alors il télécharge "2" morceaux parmi "A,B"

  Scénario: Si la playlist est introuvable, le cache est conservé
    Soit les morceaux en cache "A,B"
    Et le cache garde "5" morceaux
    Quand le pré-téléchargement choisit les morceaux
    Alors il ne supprime aucun morceau
    Et il télécharge "0" morceaux parmi ""

  Scénario: Les titres ajoutés à la playlist sont téléchargés en priorité
    Soit la playlist en ligne contient "A,B,C,D,E,F"
    Et la playlist contenait "A,B,C,D" au dernier téléchargement
    Et les morceaux en cache "A,B"
    Et le cache garde "2" morceaux
    Quand le pré-téléchargement choisit les morceaux
    Alors il télécharge "2" morceaux parmi "E,F"
    Et il supprime "2" morceaux parmi "A,B"

  Scénario: Un seul ajout ne remplace qu'un morceau non joué
    Soit la playlist en ligne contient "A,B,C,D,E,F"
    Et la playlist contenait "A,B,C,D,E" au dernier téléchargement
    Et les morceaux en cache "A,B"
    Et le cache garde "2" morceaux
    Quand le pré-téléchargement choisit les morceaux
    Alors il télécharge "1" morceaux parmi "F"
    Et il supprime "1" morceaux parmi "A,B"

  Scénario: Un ajout prend d'abord une place libre
    Soit la playlist en ligne contient "A,B,C,D,E"
    Et la playlist contenait "A,B,C,D" au dernier téléchargement
    Et les morceaux en cache "A"
    Et le cache garde "3" morceaux
    Quand le pré-téléchargement choisit les morceaux
    Alors il télécharge notamment "E"
    Et il ne supprime aucun morceau

  Scénario: Un ajout remplace d'abord un morceau déjà joué
    Soit la playlist en ligne contient "A,B,C,D"
    Et la playlist contenait "A,B,C" au dernier téléchargement
    Et les morceaux en cache "A,B"
    Et le morceau "A" a déjà été joué au réveil
    Et le cache garde "2" morceaux
    Quand le pré-téléchargement choisit les morceaux
    Alors il télécharge "1" morceaux parmi "D"
    Et il supprime "A"
