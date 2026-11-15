# language: fr
Fonctionnalité: Musique au réveil, en aléatoire, avec une sonnerie de secours

  Scénario: Hors-ligne, les morceaux pré-téléchargés sont joués en aléatoire
    Soit les morceaux en cache "A,B,C"
    Et le téléphone est hors-ligne
    Quand le réveil sonne
    Alors le réveil joue un des morceaux "A,B,C"
    Et la file contient "A,B,C" puis la sonnerie de secours

  Scénario: Les morceaux en cache passent avant ceux de la playlist en ligne
    Soit les morceaux en cache "A,B"
    Et la playlist en ligne contient "A,B,X,Y"
    Quand le réveil sonne
    Alors le réveil joue un des morceaux "A,B"
    Et la file contient "A,B,X,Y" puis la sonnerie de secours

  Scénario: Sans cache, les morceaux de la playlist sont joués en streaming
    Soit la playlist en ligne contient "X,Y,Z"
    Quand le réveil sonne
    Alors le réveil joue un des morceaux "X,Y,Z"

  Scénario: Sans cache ni réseau, la sonnerie de secours retentit
    Soit le téléphone est hors-ligne
    Quand le réveil sonne
    Alors le réveil joue la sonnerie de secours
    Et le morceau en cours se termine
    Alors le réveil joue la sonnerie de secours

  Scénario: Le réveil ne commence pas deux fois de suite par le même morceau
    Soit les morceaux en cache "A,B,C"
    Alors sur "30" réveils successifs le premier morceau change à chaque fois

  Scénario: Un morceau qui échoue est sauté
    Soit les morceaux en cache "A,B"
    Quand le réveil sonne
    Et le morceau en cours échoue
    Alors le réveil joue l'autre morceau parmi "A,B"

  Scénario: Trois échecs de suite déclenchent la sonnerie de secours
    Soit la playlist en ligne contient "W,X,Y,Z"
    Quand le réveil sonne
    Et le morceau en cours échoue "3" fois
    Alors le réveil joue la sonnerie de secours

  Scénario: Un morceau qui démarre remet le compteur d'échecs à zéro
    Soit la playlist en ligne contient "W,X,Y,Z"
    Quand le réveil sonne
    Et le morceau en cours échoue "2" fois
    Et le morceau en cours démarre
    Et le morceau en cours échoue "2" fois
    Alors le réveil joue un des morceaux "W,X,Y,Z"

  Scénario: Quand la file est terminée, la musique reprend au début
    Soit les morceaux en cache "A,B"
    Quand le réveil sonne
    Et le morceau en cours démarre
    Et le morceau en cours se termine
    Et le morceau en cours démarre
    Et le morceau en cours se termine
    Alors le réveil joue un des morceaux "A,B"

  Scénario: Une file qui ne contient que des échecs finit sur la sonnerie de secours
    Soit les morceaux en cache "A"
    Quand le réveil sonne
    Et le morceau en cours échoue "3" fois
    Alors le réveil joue la sonnerie de secours
    Et le morceau en cours échoue
    Alors le réveil joue la sonnerie de secours

  Scénario: Une alarme sans playlist sonne avec la sonnerie de secours
    Soit les morceaux en cache "A,B"
    Et l'alarme n'a pas de playlist
    Quand le réveil sonne
    Alors le réveil joue la sonnerie de secours
