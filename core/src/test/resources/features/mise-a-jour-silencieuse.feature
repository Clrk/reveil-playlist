# language: fr
Fonctionnalité: La mise à jour s'installe seule, sans jamais gêner un réveil
  # L'installation redémarre l'app : elle n'a lieu ni pendant une sonnerie, ni dans l'heure
  # qui précède un réveil ou la fin d'un snooze.

  Scénario: Aucun réveil dans l'heure, la mise à jour s'installe
    Soit nous sommes le "2026-11-20 23:00"
    Et un réveil sonnera le "2026-11-21 07:00"
    Alors la mise à jour peut s'installer seule

  Scénario: Un réveil dans moins d'une heure, elle attend
    Soit nous sommes le "2026-11-21 06:10"
    Et un réveil sonnera le "2026-11-21 07:00"
    Alors la mise à jour attend

  Scénario: Exactement une heure avant, elle peut encore s'installer
    Soit nous sommes le "2026-11-21 06:00"
    Et un réveil sonnera le "2026-11-21 07:00"
    Alors la mise à jour peut s'installer seule

  Scénario: Pendant une sonnerie, elle attend
    Soit nous sommes le "2026-11-21 07:01"
    Et un réveil sonne en ce moment
    Alors la mise à jour attend

  Scénario: Pendant un snooze, elle attend
    Soit nous sommes le "2026-11-21 07:05"
    Et un réveil sonnera le "2026-11-22 07:00"
    Et un snooze se termine le "2026-11-21 07:15"
    Alors la mise à jour attend

  Scénario: Sans aucun réveil programmé, elle s'installe
    Soit nous sommes le "2026-11-21 12:00"
    Alors la mise à jour peut s'installer seule
