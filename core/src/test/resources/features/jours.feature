# language: fr
Fonctionnalité: Choisir les jours d'un réveil
  # À la création, les jours se touchent un par un, ou d'un coup avec un raccourci.

  Scénario: Le raccourci « En semaine » coche du lundi au vendredi
    Quand je touche le raccourci "En semaine"
    Alors les jours cochés sont "L M M J V"
    Et le raccourci "En semaine" est en surbrillance

  Scénario: Le raccourci « Le week-end » coche samedi et dimanche
    Quand je touche le raccourci "Le week-end"
    Alors les jours cochés sont "S D"

  Scénario: « Une seule fois » ne coche aucun jour
    Quand je touche le raccourci "Tous les jours"
    Et je touche le raccourci "Une seule fois"
    Alors aucun jour n'est coché
    Et le raccourci "Une seule fois" est en surbrillance

  Scénario: Toucher un jour l'ajoute ou le retire
    Quand je touche le raccourci "En semaine"
    Et je touche le jour "mercredi"
    Alors les jours cochés sont "L M J V"
    Et aucun raccourci n'est en surbrillance

  Scénario: Le résumé dit quand le réveil sonne
    Quand je touche le raccourci "Le week-end"
    Alors le résumé est "Le week-end"

  Scénario: Des jours choisis un par un se lisent en lettres
    Quand je touche le jour "lundi"
    Et je touche le jour "jeudi"
    Alors le résumé est "L J"
