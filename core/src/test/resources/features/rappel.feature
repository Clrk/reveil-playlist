# language: fr
Fonctionnalité: Savoir quand et avec quoi le réveil sonnera

  Contexte:
    Soit nous sommes le "2026-11-16 22:48"

  Scénario: Enregistrer une alarme annonce le délai avant la sonnerie
    Quand je crée l'alarme "Boulot" à "07:00" répétée "mar"
    Alors on m'annonce "Réveil dans 8 h 12 min" pour l'alarme "Boulot"

  Scénario: À moins d'une heure, seules les minutes comptent
    Quand je crée l'alarme "Sieste" à "23:13" répétée "lun"
    Alors on m'annonce "Réveil dans 25 min" pour l'alarme "Sieste"

  Scénario: Une alarme exactement à l'heure pile n'affiche pas « 0 min »
    Quand je crée l'alarme "Pile" à "23:48" répétée "lun"
    Alors on m'annonce "Réveil dans 1 h" pour l'alarme "Pile"

  Scénario: Au-delà d'une journée, on compte en jours et heures
    Quand je crée l'alarme "Week-end" à "09:30" répétée "mer"
    Alors on m'annonce "Réveil dans 1 j 10 h" pour l'alarme "Week-end"

  Scénario: Une alarme désactivée n'annonce rien
    Soit l'alarme "Boulot" à "07:00" répétée "mar"
    Quand je désactive l'alarme "Boulot"
    Alors on ne m'annonce rien pour l'alarme "Boulot"

  Scénario: La carte du prochain réveil donne le jour, le délai et le nom
    Soit l'alarme "Boulot" à "07:00" répétée "mar"
    Alors la carte du prochain réveil affiche "mar. 07:00" et "dans 8 h 12 min · Boulot"

  Scénario: Une alarme sans nom n'ajoute rien après le délai
    Soit l'alarme "" à "07:00" répétée "mar"
    Alors la carte du prochain réveil affiche "mar. 07:00" et "dans 8 h 12 min"

  Scénario: Sans alarme active, la carte l'indique
    Alors la carte du prochain réveil affiche "Aucun réveil prévu" et "Appuie sur + pour en créer un"

  Scénario: Une playlist téléchargée est prête hors ligne
    Soit l'alarme "Boulot" à "07:00" répétée "mar" avec la playlist "PLchill" nommée "Chill matin"
    Quand "15" titres de sa playlist sont téléchargés
    Alors l'alarme "Boulot" indique "15 titres prêts hors ligne"

  Scénario: Un seul titre téléchargé s'écrit au singulier
    Soit l'alarme "Boulot" à "07:00" répétée "mar" avec la playlist "PLchill" nommée "Chill matin"
    Quand "1" titres de sa playlist sont téléchargés
    Alors l'alarme "Boulot" indique "1 titre prêt hors ligne"

  Scénario: Rien de téléchargé, on prévient que ça se fera la veille
    Soit l'alarme "Boulot" à "07:00" répétée "mar" avec la playlist "PLchill" nommée "Chill matin"
    Quand "0" titres de sa playlist sont téléchargés
    Alors l'alarme "Boulot" indique "Pas encore téléchargée, ce sera fait la veille"

  Scénario: Une alarme sans playlist n'a pas d'état hors ligne
    Soit l'alarme "Boulot" à "07:00" répétée "mar"
    Alors l'alarme "Boulot" n'indique rien sur le hors ligne
