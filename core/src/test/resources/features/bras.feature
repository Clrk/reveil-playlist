# language: fr
Fonctionnalité: Le bras de la platine se relève quand on arrête
  # Glisser pour arrêter soulève le bras du disque et le ramène sur son support,
  # comme on le ferait à la main.

  Scénario: Pendant la sonnerie le bras est posé sur le disque
    Quand le curseur d'arrêt est à "0" %
    Alors le bras est posé
    Et le bras est au-dessus du disque

  Scénario: En commençant à glisser, le bras se soulève avant de bouger
    Quand le curseur d'arrêt est à "25" %
    Alors le bras est soulevé
    Et le bras est au-dessus du disque

  Scénario: À mi-course le bras soulevé part vers son support
    Quand le curseur d'arrêt est à "60" %
    Alors le bras est soulevé
    Et le bras est entre le disque et son support

  Scénario: Au bout de la glissade le bras est reposé sur son support
    Quand le curseur d'arrêt est à "100" %
    Alors le bras est posé
    Et le bras est sur son support

  Scénario: Quand la sonnerie commence, le bras attend sur son support et le disque est arrêté
    Quand la sonnerie dure depuis "0" ms
    Alors le bras est posé
    Et le bras est sur son support
    Et le disque est à l'arrêt

  Scénario: Le bras se soulève et part vers le disque
    Quand la sonnerie dure depuis "1000" ms
    Alors le bras est soulevé
    Et le bras est entre le disque et son support
    Et le disque est à l'arrêt

  Scénario: Une fois le bras posé, le disque se met à tourner doucement
    Quand la sonnerie dure depuis "2800" ms
    Alors le bras est posé
    Et le bras est au-dessus du disque
    Et le disque accélère

  Scénario: Ensuite le disque tourne à 33 tours
    Quand la sonnerie dure depuis "5000" ms
    Alors le disque tourne à "33" tours par minute
