# language: fr
Fonctionnalité: Changer de disque entre deux morceaux
  # À la fin d'un morceau, le bras se lève et le disque s'arrête ; la nouvelle pochette arrive,
  # puis le bras se repose quand le morceau suivant démarre vraiment, et le disque repart.
  # Le chargement du morceau suivant devient le geste de changer de disque.

  Contexte:
    Soit un morceau qui se termine à "60000" ms

  Scénario: Le morceau se termine, le bras se lève
    Quand on regarde à "60500" ms
    Alors le bras est soulevé
    Et le bras est au-dessus du disque
    Et le disque ralentit

  Scénario: Le disque s'arrête et la nouvelle pochette commence à se poser
    Quand on regarde à "61300" ms
    Alors le disque est à l'arrêt
    Et la nouvelle pochette se pose en fondu sur l'ancienne

  Scénario: La nouvelle pochette est posée
    Quand on regarde à "61800" ms
    Alors la nouvelle pochette est sur le disque

  Scénario: Pendant le ralenti, l'ancienne pochette tourne encore
    Quand on regarde à "60600" ms
    Alors l'ancienne pochette est sur le disque

  Scénario: Tant que le morceau suivant charge, le bras attend levé
    Quand on regarde à "64000" ms
    Alors le bras est soulevé
    Et le disque est à l'arrêt

  Scénario: Le morceau suivant démarre, le bras se pose puis le disque repart
    Soit le morceau suivant démarre à "63000" ms
    Quand on regarde à "63600" ms
    Alors le bras est posé
    Et le disque accélère

  Scénario: Tant que le bras se pose, le disque reste à l'arrêt
    Soit le morceau suivant démarre à "63000" ms
    Quand on regarde à "63100" ms
    Alors le disque est à l'arrêt

  Scénario: Le morceau suivant démarre tout de suite, le bras prend quand même le temps de changer de disque
    Soit le morceau suivant démarre à "60300" ms
    Quand on regarde à "60800" ms
    Alors le bras est soulevé

  Scénario: Ensuite le disque retrouve ses 33 tours
    Soit le morceau suivant démarre à "63000" ms
    Quand on regarde à "66000" ms
    Alors le disque tourne à "33" tours par minute

  Scénario: Un morceau qui échoue pendant le changement ne relance pas le geste
    Quand un autre morceau se termine à "62000" ms
    Alors le changement a commencé à "60000" ms

  Scénario: Une fois le disque reparti, la fin du morceau lance un nouveau changement
    Soit le morceau suivant démarre à "63000" ms
    Quand un autre morceau se termine à "200000" ms
    Alors le changement a commencé à "200000" ms
    Et le bras attend le morceau suivant

  Scénario: Le bras ne se pose qu'une fois par changement
    Soit le morceau suivant démarre à "63000" ms
    Et le morceau suivant démarre à "70000" ms
    Quand on regarde à "63600" ms
    Alors le bras est posé
