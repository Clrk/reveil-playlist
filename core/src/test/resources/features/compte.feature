# language: fr
Fonctionnalité: Retrouver mes propres playlists YouTube Music grâce à ma connexion

  Scénario: Je suis connecté quand YouTube Music m'a donné son cookie de session
    Soit les cookies de YouTube Music "PREF=f1=1; SAPISID=abcDEF/123; HSID=x"
    Alors je suis connecté

  Scénario: Le cookie sécurisé suffit aussi
    Soit les cookies de YouTube Music "__Secure-3PAPISID=abcDEF/123; PREF=f1"
    Alors je suis connecté

  Scénario: Sans cookie de session je ne suis pas connecté et rien n'est signé
    Soit les cookies de YouTube Music "PREF=f1=1; VISITOR_INFO1_LIVE=xyz; SAPISID="
    Alors je ne suis pas connecté
    Quand une requête part à l'instant "1700000000"
    Alors elle ne porte pas d'autorisation

  Scénario: Les requêtes sont signées comme le fait le site YouTube Music
    Soit les cookies de YouTube Music "PREF=f1=1; SAPISID=abcDEF/123"
    Quand une requête part à l'instant "1700000000"
    Alors elle porte l'autorisation "SAPISIDHASH 1700000000_a7ca805bfb7d44212f4eb3f5e51d74ed968bb718"

  Scénario: Ma bibliothèque liste mes playlists, titres favoris compris
    Soit YouTube Music renvoie la bibliothèque "bibliotheque.json"
    Alors mes playlists sont "Titres J'aime, Réveil du lundi, Road trip"
    Et la playlist "Réveil du lundi" a l'identifiant "PLprive123"
    Et la playlist "Titres J'aime" a l'identifiant "LM"

  Scénario: Une bibliothèque illisible donne une liste vide
    Soit YouTube Music renvoie la bibliothèque "illisible.json"
    Alors mes playlists sont ""

  Scénario: Les morceaux d'une playlist privée sont lus avec ma connexion
    Soit YouTube Music renvoie la playlist "playlist.json"
    Alors ses morceaux sont "Clair de lune, Gymnopédie No.1, Titre inconnu"
    Et le morceau "Clair de lune" a l'identifiant "vid0000001"

  Scénario: Connecté, mes playlists sont lues avec mon compte, sinon sans compte
    Soit la lecture avec mon compte échoue
    Et la lecture sans compte trouve "A,B"
    Alors la playlist donne "A,B"

  Scénario: Connecté, la lecture avec mon compte est prioritaire
    Soit la lecture avec mon compte trouve "C"
    Et la lecture sans compte trouve "A,B"
    Alors la playlist donne "C"
