# language: fr
Fonctionnalité: Préparer la musique même quand le téléchargement se passe mal

  Scénario: Tous les morceaux sont téléchargés
    Soit "3" morceaux à télécharger
    Quand "3" téléchargements réussissent
    Alors la préparation est réussie
    Et la playlist est notée comme à jour

  Scénario: Une partie des morceaux échoue
    Soit "3" morceaux à télécharger
    Quand "1" téléchargements réussissent
    Alors la préparation est réussie
    Et la playlist n'est pas notée comme à jour, pour reprendre les morceaux manquants

  Scénario: Le soir, si aucun morceau n'arrive, la préparation est retentée plus tard
    Soit "3" morceaux à télécharger
    Quand "0" téléchargements réussissent
    Alors la préparation sera retentée plus tard

  Scénario: Lancée à la main, si aucun morceau n'arrive, l'échec est affiché
    Soit la préparation est lancée à la main
    Et "3" morceaux à télécharger
    Quand "0" téléchargements réussissent
    Alors la préparation est en échec

  Scénario: Le soir, une playlist injoignable est retentée plus tard
    Soit la playlist est injoignable
    Alors la préparation sera retentée plus tard

  Scénario: Lancée à la main, une playlist injoignable affiche l'échec
    Soit la préparation est lancée à la main
    Et la playlist est injoignable
    Alors la préparation est en échec

  Scénario: Rien à télécharger, la préparation est réussie
    Soit "0" morceaux à télécharger
    Quand "0" téléchargements réussissent
    Alors la préparation est réussie
    Et la playlist est notée comme à jour
