# language: fr
Fonctionnalité: Les ombres des gouttes glissent doucement vers la fenêtre
  # Une goutte qui descend la vitre projette son ombre de plus en plus près de la fenêtre :
  # sur la platine, les ombres partent du bas à droite et glissent vers la gauche.

  Scénario: Au début de la sonnerie les gouttes sont à leur place
    Quand il pleut depuis "0" s
    Alors les gouttes n'ont pas bougé

  Scénario: Les gouttes glissent vers la gauche et remontent un peu vers la fenêtre
    Quand il pleut depuis "10" s
    Alors les gouttes sont allées vers la gauche
    Et les gouttes sont allées vers le haut, moins que vers la gauche

  Scénario: Les gouttes vont doucement
    Quand il pleut depuis "1" s
    Alors les gouttes ont glissé de moins de "10" pixels

  Scénario: Les gouttes proches glissent plus vite que les lointaines
    Quand il pleut depuis "10" s
    Alors la seconde couche de gouttes est allée plus loin que la première
