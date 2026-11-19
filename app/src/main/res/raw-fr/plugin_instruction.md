MCP Server permet aux clients IA de votre ordinateur, comme Claude Code et Cursor, de piloter ce téléphone via le [Model Context Protocol](https://modelcontextprotocol.io). Une fois connecté en USB ou sur le réseau local, un client peut utiliser AutoJs6 pour exécuter des scripts, examiner et manipuler l'écran, prendre des captures d'écran, et lire et écrire des fichiers dans le répertoire de travail. Chaque nouveau client doit être confirmé sur le téléphone lors de son premier appel d'outil.

### Prise en main

1. Activez l'interrupteur "Serveur MCP" dans le tiroir d'AutoJs6, et activez le plugin si cela vous est demandé la première fois.
2. Appuyez longuement sur l'élément ou ouvrez les paramètres du plugin dans le "Centre des plugins", puis copiez la configuration de votre client depuis "Configuration du client" et ajoutez-la au client MCP de votre ordinateur.
3. Reliez le téléphone à l'ordinateur en USB et exécutez sur l'ordinateur la commande adb forward affichée sous "Connexion" (appuyez sur "Copier la commande adb forward" pour la copier).
4. Lorsque le client appelle un outil pour la première fois, appuyez sur "Autoriser" sur le téléphone pour confirmer l'association.

### Fonctionnalités principales

- **Scripts et écran**: les clients peuvent exécuter des scripts dans AutoJs6 et lire la sortie de la console, exporter l'arborescence des nœuds d'accessibilité, trouver des contrôles puis appuyer, saisir ou faire défiler, et prendre des captures d'écran.
- **Fichiers et appareil**: lire et écrire des fichiers dans le répertoire de travail d'AutoJs6, lancer des applications, lire et écrire le presse-papiers. Avec "Autoriser la lecture du stockage partagé" activé, les clients peuvent aussi lire, sans les modifier, des emplacements comme la galerie et le dossier des téléchargements.
- **Autorisations des outils**: activez ou désactivez les groupes d'outils proposés aux clients; un groupe désactivé n'apparaît pas dans la liste d'outils du client. "Suppression de fichiers", "Gestes par coordonnées" et "Commandes Shell" sont désactivés par défaut.
- **Outils des plugins**: une fois des plugins comme OCR, Barcode et OpenCV installés, les clients peuvent reconnaître du texte et des codes-barres et rechercher des images et des couleurs. Les outils déclarés par d'autres plugins s'activent dans "Outils des plugins".
- **Scripts enregistrés**: les scripts enregistrés par un commentaire `@agent` sont proposés aux clients, et le modèle peut les exécuter avec les paramètres enregistrés; pour les scripts qui demandent une confirmation, le client sollicite votre accord avant de les exécuter.
- **Jeton et association**: les clients se connectent avec un jeton d'accès, dont la page des paramètres n'affiche que les 4 derniers caractères. Révoquez les associations dans "Clients associés"; après "Renouveler le jeton", l'ancien jeton cesse aussitôt de fonctionner.
- **Connexions du réseau local**: avec "Autoriser les connexions du réseau local" activé, les ordinateurs du même réseau peuvent se connecter sans USB; le jeton et la confirmation d'association restent requis.
- **Arrêt automatique en cas d'inactivité**: arrête le serveur si aucune requête n'arrive dans le délai choisi; désactivé par défaut.

### Prérequis

- Android 7.0 ou version ultérieure et AutoJs6 6.8.0 ou version ultérieure.
- Un client MCP sur l'ordinateur; une connexion USB nécessite aussi adb. Les clients qui ne prennent en charge que stdio, comme Claude Desktop, ont besoin du [pont](https://github.com/SuperMonster003/AutoJs6-MCP-Bridge) installé à part.
- Les opérations à l'écran et les captures d'écran nécessitent le service d'accessibilité d'AutoJs6; sur certains appareils, les captures d'écran demandent aussi une autorisation sur le téléphone.
- L'autorisation des notifications permet de confirmer les demandes d'association depuis n'importe quel écran; à partir d'Android 17, ce plugin doit aussi être autorisé à accéder aux appareils à proximité (pour le réseau local).
- Les outils des plugins, comme la reconnaissance de texte et de codes-barres, nécessitent le plugin correspondant installé et activé dans AutoJs6.

### Questions fréquentes

**Raisons pour lesquelles le client signale 401 Unauthorized**

Le jeton est absent, mal saisi ou renouvelé. Copiez de nouveau la configuration depuis la page des paramètres; après un renouvellement du jeton, chaque client doit utiliser le nouveau.

**Marche à suivre quand la connexion est refusée**

Vérifiez que l'interrupteur "Serveur MCP" du tiroir est activé. Reconnecter l'appareil ou redémarrer adb efface la redirection de port; exécutez alors de nouveau la commande adb forward sur l'ordinateur.

**Raisons de l'arrêt du serveur écran éteint**

Lorsque le système limite l'utilisation de la batterie par cette application, il peut arrêter le serveur écran éteint. La page des paramètres affiche alors un avertissement; appuyez sur "Paramètres de batterie" et choisissez "Sans restriction" pour MCP Server.

**Raisons pour lesquelles un outil n'apparaît pas dans le client**

Son groupe est désactivé dans "Autorisations des outils", ou le plugin requis n'est pas installé ou activé dans AutoJs6. La liste d'outils du client se met à jour automatiquement une fois le groupe ou le plugin activé.

La configuration de chaque client, la liste des outils et les notes de sécurité figurent sur la [page du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server).
