# language: fr
Fonctionnalité: Savoir si YouTube fonctionne encore avec l'extracteur
  # L'hôte prévient l'utilisateur quand YouTube a changé. Une coupure réseau, une vidéo retirée
  # ou un captcha ne disent rien de l'extracteur ; une page qu'il ne sait plus lire, si.

  Scénario: Deux pages illisibles de suite, YouTube est en panne
    Soit YouTube fonctionne
    Quand YouTube renvoie une page illisible
    Et YouTube renvoie une page illisible
    Alors YouTube est signalé en panne

  Scénario: Une réponse normale efface la panne
    Soit YouTube est en panne
    Quand YouTube répond normalement
    Alors YouTube fonctionne de nouveau

  Scénario: Une vidéo retirée ne compte pas comme une panne
    Soit YouTube fonctionne
    Quand une vidéo est retirée
    Et YouTube renvoie une page illisible
    Alors YouTube n'est pas signalé en panne

  Scénario: Un captcha ne compte pas comme une panne
    Soit YouTube est en panne
    Quand YouTube demande un captcha
    Alors YouTube est signalé en panne

  Scénario: Sans réseau, rien ne change
    Soit YouTube est en panne
    Quand le réseau est coupé
    Alors YouTube est signalé en panne

  Scénario: Au démarrage, l'état est relu chez l'hôte
    Soit l'hôte a gardé "2" pages illisibles de suite
    Quand l'app démarre
    Alors YouTube est signalé en panne

  Scénario: L'erreur arrive quand même à l'appelant
    Soit YouTube fonctionne
    Quand YouTube renvoie une page illisible
    Alors l'appelant reçoit l'erreur
