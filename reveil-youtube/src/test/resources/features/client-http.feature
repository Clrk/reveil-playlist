# language: fr
Fonctionnalité: YouTube passe par le client HTTP de l'app hôte
  # Sous Android 7, l'hôte embarque ses propres certificats racines : il doit pouvoir les imposer
  # à chaque appel vers YouTube. Le module ne crée donc jamais son propre client.

  Contexte:
    Soit un hôte qui fournit son propre client HTTP

  Scénario: Chaque appel à YouTube passe par le client de l'hôte
    Quand YouTube est interrogé
    Alors la requête est passée par le client de l'hôte
    Et elle se présente comme un navigateur

  Scénario: Les en-têtes demandés par l'extracteur remplacent ceux par défaut
    Quand YouTube est interrogé avec l'en-tête "User-Agent" à "Extracteur"
    Alors la requête porte l'en-tête "User-Agent" à "Extracteur"

  Scénario: Trop de requêtes, YouTube demande de prouver qu'on n'est pas un robot
    Soit YouTube répond "429"
    Quand YouTube est interrogé
    Alors l'extracteur reçoit une demande de captcha
