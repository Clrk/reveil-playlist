# language: fr
Fonctionnalité: Lumière du matin sur l'écran de réveil
  # Pendant la montée du volume, la pièce passe de la nuit au plein matin :
  # d'abord un premier rayon rose, puis la tache de soleil entre par la fenêtre, puis la lumière dorée.

  Scénario: Au début de la sonnerie la pièce est dans la nuit
    Soit une montée du volume sur "60" secondes
    Quand la sonnerie commence
    Alors le premier rayon est à "0" %
    Et la tache de soleil est à "0" %
    Et l'heure est affichée à "30" % de luminosité

  Scénario: À mi-chemin le premier rayon est là et le soleil entre
    Soit une montée du volume sur "60" secondes
    Quand la sonnerie dure depuis "30" secondes
    Alors le premier rayon est à "100" %
    Et la tache de soleil est à "54" %
    Et la lumière dorée est à "0" %

  Scénario: À la fin de la montée c'est le plein matin
    Soit une montée du volume sur "60" secondes
    Quand la sonnerie dure depuis "60" secondes
    Alors la tache de soleil est à "100" %
    Et la tache a fini d'entrer dans la pièce
    Et la lumière dorée est à "75" %
    Et l'heure est affichée à "100" % de luminosité

  Scénario: Sans montée du volume, c'est le plein matin tout de suite
    Soit une montée du volume sur "0" secondes
    Quand la sonnerie commence
    Alors la tache de soleil est à "100" %
