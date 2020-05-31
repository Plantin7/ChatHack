Rendu : 


Tout le code source est dans le dossier Project_ChatHack


 |---> /jar  = Contient les exécutables pour le serveur et le client
 |---> /src = Contient les sources du projet
        |--->/fr/uge/nonblocking = Contient les différentes classes du projet ChatHack.
        |--->/fr/uge/protocol = Contient les protocoles de la base de donnée ainsi que du projet ChatHack
 |---> /javadoc = Contient la javadoc du projet
 |---> /documentation  = Contient toute la documentation du projet (manuel développeur, utilisateur, RFC, détail implémentations...)


Pour lancer un client (sans jar): 


>> java fr.uge.nonblocking.client.ClientChatHack  [adresse hôte]  [port] [nom du fichier contenant les login/mot de passe]  [login]  [password] (optionnel)


Pour lancer le Serveur ChatHack (sans jar) : 


>> java fr.uge.nonblocking.server.ServerChatHack [Port d’écoute du client] [adresse hôte] [port d’écoute du serveur de base de données]




Pour lancer un client (avec jar): 


>> java - jar ClientChatHack.jar  [adresse hôte]  [port] [nom du fichier contenant les login/mot de passe]  [login]  [password] (optionnel)


Pour lancer le Serveur ChatHack (avec jar) : 


>>  java - jar ServerChatHack.jar [Port d’écoute du client] [adresse hôte] [port d’écoute du serveur de base de données]