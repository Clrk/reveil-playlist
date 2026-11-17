# language: fr
Fonctionnalité: Rappel avant le réveil et réveil sauté
  # Une heure avant, une notification rappelle le réveil et permet de l'ignorer.
  # On peut aussi sauter une seule occurrence d'une alarme répétée.

  Contexte:
    Soit nous sommes le "2026-11-16 06:00"

  Scénario: Un rappel est programmé une heure avant le réveil
    Quand je crée l'alarme "Boulot" à "07:30" répétée "lun,mar"
    Alors le rappel de l'alarme "Boulot" est programmé pour le "2026-11-16 06:30"

  Scénario: Trop tard pour un rappel, il passe au réveil suivant
    Soit nous sommes le "2026-11-16 06:45"
    Quand je crée l'alarme "Boulot" à "07:30" répétée "lun,mar"
    Alors l'alarme "Boulot" est programmée pour le "2026-11-16 07:30"
    Et l'alarme "Boulot" n'a pas de rappel

  Scénario: Une alarme désactivée n'a pas de rappel
    Soit l'alarme "Boulot" à "07:30" répétée "lun"
    Quand je désactive l'alarme "Boulot"
    Alors l'alarme "Boulot" n'a pas de rappel

  Scénario: Sauter le prochain réveil d'une alarme répétée
    Soit l'alarme "Boulot" à "07:30" répétée "lun,mar"
    Quand je saute le prochain réveil de l'alarme "Boulot"
    Alors l'alarme "Boulot" est programmée pour le "2026-11-17 07:30"
    Et le rappel de l'alarme "Boulot" est programmé pour le "2026-11-17 06:30"
    Et l'alarme "Boulot" indique "Réveil de lun. 07:30 sauté"
    Et le prochain réveil est "Boulot" le "2026-11-17 07:30"

  Scénario: Le saut ne concerne qu'une seule fois
    Soit l'alarme "Boulot" à "07:30" répétée "lun"
    Quand je saute le prochain réveil de l'alarme "Boulot"
    Alors l'alarme "Boulot" est programmée pour le "2026-11-23 07:30"
    Quand l'alarme "Boulot" sonne le "2026-11-23 07:30"
    Alors l'alarme "Boulot" est programmée pour le "2026-11-30 07:30"
    Et l'alarme "Boulot" n'indique aucun saut

  Scénario: Annuler le saut remet le réveil prévu
    Soit l'alarme "Boulot" à "07:30" répétée "lun,mar"
    Quand je saute le prochain réveil de l'alarme "Boulot"
    Et j'annule le saut de l'alarme "Boulot"
    Alors l'alarme "Boulot" est programmée pour le "2026-11-16 07:30"
    Et l'alarme "Boulot" n'indique aucun saut

  Scénario: Ignorer une alarme ponctuelle la désactive
    Soit l'alarme ponctuelle "Sieste" à "07:00"
    Quand je saute le prochain réveil de l'alarme "Sieste"
    Alors l'alarme "Sieste" est désactivée
    Et l'alarme "Sieste" n'est pas programmée

  Scénario: Le saut est conservé entre deux lancements
    Soit l'alarme "Boulot" à "07:30" répétée "lun,mar"
    Quand je saute le prochain réveil de l'alarme "Boulot"
    Et l'application redémarre
    Alors l'alarme "Boulot" est programmée pour le "2026-11-17 07:30"

  Scénario: Sauter une alarme inconnue ne fait rien
    Soit l'alarme "Boulot" à "07:30" répétée "lun"
    Et je supprime l'alarme "Boulot"
    Quand je saute le prochain réveil de l'alarme "Boulot"
    Et j'annule le saut de l'alarme "Boulot"
    Alors la liste des alarmes est ""
