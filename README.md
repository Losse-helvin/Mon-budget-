# Mon budget — base Android native

> **État : base partielle, non compilée ni testé sur Android dans l'environnement de développement actuel. Aucun APK n'est livré.** Ce projet ne remplit pas encore tous les modules du cahier des charges et ne doit pas être utilisé comme unique registre de données financières.

## Architecture

- **Interface :** une activité, Jetpack Compose et Material 3, navigation à cinq onglets avec Navigation Compose ; un `BudgetViewModel` expose les flux et les opérations à l'interface.
- **Données :** Room et SQLite sur l'appareil pour enveloppes, opérations, objectifs et configuration ; DataStore pour le thème. Pas de permission Internet, de compte, de publicité ou de service distant. La sauvegarde Android automatique est désactivée.
- **Calculs :** `BudgetMath` est séparé de l'interface et possède des tests unitaires. Les montants sont des entiers `Long` en Ariary ; les dates sont des dates civiles ISO `AAAA-MM-JJ` et les périodes utilisent `java.time`.
- **Initialisation :** une transaction Room crée une seule fois les sept enveloppes demandées, le revenu mensuel prévu et l'objectif initial. Les valeurs sont modifiables. Le revenu prévu n'est pas un revenu réellement encaissé : il faut l'enregistrer dans l'historique.

### Règles financières actuellement appliquées

- Le solde est la somme des revenus enregistrés jusqu'à aujourd'hui, moins les dépenses nettes et les versements d'épargne. Un remboursement réduit les dépenses. Le montant de départ d'un objectif n'est pas ajouté au solde.
- Les transferts ajustent uniquement les budgets des enveloppes **pour le mois de la date du transfert** ; ils n'affectent ni revenus, ni dépenses, ni solde. Les budgets mensuels d'origine ne sont pas modifiés pour les autres mois.
- Les chiffres mensuels incluent les opérations datées dans tout le mois, même celles datées dans le futur ; le solde disponible et la progression des objectifs s'arrêtent à aujourd'hui.
- La consommation d'une enveloppe comprend les dépenses et les versements d'épargne, moins les remboursements. Le budget restant du tableau de bord est la somme des budgets mensuels moins ces consommations. Un budget quotidien indicatif répartit ce reste sur les jours restants, aujourd'hui compris.
- L'épargne d'un objectif correspond au montant de départ plus ses versements d'épargne liés. La simulation mensuelle inclut le mois courant et arrondit à l'ariary supérieur ; elle n'est pas affichée pour une date passée. Elle ne prévoit pas d'intérêts.
- Les seuils 70 %, 90 % et 100 % sont visibles dans l'application, mais ne produisent **pas encore de notifications**. Les limites quotidiennes et hebdomadaires sont affichées avec leur consommation, sans notification.

## Fonctions présentes dans le code

- Tableau de bord (solde enregistré, chiffres du mois, comparaison au mois précédent, aperçu de la semaine, budget quotidien indicatif, conseil simple calculé, aperçu des objectifs).
- Création, modification et suppression des enveloppes avec limites ; suppression bloquée si un historique les référence.
- Création, modification et suppression de revenus, dépenses, remboursements, épargne et transferts ; recherche et filtre par type dans l'historique.
- Création, modification et suppression d'objectifs, suivi par versements liés et simulation mensuelle ; suppression bloquée si des versements les référencent.
- Thème clair/sombre et modification du revenu prévu. Les budgets et l'objectif préremplis sont modifiables dans leurs onglets.

Ces fonctions restent à **compiler et valider sur appareil ou émulateur** avant de pouvoir garantir leur fonctionnement. Les seuils et le conseil affichés ne remplacent pas le coach complet demandé.

## Ce qui reste à réaliser

- Configuration initiale guidée, graphiques et indicateur de santé, historique détaillé par enveloppe et vues quotidienne/hebdomadaire/mensuelle complètes.
- Moteur de recommandations complet et estimation de durée d'épargne ; programme pédagogique de 30 leçons et quiz.
- Rappels et alertes par WorkManager, permissions de notifications ; export CSV/PDF, sauvegarde et restauration JSON sécurisées ; verrouillage PIN et protection des exports.
- Tests instrumentés, vérification d'accessibilité, compatibilité petits écrans et tests sur appareil ; compilation et vérification APK debug et release signée avec une clé durable. Aucun APK n'est inclus à ce stade.

## Compiler et essayer sur son propre ordinateur

1. Installer Android Studio récent, le SDK Android 35 et Java 17. La première compilation nécessite Internet pour Gradle et les dépendances ; l'application elle-même est conçue pour fonctionner hors ligne.
2. Ouvrir ce dossier dans Android Studio, attendre la synchronisation Gradle, puis lancer les tests et l'application sur un émulateur ou appareil Android 8.0 (API 26) ou plus récent.
3. En ligne de commande : `./gradlew testDebugUnitTest assembleDebug` (sur Windows : `gradlew.bat testDebugUnitTest assembleDebug`). L'APK debug, **si la commande réussit**, est créé dans `app/build/outputs/apk/debug/app-debug.apk`.
4. Installer l'APK debug depuis Android Studio ou avec `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Android peut demander l'autorisation d'installer depuis cette source. Un APK debug est destiné aux essais, pas à une distribution durable.
5. Une version release durable doit être générée et signée dans Android Studio via **Build > Generate Signed Bundle / APK** avec un keystore personnel conservé en lieu sûr. Ne jamais committer le keystore ou ses mots de passe. Conserver la même clé pour pouvoir installer les mises à jour. Cette procédure n'a pas été vérifiée ici ; aucune clé ni release signée n'est fournie.

Le workflow GitHub Actions `Vérification Android` lance tests et compilation debug et publie l'APK en artefact **si le job réussit**. Il n'a pas été exécuté dans cet environnement.

## Utilisation de la base

- Dans **Réglages**, ajuster le revenu prévu ; dans **Enveloppes**, ajuster les budgets et limites. Ils ne créent pas automatiquement de transactions.
- Utiliser le bouton **+** sur Accueil ou Historique pour enregistrer un mouvement. Choisir l'enveloppe pour une dépense ou une épargne, deux enveloppes différentes pour un transfert, et éventuellement un objectif pour un versement d'épargne.
- Dans **Historique**, rechercher une date ISO, un nom d'enveloppe ou une description, filtrer par type, modifier ou supprimer un mouvement.
- Dans **Objectifs**, ajuster l'objectif initial ou en créer d'autres. Le montant de départ représente l'épargne existante non saisie sous forme de versement, afin d'éviter de la compter deux fois.
- Les données sont stockées sur l'appareil et seront perdues à la désinstallation. L'export et la restauration ne sont pas encore disponibles.
