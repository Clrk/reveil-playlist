# language: fr
Fonctionnalité: Montée progressive du volume
  # Pour ne pas être réveillé en sursaut, la musique démarre doucement
  # puis monte jusqu'au volume réglé du téléphone.

  Scénario: Par défaut le volume monte sur une minute
    Soit une alarme aux réglages par défaut
    Alors le volume monte sur "60" secondes

  Scénario: Le volume part de 10 % et monte régulièrement
    Soit une montée du volume sur "60" secondes
    Alors au début le volume est à "10" %
    Et après "30" secondes le volume est à "55" %
    Et après "60" secondes le volume est à "100" %
    Et après "300" secondes le volume est à "100" %

  Scénario: Sans montée, le volume est au maximum tout de suite
    Soit une montée du volume sur "0" secondes
    Alors au début le volume est à "100" %
