# language: fr
Fonctionnalité: Échanger des alarmes entre deux apps de versions différentes
  # Le compagnon sur téléphone règle les réveils du Bonjour : les alarmes voyagent en JSON,
  # avec un numéro de version du format. Une app plus ancienne refuse franchement un format
  # plus récent qu'elle, au lieu de mal le lire.

  Scénario: Une alarme fait l'aller-retour sans rien perdre
    Soit une alarme à "06:45" en semaine "Travail" avec la playlist "PLmatin"
    Quand on l'envoie puis on la relit
    Alors on retrouve la même alarme

  Scénario: Le message porte le numéro de version du format
    Soit une alarme à "06:45" en semaine "Travail" avec la playlist "PLmatin"
    Quand on l'envoie
    Alors le message porte le format "1"

  Scénario: Un champ inconnu, ajouté par une version plus récente, est ignoré
    Soit un message au format "1" avec une alarme à "07:30" et le champ inconnu "lumiere"
    Quand on le relit
    Alors on lit une alarme à "07:30"

  Scénario: Un format plus récent est refusé
    Soit un message au format "2" avec une alarme à "07:30"
    Quand on le relit
    Alors le message est refusé car son format "2" est plus récent que le format "1" connu

  Scénario: Un message sans numéro de format est illisible
    Soit le message "{\"alarms\":[]}"
    Quand on le relit
    Alors le message est illisible

  Scénario: Un message abîmé est illisible
    Soit le message "{\"format\":1,\"alarms\":[{\"id\":"
    Quand on le relit
    Alors le message est illisible
