# language: fr
Fonctionnalité: Répéter la sonnerie plus tard (snooze)

  Contexte:
    Soit nous sommes le "2026-11-16 06:00"

  Scénario: Le snooze reporte la sonnerie de la durée choisie
    Soit l'alarme "Boulot" à "07:30" répétée "lun" avec un snooze de "9" minutes
    Quand l'alarme "Boulot" sonne le "2026-11-16 07:30"
    Et je repousse l'alarme "Boulot" à "07:31"
    Alors l'alarme "Boulot" sonnera à nouveau le "2026-11-16 07:40"
    Et l'alarme "Boulot" est programmée pour le "2026-11-23 07:30"

  Scénario: Le snooze par défaut dure 10 minutes
    Soit l'alarme ponctuelle "Sieste" à "07:00"
    Quand l'alarme "Sieste" sonne le "2026-11-16 07:00"
    Et je repousse l'alarme "Sieste" à "07:00"
    Alors l'alarme "Sieste" sonnera à nouveau le "2026-11-16 07:10"

  Scénario: Arrêter l'alarme annule le snooze en cours
    Soit l'alarme "Boulot" à "07:30" répétée "lun"
    Quand l'alarme "Boulot" sonne le "2026-11-16 07:30"
    Et je repousse l'alarme "Boulot" à "07:30"
    Et j'arrête l'alarme "Boulot"
    Alors l'alarme "Boulot" n'a pas de snooze en attente

  Scénario: Supprimer une alarme annule aussi son snooze
    Soit l'alarme "Boulot" à "07:30" répétée "lun"
    Quand l'alarme "Boulot" sonne le "2026-11-16 07:30"
    Et je repousse l'alarme "Boulot" à "07:30"
    Et je supprime l'alarme "Boulot"
    Alors l'alarme "Boulot" n'a pas de snooze en attente

  Scénario: Le snooze survit à un redémarrage du téléphone
    Soit l'alarme ponctuelle "Sieste" à "07:00"
    Quand l'alarme "Sieste" sonne le "2026-11-16 07:00"
    Et je repousse l'alarme "Sieste" à "07:00"
    Et le téléphone a oublié toutes les programmations
    Et le téléphone redémarre
    Alors l'alarme "Sieste" sonnera à nouveau le "2026-11-16 07:10"

  Scénario: Un snooze déjà expiré n'est pas reprogrammé au redémarrage
    Soit l'alarme ponctuelle "Sieste" à "07:00"
    Quand l'alarme "Sieste" sonne le "2026-11-16 07:00"
    Et je repousse l'alarme "Sieste" à "07:00"
    Et le téléphone a oublié toutes les programmations
    Et le téléphone redémarre le "2026-11-16 08:00"
    Alors l'alarme "Sieste" n'a pas de snooze en attente

  Scénario: Par défaut on peut repousser trois fois
    Soit l'alarme "Boulot" à "07:30" répétée "lun"
    Quand l'alarme "Boulot" sonne le "2026-11-16 07:30"
    Alors il reste "3" snoozes à l'alarme "Boulot"

  Scénario: Chaque report consomme un snooze, même quand le snooze sonne
    Soit l'alarme "Boulot" à "07:30" répétée "lun"
    Quand l'alarme "Boulot" sonne le "2026-11-16 07:30"
    Et je repousse l'alarme "Boulot" à "07:30"
    Et le snooze de l'alarme "Boulot" sonne le "2026-11-16 07:40"
    Alors il reste "2" snoozes à l'alarme "Boulot"

  Scénario: Une fois les snoozes épuisés, on ne peut plus repousser
    Soit l'alarme "Boulot" à "07:30" répétée "lun" avec "2" snoozes au maximum
    Quand l'alarme "Boulot" sonne le "2026-11-16 07:30"
    Et je repousse l'alarme "Boulot" à "07:30"
    Et le snooze de l'alarme "Boulot" sonne le "2026-11-16 07:40"
    Et je repousse l'alarme "Boulot" à "07:40"
    Et le snooze de l'alarme "Boulot" sonne le "2026-11-16 07:50"
    Alors il reste "0" snoozes à l'alarme "Boulot"
    Et je ne peux plus repousser l'alarme "Boulot" à "07:50"
    Et l'alarme "Boulot" n'a pas de snooze en attente

  Scénario: Le compteur repart de zéro au réveil suivant
    Soit l'alarme "Boulot" à "07:30" répétée "lun,mar" avec "1" snoozes au maximum
    Quand l'alarme "Boulot" sonne le "2026-11-16 07:30"
    Et je repousse l'alarme "Boulot" à "07:30"
    Et le snooze de l'alarme "Boulot" sonne le "2026-11-16 07:40"
    Et l'alarme "Boulot" sonne le "2026-11-17 07:30"
    Alors il reste "1" snoozes à l'alarme "Boulot"

  Scénario: Arrêter l'alarme remet le compteur à zéro
    Soit l'alarme "Boulot" à "07:30" répétée "lun"
    Quand l'alarme "Boulot" sonne le "2026-11-16 07:30"
    Et je repousse l'alarme "Boulot" à "07:30"
    Et j'arrête l'alarme "Boulot"
    Alors il reste "3" snoozes à l'alarme "Boulot"

  Scénario: Sans limite, on peut toujours repousser
    Soit l'alarme "Boulot" à "07:30" répétée "lun" sans limite de snoozes
    Quand l'alarme "Boulot" sonne le "2026-11-16 07:30"
    Et je repousse l'alarme "Boulot" à "07:30"
    Et je repousse l'alarme "Boulot" à "07:40"
    Et je repousse l'alarme "Boulot" à "07:50"
    Et je repousse l'alarme "Boulot" à "08:00"
    Alors l'alarme "Boulot" n'a pas de limite de snoozes
    Et l'alarme "Boulot" sonnera à nouveau le "2026-11-16 08:10"
