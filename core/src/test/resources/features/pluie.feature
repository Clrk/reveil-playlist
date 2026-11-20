# language: fr
Fonctionnalité: Quelques gouttes glissent sur la vitre et leur ombre traverse la platine
  # Comme sur une vraie vitre : la plupart des gouttes restent en place, et de temps en temps
  # l'une d'elles part, glisse par à-coups et laisse une traînée de gouttelettes.
  # Une goutte qui descend la vitre projette son ombre de plus en plus près de la fenêtre :
  # sur la platine, l'ombre part du bas à droite et glisse vers la gauche.

  Scénario: Une goutte attend un moment avant de glisser
    Quand on regarde une goutte à "0.5" s puis à "1.5" s
    Alors elle n'a pas bougé

  Scénario: La goutte glisse vers la gauche en remontant un peu vers la fenêtre
    Quand on regarde une goutte à "0" s puis à "6" s
    Alors elle est allée vers la gauche
    Et elle est allée vers le haut, moins que vers la gauche

  Scénario: La goutte glisse par à-coups
    Quand on suit une goutte pendant qu'elle glisse
    Alors elle ralentit presque jusqu'à s'arrêter puis repart

  Scénario: La goutte laisse une traînée de gouttelettes derrière elle
    Quand on regarde une goutte à "0" s puis à "5" s
    Alors des gouttelettes sont restées entre son départ et elle

  Scénario: La traînée s'efface avant que la goutte recommence
    Quand on regarde la traînée juste avant que la goutte recommence
    Alors la traînée a disparu

  Scénario: Seules quelques gouttes glissent en même temps
    Quand on regarde la vitre pendant dix minutes
    Alors en moyenne moins d'une goutte glisse à la fois
    Et jamais plus de "4" gouttes ne glissent à la fois
    Et au moins une goutte a glissé
