# language: fr
Fonctionnalité: Calcul du prochain réveil

  Scénario: Une alarme ponctuelle dont l'heure n'est pas passée sonne aujourd'hui
    Soit nous sommes le "2026-11-16 06:00"
    Quand je règle une alarme ponctuelle à "07:30"
    Alors elle sonnera le "2026-11-16 07:30"

  Scénario: Une alarme ponctuelle dont l'heure est passée sonne demain
    Soit nous sommes le "2026-11-16 08:00"
    Quand je règle une alarme ponctuelle à "07:30"
    Alors elle sonnera le "2026-11-17 07:30"

  Scénario: Une alarme réglée sur la minute en cours sonne le lendemain
    Soit nous sommes le "2026-11-16 07:30"
    Quand je règle une alarme ponctuelle à "07:30"
    Alors elle sonnera le "2026-11-17 07:30"

  Scénario: Une alarme de semaine saute le week-end
    Soit nous sommes le "2026-11-20 08:00"
    Quand je règle une alarme à "07:00" répétée "lun,mar,mer,jeu,ven"
    Alors elle sonnera le "2026-11-23 07:00"

  Scénario: Une alarme répétée sonne le jour même si l'heure n'est pas passée
    Soit nous sommes le "2026-11-20 06:00"
    Quand je règle une alarme à "07:00" répétée "ven"
    Alors elle sonnera le "2026-11-20 07:00"

  Scénario: Une alarme répétée un seul jour attend la semaine suivante
    Soit nous sommes le "2026-11-20 08:00"
    Quand je règle une alarme à "07:00" répétée "ven"
    Alors elle sonnera le "2026-11-27 07:00"

  Scénario: Le passage à l'heure d'été ne décale pas le réveil
    Soit nous sommes le "2027-03-27 23:00"
    Quand je règle une alarme ponctuelle à "07:00"
    Alors elle sonnera le "2027-03-28 07:00"
    Et ce sera dans "7" heures

  Scénario: Une alarme tombant dans l'heure sautée sonne juste après
    Soit nous sommes le "2027-03-28 01:00"
    Quand je règle une alarme ponctuelle à "02:30"
    Alors elle sonnera le "2027-03-28 03:30"

  Scénario: Une alarme désactivée ne sonne jamais
    Soit nous sommes le "2026-11-16 06:00"
    Quand je règle une alarme désactivée à "07:30"
    Alors elle ne sonnera pas
