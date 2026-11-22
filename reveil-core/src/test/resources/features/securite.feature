# language: fr
Fonctionnalité: Le réveil sonne toujours, même quand la musique ne vient pas

  Scénario: Un morceau qui ne démarre pas en 10 secondes est sauté
    Soit les morceaux en cache "A,B"
    Quand le réveil sonne
    Et rien ne démarre pendant "10" secondes
    Alors le réveil joue l'autre morceau parmi "A,B"

  Scénario: Un morceau qui charge encore à 9 secondes n'est pas sauté
    Soit les morceaux en cache "A,B"
    Quand le réveil sonne
    Et rien ne démarre pendant "9" secondes
    Alors le réveil joue toujours le premier morceau

  Scénario: Un morceau qui démarre lentement n'est plus sauté ensuite
    Soit les morceaux en cache "A,B"
    Quand le réveil sonne
    Et rien ne démarre pendant "8" secondes
    Et le morceau en cours démarre
    Et "60" secondes passent
    Alors le réveil joue toujours le premier morceau

  Scénario: Trois morceaux muets de suite donnent la sonnerie de secours en 30 secondes
    Soit la playlist en ligne contient "W,X,Y,Z"
    Quand le réveil sonne
    Et rien ne démarre pendant "29" secondes
    Alors le réveil joue un des morceaux "W,X,Y,Z"
    Et rien ne démarre pendant "1" secondes
    Alors le réveil joue la sonnerie de secours

  Scénario: Un morceau dont le lien est introuvable est sauté tout de suite
    Soit la playlist en ligne contient "X,Y"
    Quand le réveil sonne
    Et le lien du morceau en cours est introuvable
    Alors le réveil joue l'autre morceau parmi "X,Y"

  Scénario: La sonnerie de secours n'est jamais sautée faute de démarrer
    Soit le téléphone est hors-ligne
    Quand le réveil sonne
    Et rien ne démarre pendant "60" secondes
    Alors le réveil joue la sonnerie de secours

  Scénario: Si la sonnerie de secours échoue, l'alarme du téléphone prend le relais
    Soit le téléphone est hors-ligne
    Quand le réveil sonne
    Et le morceau en cours échoue
    Alors l'alarme du téléphone sonne en dernier recours

  Scénario: Tant que la musique peut encore jouer, pas de dernier recours
    Soit les morceaux en cache "A,B"
    Quand le réveil sonne
    Et le morceau en cours échoue "2" fois
    Alors l'alarme du téléphone n'est pas utilisée

  Scénario: Le réveil s'arrête seul au bout de 30 minutes
    Soit les morceaux en cache "A,B"
    Quand le réveil sonne
    Et le morceau en cours démarre
    Alors à "1799" secondes le réveil sonne encore
    Et à "1800" secondes le réveil s'arrête

  Scénario: Il s'arrête aussi au bout de 30 minutes sur la sonnerie de secours
    Soit le téléphone est hors-ligne
    Quand le réveil sonne
    Alors à "1800" secondes le réveil s'arrête
