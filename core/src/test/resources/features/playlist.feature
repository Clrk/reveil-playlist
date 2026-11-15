# language: fr
Fonctionnalité: Choisir une playlist YouTube Music par son lien

  Scénario: Les liens de playlist YouTube Music et YouTube sont reconnus
    Alors le lien "https://music.youtube.com/playlist?list=PLabc123" désigne la playlist "PLabc123"
    Et le lien "https://www.youtube.com/playlist?list=OLAK5uy_xyz-9&si=partage" désigne la playlist "OLAK5uy_xyz-9"
    Et le lien "https://music.youtube.com/browse/VLPLabc123" désigne la playlist "PLabc123"
    Et le lien "https://m.youtube.com/watch?v=dQw4w9WgXcQ&list=RDCLAKabc" désigne la playlist "RDCLAKabc"
    Et le lien "  PLabc123  " désigne la playlist "PLabc123"

  Scénario: Les liens qui ne sont pas des playlists sont refusés
    Alors le lien "https://example.com/playlist?list=PLabc123" n'est pas une playlist
    Et le lien "https://music.youtube.com/watch?v=dQw4w9WgXcQ" n'est pas une playlist
    Et le lien "bonjour" n'est pas une playlist
    Et le lien "" n'est pas une playlist
    Et le lien "https://music.youtube.com/" n'est pas une playlist
    Et le lien "https://music.youtube.com/watch?v=abc&list=pas-valide" n'est pas une playlist
    Et le lien "https://music.youtube.com/browse/UCchaine" n'est pas une playlist
    Et le lien "PL abc" n'est pas une playlist

  Scénario: Une playlist reconnue a une adresse standard pour l'extraction
    Alors l'adresse de la playlist "PLabc123" est "https://www.youtube.com/playlist?list=PLabc123"
