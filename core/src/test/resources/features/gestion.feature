# language: fr
Fonctionnalité: Gestion des alarmes

  Contexte:
    Soit nous sommes le "2026-11-16 06:00"

  Scénario: Créer une alarme la programme auprès du téléphone
    Quand je crée l'alarme "Boulot" à "07:30" répétée "lun,mar,mer,jeu,ven"
    Alors l'alarme "Boulot" est programmée pour le "2026-11-16 07:30"
    Et la liste des alarmes contient "Boulot"

  Scénario: Les alarmes sont listées par heure
    Quand je crée l'alarme "Tard" à "09:00" répétée "lun"
    Et je crée l'alarme "Tôt" à "06:30" répétée "lun"
    Alors la liste des alarmes est "Tôt,Tard"

  Scénario: Désactiver une alarme annule sa programmation
    Soit l'alarme "Boulot" à "07:30" répétée "lun"
    Quand je désactive l'alarme "Boulot"
    Alors l'alarme "Boulot" n'est pas programmée

  Scénario: Réactiver une alarme la reprogramme
    Soit l'alarme "Boulot" à "07:30" répétée "lun"
    Quand je désactive l'alarme "Boulot"
    Et je réactive l'alarme "Boulot"
    Alors l'alarme "Boulot" est programmée pour le "2026-11-16 07:30"

  Scénario: Changer l'heure d'une alarme la reprogramme
    Soit l'alarme "Boulot" à "07:30" répétée "lun"
    Quand je change l'heure de l'alarme "Boulot" à "08:15"
    Alors l'alarme "Boulot" est programmée pour le "2026-11-16 08:15"

  Scénario: Supprimer une alarme l'oublie complètement
    Soit l'alarme "Boulot" à "07:30" répétée "lun"
    Quand je supprime l'alarme "Boulot"
    Alors l'alarme "Boulot" n'est pas programmée
    Et la liste des alarmes est ""

  Scénario: Une alarme ponctuelle se désactive après avoir sonné
    Soit l'alarme ponctuelle "Sieste" à "07:00"
    Quand l'alarme "Sieste" sonne le "2026-11-16 07:00"
    Alors l'alarme "Sieste" est désactivée
    Et l'alarme "Sieste" n'est pas programmée

  Scénario: Une alarme répétée se reprogramme après avoir sonné
    Soit l'alarme "Boulot" à "07:30" répétée "lun,mar"
    Quand l'alarme "Boulot" sonne le "2026-11-16 07:30"
    Alors l'alarme "Boulot" est programmée pour le "2026-11-17 07:30"

  Scénario: Après un redémarrage du téléphone les alarmes actives sont reprogrammées
    Soit l'alarme "Boulot" à "07:30" répétée "lun"
    Et l'alarme "Weekend" à "09:00" répétée "sam"
    Et je désactive l'alarme "Weekend"
    Et le téléphone a oublié toutes les programmations
    Quand le téléphone redémarre
    Alors l'alarme "Boulot" est programmée pour le "2026-11-16 07:30"
    Et l'alarme "Weekend" n'est pas programmée

  Scénario: Les alarmes sont conservées entre deux lancements de l'application
    Soit l'alarme "Boulot" à "07:30" répétée "lun" avec la playlist "PLabc" nommée "Chill matin"
    Quand l'application redémarre
    Alors l'alarme "Boulot" sonne à "07:30" les "lun" avec la playlist "PLabc" nommée "Chill matin"

  Scénario: Des données illisibles donnent une liste vide plutôt qu'un plantage
    Soit les données enregistrées sont corrompues
    Quand l'application redémarre
    Alors la liste des alarmes est ""

  Scénario: Le prochain réveil est le plus proche des alarmes actives
    Soit l'alarme "Boulot" à "07:30" répétée "mar"
    Et l'alarme "Sieste" à "13:00" répétée "lun"
    Et l'alarme "Weekend" à "06:30" répétée "lun"
    Et je désactive l'alarme "Weekend"
    Alors le prochain réveil est "Sieste" le "2026-11-16 13:00"

  Scénario: Sans alarme active il n'y a pas de prochain réveil
    Alors il n'y a pas de prochain réveil

  Scénario: Une alarme supprimée dont le signal arrive quand même est ignorée
    Soit l'alarme "Boulot" à "07:30" répétée "lun"
    Et je supprime l'alarme "Boulot"
    Quand l'alarme oubliée "Boulot" sonne, est repoussée, arrêtée puis réactivée
    Alors l'alarme "Boulot" n'est pas programmée
    Et la liste des alarmes est ""

  Scénario: Les réglages de snooze et de volume sont conservés
    Soit l'alarme "Boulot" à "07:30" répétée "lun" avec "5" snoozes au maximum et une montée de "120" secondes
    Quand l'application redémarre
    Alors il reste "5" snoozes à l'alarme "Boulot"
    Et le volume de l'alarme "Boulot" monte sur "120" secondes

  Scénario: Une alarme enregistrée par une ancienne version prend les réglages par défaut
    Soit l'alarme "Ancienne" enregistrée par la version 0.3.0
    Alors il reste "3" snoozes à l'alarme "Ancienne"
    Et le volume de l'alarme "Ancienne" monte sur "60" secondes

  Scénario: Une alarme sans limite de snoozes le reste après redémarrage
    Soit l'alarme "Boulot" à "07:30" répétée "lun" sans limite de snoozes
    Quand l'application redémarre
    Alors l'alarme "Boulot" n'a pas de limite de snoozes
