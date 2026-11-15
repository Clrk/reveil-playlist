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
