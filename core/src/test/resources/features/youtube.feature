# language: fr
Fonctionnalité: Prévenir quand YouTube ne fonctionne plus avec l'app
  # YouTube change parfois son site et l'extracteur ne sait plus le lire. Le réveil sonne quand
  # même (sonnerie de secours), mais l'utilisateur doit savoir que sa musique risque de manquer.

  Scénario: Quand YouTube répond normalement, il n'y a pas d'alerte
    Quand YouTube répond normalement
    Alors aucune alerte YouTube n'est affichée

  Scénario: Une réponse illisible isolée ne suffit pas
    Quand YouTube répond de façon illisible "1" fois
    Alors aucune alerte YouTube n'est affichée

  Scénario: Deux réponses illisibles de suite affichent l'alerte
    Quand YouTube répond de façon illisible "2" fois
    Alors l'alerte YouTube est affichée

  Scénario: Les coupures de réseau ne comptent pas
    Quand YouTube répond de façon illisible "1" fois
    Et le réseau est coupé "3" fois
    Alors aucune alerte YouTube n'est affichée

  Scénario: Une coupure de réseau n'efface pas une alerte
    Quand YouTube répond de façon illisible "2" fois
    Et le réseau est coupé "1" fois
    Alors l'alerte YouTube est affichée

  Scénario: Dès que YouTube répond de nouveau, l'alerte disparaît
    Quand YouTube répond de façon illisible "3" fois
    Et YouTube répond normalement
    Alors aucune alerte YouTube n'est affichée
