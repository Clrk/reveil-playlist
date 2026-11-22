# language: fr
Fonctionnalité: Ville de la météo
  # Il n'y a pas de ville par défaut : la météo n'apparaît qu'une fois une ville choisie.
  # La recherche passe par le service de géocodage d'Open-Meteo, gratuit et sans compte.

  Scénario: Sans ville choisie, aucune prévision n'est demandée
    Soit aucune ville choisie
    Alors aucune prévision n'est demandée

  Scénario: Une ville choisie est retenue et sa prévision demandée
    Quand je choisis la ville "Claix" à "45.12" et "5.67"
    Alors la ville retenue est "Claix"
    Et la prévision est demandée pour "45.12" et "5.67"

  Scénario: Chercher une ville interroge Open-Meteo en français
    Quand je cherche la ville "Saint-Égrève"
    Alors l'adresse appelée est "https://geocoding-api.open-meteo.com/v1/search?name=Saint-%C3%89gr%C3%A8ve&count=8&language=fr&format=json"

  Scénario: Chaque ville trouvée affiche sa région et son pays
    Soit Open-Meteo trouve "Claix" en "Auvergne-Rhône-Alpes", "France" à "45.12" et "5.67"
    Et Open-Meteo trouve "Claix" en "Nouvelle-Aquitaine", "France" à "45.6" et "0.03"
    Alors la ville 1 est "Claix" avec "Auvergne-Rhône-Alpes, France"
    Et la ville 2 est "Claix" avec "Nouvelle-Aquitaine, France"

  Scénario: Une réponse illisible ne trouve aucune ville
    Soit Open-Meteo répond "pas du json"
    Alors aucune ville n'est trouvée

  Scénario: Des données de ville abîmées sont oubliées
    Soit la ville enregistrée est illisible
    Alors aucune prévision n'est demandée

  Scénario: Une ville sans région n'affiche que son pays
    Soit Open-Meteo trouve "Monaco" sans région, en "Monaco" à "43.73" et "7.42"
    Alors la ville 1 est "Monaco" avec "Monaco"

  Scénario: Rien de trouvé
    Soit Open-Meteo répond "{}"
    Alors aucune ville n'est trouvée
