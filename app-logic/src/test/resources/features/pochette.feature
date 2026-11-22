# language: fr
Fonctionnalité: La pochette remplit l'étiquette du disque
  # La pochette est recadrée au centre en carré, sans être déformée, pour remplir le rond.
  # Les vignettes YouTube en 4:3 ont des bandes noires autour d'une image en 16:9 :
  # on ne garde que le carré central de l'image.

  Scénario: Une pochette carrée est gardée entière
    Quand la pochette fait "500" sur "500" pixels
    Alors on garde le carré de "500" pixels à partir de "0", "0"

  Scénario: Une image en largeur est recadrée au centre
    Quand la pochette fait "1280" sur "720" pixels
    Alors on garde le carré de "720" pixels à partir de "280", "0"

  Scénario: Une image en hauteur est recadrée au centre
    Quand la pochette fait "600" sur "800" pixels
    Alors on garde le carré de "600" pixels à partir de "0", "100"

  Scénario: Une vignette YouTube perd ses bandes noires
    Quand la pochette fait "480" sur "360" pixels
    Alors on garde le carré de "270" pixels à partir de "105", "45"
