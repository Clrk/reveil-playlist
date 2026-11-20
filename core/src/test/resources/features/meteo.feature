# language: fr
Fonctionnalité: Météo du jour sur l'écran de réveil
  # Sous l'heure : le temps du matin et celui de l'après-midi, chacun avec une icône et une température.
  # La scène du réveil (pluie, neige, gel, nuages ou soleil) suit le temps du matin.

  Scénario: Matin pluvieux, après-midi couvert
    Soit de "6" h à "11" h il pleut à "8" °C
    Et de "12" h à "17" h c'est couvert à "11" °C
    Alors le matin affiche "pluie" et "8" °C
    Et l'après-midi affiche "nuages" et "11" °C
    Et la scène du réveil est "pluie"

  Scénario: Le matin affiche la plus basse température, l'après-midi la plus haute
    Soit de "6" h à "8" h le ciel est dégagé à "2" °C
    Et de "9" h à "11" h le ciel est dégagé à "5" °C
    Et de "12" h à "14" h le ciel est dégagé à "9" °C
    Et de "15" h à "17" h le ciel est dégagé à "7" °C
    Alors le matin affiche "soleil" et "2" °C
    Et l'après-midi affiche "soleil" et "9" °C

  Scénario: Un ciel dégagé sous zéro, c'est du gel
    Soit de "6" h à "11" h le ciel est dégagé à "-3" °C
    Et de "12" h à "17" h le ciel est dégagé à "4" °C
    Alors le matin affiche "gel" et "-3" °C
    Et la scène du réveil est "gel"

  Scénario: La neige l'emporte sur le reste à égalité
    Soit de "6" h à "8" h il neige à "-1" °C
    Et de "9" h à "11" h c'est couvert à "0" °C
    Et de "12" h à "17" h c'est couvert à "2" °C
    Alors le matin affiche "neige" et "-1" °C

  Scénario: Le temps le plus fréquent du matin l'emporte
    Soit de "6" h à "6" h il pleut à "6" °C
    Et de "7" h à "11" h c'est couvert à "7" °C
    Et de "12" h à "17" h le ciel est dégagé à "12" °C
    Alors le matin affiche "nuages" et "6" °C

  Scénario: Sans prévision, pas de météo et la scène est ensoleillée
    Soit aucune prévision
    Alors aucune météo n'est affichée
    Et la scène du réveil est "soleil"

  Scénario: Sans prévision pour l'après-midi, pas de météo
    Soit de "6" h à "11" h il pleut à "8" °C
    Alors aucune météo n'est affichée

  Scénario: La nuit et la soirée ne comptent pas
    Soit de "0" h à "5" h il neige à "-5" °C
    Et de "6" h à "8" h c'est couvert à "8" °C
    Et de "9" h à "11" h c'est couvert à "5" °C
    Et de "12" h à "14" h il pleut à "9" °C
    Et de "15" h à "17" h il pleut à "12" °C
    Et de "18" h à "23" h il neige à "-2" °C
    Alors le matin affiche "nuages" et "5" °C
    Et l'après-midi affiche "pluie" et "12" °C
