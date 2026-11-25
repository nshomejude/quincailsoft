import type { ProductPageData, ProductUi } from "../product-types";
import type { ProductSlug } from "../product-routes";

export const ui: ProductUi = {
  status: {
    available: "Disponible dans la tranche de migration",
    partial: "Partiellement disponible",
    planned: "Prévu",
    pending: "En attente / désactivé pour l'instant",
  },
  onThisPage: "Sur cette page",
  related: "Pages liées",
  contact: "Contactez-nous",
  seeFeatures: "Voir les fonctionnalités",
  author: "Opesware Technologies",
  published: "Publié le 25 novembre 2026",
  readGuide: "Lire le guide",
  limitsTitle: "Limites, en toute transparence",
  moduleWhat: "Ce que cela fait",
  moduleHow: "Comment cela fonctionne",
  guidesTitle: "Guides pratiques",
  relatedGuides: "Guides liés",
  resources: "Ressources",
  minutes: "min de lecture",
};

const CTA = {
  ctaTitle: "Envie de voir si cela convient à votre activité ?",
  ctaBody: "Parlez-nous de votre quincaillerie ou de votre activité de distribution. Nous répondons par e-mail en français ou en anglais, en disant clairement ce qui est prêt et ce qui est en cours de construction.",
};

export const products: Record<ProductSlug, ProductPageData> = {
  "mobile-app": {
    icon: "phone",
    kicker: "Application mobile",
    metaTitle: "Application mobile pour dirigeants et vendeurs | QuincaiSoft",
    description: "L'application mobile QuincaiSoft est incluse dans chaque formule : connexion sécurisée, catalogue et ventes, brouillons de vente hors ligne chiffrés. La comptabilisation économique hors ligne est désactivée pour l'instant.",
    h1: "Une application mobile dans chaque formule, conçue pour une connexion faible",
    answer: "QuincaiSoft inclut une application mobile native, construite avec Expo et React Native, pour les dirigeants, les responsables et les vendeurs, dans chaque formule. Aujourd'hui, elle se connecte via votre fournisseur d'identité, recherche dans le catalogue, affiche les ventes et conserve des brouillons de vente dans une file locale chiffrée, envoyée au retour de la connexion. La comptabilisation hors ligne des ventes terminées et des mouvements de stock est désactivée tant que les quotas de stock signés et les preuves sur appareils réels n'existent pas.",
    glance: {
      title: "En un coup d'œil",
      items: [
        { label: "Connexion sécurisée", status: "available", note: "OpenID Connect avec PKCE dans le navigateur système." },
        { label: "Catalogue et ventes", status: "available", note: "Limités à votre société et votre succursale." },
        { label: "Brouillons de vente hors ligne", status: "available", note: "Brouillons seulement. L'application ne prétend jamais avoir comptabilisé stock ou argent." },
        { label: "Ventes terminées hors ligne", status: "pending", note: "Désactivées tant que quotas signés et preuves sur appareils ne sont pas complets." },
      ],
    },
    blocks: [
      {
        kind: "cards", id: "today", title: "Ce que l'application fait aujourd'hui",
        items: [
          { icon: "key", status: "available", title: "Connexion sécurisée", body: "La connexion utilise le flux OpenID Connect Authorization Code avec PKCE dans le navigateur système. Les jetons sont conservés dans le magasin sécurisé du système, jamais dans le stockage du navigateur, et aucun secret client n'est embarqué." },
          { icon: "search", status: "available", title: "Recherche et fiches produit", body: "Recherchez et actualisez le catalogue, puis ouvrez la fiche d'un produit, lue depuis l'API QuincaiSoft dans le périmètre de votre société et de votre succursale." },
          { icon: "cart", status: "available", title: "Liste et détail des ventes", body: "Consultez les ventes avec leurs lignes, client, entrepôt, date et montants. Les quantités sont saisies comme des valeurs exactes, sans arrondi silencieux." },
          { icon: "walloff", status: "available", title: "Brouillons de vente hors ligne", body: "Saisissez un brouillon de vente sans connexion. Il attend dans une file locale durable et est transmis quand vous choisissez de l'envoyer. Seule la création de brouillons est implémentée sur mobile." },
          { icon: "database", status: "available", title: "Base locale chiffrée", body: "Chaque périmètre a sa propre base SQLCipher chiffrée, avec une clé aléatoire de 256 bits dans le magasin sécurisé. Si le chiffrement natif est absent, l'application refuse de stocker des données en clair." },
          { icon: "route", status: "available", title: "Nouvelles tentatives sûres", body: "Un brouillon dont l'arrivée est incertaine est renvoyé avec son identifiant d'opération d'origine : une répétition ne peut pas créer une seconde vente." },
        ],
      },
      {
        kind: "table", id: "matrix", alt: true, title: "Ce qui fonctionne en ligne et hors ligne",
        intro: "Voici la matrice de capacités visée et la situation de chaque fonction aujourd'hui.",
        caption: "Capacités mobiles selon l'état de la connexion",
        head: ["Fonction", "En ligne", "Hors ligne", "État aujourd'hui"],
        rows: [
          ["Consultation du catalogue et des prix", "Oui", "Catalogue en cache en lecture seule (étape 1)", "En ligne disponible ; cache hors ligne prévu"],
          ["Brouillon de vente", "Oui", "Oui, en file locale", "Disponible"],
          ["Saisie d'inventaire", "Oui", "Comptages en brouillon, validés après synchronisation (étape 2)", "Prévu sur mobile"],
          ["Vente au comptant", "Oui", "Contrôlée, dans un quota de stock signé (étape 3)", "Hors ligne désactivé"],
          ["Vente à crédit", "Oui", "Restreinte par une autorisation de crédit en cache", "Prévu"],
          ["Vérification mobile money", "Oui", "Jamais. Reste en attente jusqu'à confirmation du fournisseur en ligne.", "Par conception"],
          ["Remboursements et ajustements de caisse sensibles", "Oui, avec authentification renforcée", "Restreint", "Prévu"],
          ["Rôles utilisateurs et facturation", "Oui, avec MFA", "Non", "Par conception"],
        ],
      },
      {
        kind: "steps", id: "stages", title: "Comment la vente hors ligne sera déployée",
        intro: "Nous déployons la capacité hors ligne par étapes, et seulement quand chaque étape s'appuie sur des preuves.",
        items: [
          { title: "Étape 1 : en ligne, avec catalogue en cache en lecture seule", body: "Consulter produits et prix quand la connexion est faible." },
          { title: "Étape 2 : inventaires hors ligne", body: "Saisir les comptages en rayon. Un responsable les valide et les comptabilise après synchronisation." },
          { title: "Étape 3 : ventes au comptant hors ligne contrôlées", body: "Vendre dans un quota de stock signé, une politique hors ligne et des limites fixées pour l'appareil. Cette étape reste désactivée tant que le quota et les preuves sur appareils ne sont pas complets." },
        ],
      },
      {
        kind: "cards", id: "safety", alt: true, title: "Comment les données hors ligne sont protégées",
        items: [
          { icon: "fingerprint", status: "available", title: "Enrôlement de l'appareil", body: "Un appareil prouve qu'il possède une clé RSA d'au moins 2 048 bits et est approuvé par un propriétaire. Cela n'établit pas d'attestation matérielle, et l'approbation n'accorde pas la vente hors ligne." },
          { icon: "layers", status: "available", title: "Brouillons et faits terminés séparés", body: "Les enregistrements locaux sont classés brouillon, terminé-autorisé ou terminé-exception. Les preuves de transactions terminées sont conservées pour revue et jamais envoyées comme un brouillon ordinaire." },
          { icon: "shield", status: "available", title: "Quarantaine de récupération", body: "La récupération signée par un propriétaire conserve côte à côte les historiques en conflit pour revue, sans écraser aucun d'eux." },
        ],
      },
      {
        kind: "limits", id: "limits", title: "Limites, en toute transparence",
        items: [
          "La comptabilisation hors ligne des ventes terminées est désactivée. Les brouillons sont le seul enregistrement hors ligne créé par l'application.",
          "Les paiements mobile money ne peuvent pas être vérifiés hors ligne. Ils restent en attente jusqu'à confirmation en ligne du fournisseur.",
          "Une compilation réussie n'est pas une certification sur appareils réels. Les preuves sur appareils natifs restent à produire.",
          "Le scan de codes-barres par la caméra est dans l'architecture mais pas dans la tranche mobile livrée.",
          "Déconnectée, l'application ne peut pas garantir le stock ou les soldes en temps réel.",
        ],
      },
      {
        kind: "faq", id: "faq", alt: true, title: "Questions sur l'application mobile",
        items: [
          { q: "L'application mobile est-elle incluse dans la formule ?", a: "Oui. Chaque formule inclut l'accès mobile aux fonctions auxquelles vous souscrivez. Le prix dépend de l'échelle et des droits, pas d'un abonnement séparé pour l'application." },
          { q: "Puis-je vendre sans internet aujourd'hui ?", a: "Vous pouvez enregistrer un brouillon de vente hors ligne, mais ce n'est pas une vente terminée. Les ventes terminées hors ligne sont désactivées tant que quotas signés et preuves sur appareils ne sont pas en place." },
          { q: "Sur quels téléphones fonctionne-t-elle ?", a: "Elle est construite avec Expo et React Native pour téléphones. Les tests et la certification sur appareils natifs ne sont pas terminés : demandez-nous les appareils pilotes avant de planifier un déploiement." },
          { q: "Que se passe-t-il si un téléphone est perdu ?", a: "Les données locales sont chiffrées. Un propriétaire peut modifier ou désactiver l'accès d'un membre, ce qui incrémente une époque d'autorisation : les anciens jetons sont alors rejetés par l'API." },
        ],
      },
    ],
    related: ["features", "payments", "security", "desktop", "resources/run-multiple-branches"],
    ...CTA,
  },

  payments: {
    icon: "card",
    kicker: "Paiements",
    metaTitle: "Paiements, crédit et MTN Mobile Money | QuincaiSoft",
    description: "Comment QuincaiSoft gère espèces, crédit client, acomptes et MTN Mobile Money. Orange Money n'est pas intégré et QuincaiSoft ne détient pas l'argent de vos clients.",
    h1: "Paiements : espèces, crédit, acomptes et MTN Mobile Money",
    answer: "QuincaiSoft enregistre aujourd'hui les espèces, le crédit client, les acomptes et les paiements échelonnés, et inclut un adaptateur MTN Mobile Money qui nécessite encore des identifiants réels et l'acceptation de MTN avant de manipuler de l'argent réel. Orange Money n'est pas intégré. Ni QuincaiSoft ni Opesware Technologies ne détiennent l'argent de vos clients : les paiements appartiennent au commerçant et à son prestataire de paiement agréé.",
    glance: {
      title: "En un coup d'œil",
      items: [
        { label: "Espèces et sessions de caisse", status: "available", note: "Fonds de caisse, clôture comptée et revue des écarts." },
        { label: "Crédit, acomptes, encaissements", status: "available", note: "Créances, soldes d'acomptes et affectations en FCFA." },
        { label: "MTN Mobile Money", status: "partial", note: "Transport et rapprochement construits. Identifiants réels et acceptation en attente." },
        { label: "Orange Money", status: "planned", note: "Non intégré. En attente d'une intégration contractualisée." },
      ],
    },
    blocks: [
      {
        kind: "cards", id: "tenders", title: "Comment chaque moyen de paiement est enregistré",
        items: [
          { icon: "banknote", status: "available", title: "Espèces", body: "Une session de caisse s'ouvre avec un fonds déclaré. Les espèces attendues sont dérivées des enregistrements de caisse immuables de la session, et la clôture les compare au montant compté. Un écart est examiné, jamais effacé." },
          { icon: "wallet", status: "available", title: "Crédit client", body: "Une vente à crédit crée une créance. Les encaissements s'appliquent à la dette reconnue, et un encaissement supérieur à la dette est rejeté plutôt que masqué." },
          { icon: "landmark", status: "available", title: "Acomptes et paiements échelonnés", body: "Un acompte est conservé comme dette envers le client, puis affecté à une vente sans second encaissement. Le remboursement en espèces d'un acompte inutilisé exige sa propre permission et ne peut dépasser le solde restant." },
          { icon: "receipt", status: "partial", title: "Virement et références manuelles", body: "Les références manuelles d'un virement ou d'un paiement mobile money sont enregistrées comme non vérifiées. Elles ne sont jamais présentées comme confirmées par le fournisseur parce qu'un numéro semble valide." },
        ],
      },
      {
        kind: "steps", id: "mtn", alt: true, title: "Comment fonctionne un encaissement MTN Mobile Money",
        intro: "L'adaptateur est testé avec des réponses simulées du fournisseur. La vérification en bac à sable ne comptabilise jamais de FCFA. L'usage réel exige identifiants et acceptation de MTN.",
        items: [
          { title: "Préparer l'intention", body: "La vente et le montant exact sont figés. L'intention est enregistrée comme en file d'attente avant tout envoi au fournisseur." },
          { title: "Envoyer la demande de paiement", body: "L'état est enregistré comme inconnu avant l'envoi. Seules des adresses HTTPS approuvées sont utilisées et les redirections sont désactivées." },
          { title: "Interroger le statut", body: "Si la réponse est ambiguë, la même référence est interrogée. Un nouveau prélèvement n'est jamais soumis pour lever le doute." },
          { title: "Vérifier", body: "Un paiement n'est vérifié que lorsque le fournisseur renvoie une référence de transaction financière correspondant au montant, à la devise et au payeur." },
          { title: "Rapprocher", body: "La vérité du fournisseur est enregistrée d'abord, puis affectée à la créance. Si la période est clôturée ou l'affectation modifiée, le paiement passe en revue et le client n'est pas débité à nouveau." },
        ],
      },
      {
        kind: "cards", id: "providers", title: "État par fournisseur",
        items: [
          { icon: "phone", status: "partial", title: "MTN Mobile Money", body: "Le transport (jeton, demande de paiement, statut) existe, avec intentions durables et rapprochement. Les identifiants réels et l'acceptation de MTN restent nécessaires." },
          { icon: "phone", status: "planned", title: "Orange Money", body: "Non intégré. En attente d'une intégration contractualisée, et indiqué comme non pris en charge partout dans le produit." },
          { icon: "card", status: "planned", title: "Cartes et autres portefeuilles", body: "Nécessiteraient un contrat fournisseur, une revue de sécurité et un rapprochement des règlements. Les données de carte ne seraient pas stockées par QuincaiSoft sans périmètre PCI approuvé." },
        ],
      },
      {
        kind: "text", id: "custody", alt: true, title: "Aucune garde des fonds",
        paras: [
          "Les paiements de vos clients vous appartiennent, à vous et à votre prestataire agréé. Opesware Technologies, qui développe QuincaiSoft, n'est pas dépositaire des espèces ni des règlements des commerçants, et l'abonnement logiciel est distinct de vos ventes.",
          "Le rapport des intentions de paiement liste les demandes et leur statut. Il n'additionne pas les montants, car une demande n'est pas un encaissement et les environnements et devises diffèrent.",
        ],
      },
      {
        kind: "table", id: "reconcile", title: "Rapprochement : trois vues",
        caption: "Vues de rapprochement et leur état",
        head: ["Vue", "Ce qu'elle contient", "État"],
        rows: [
          ["Registre interne des encaissements", "Encaissements confirmés dans QuincaiSoft : espèces, règlements, acomptes, paiements vérifiés du fournisseur.", "Disponible"],
          ["Relevés du fournisseur", "Transactions du fournisseur et lots de règlement.", "Prévu (import des règlements)"],
          ["Relevé bancaire", "Le relevé bancaire du commerçant, lorsqu'il est disponible.", "Prévu"],
        ],
      },
      {
        kind: "limits", id: "limits", title: "Limites, en toute transparence",
        items: [
          "Aucun encaissement MTN réel n'est activé tant que identifiants et acceptation du fournisseur ne sont pas en place.",
          "Le routage public des rappels (callbacks) n'est pas activé. Un indice de rappel peut seulement signaler un paiement à rapprocher. Il ne règle jamais d'argent.",
          "Les remboursements via le fournisseur sont prévus. Un remboursement ne sera pas marqué remboursé avant confirmation du fournisseur.",
          "Aucune certification, approbation fiscale ou approbation de fournisseur n'est revendiquée.",
        ],
      },
      {
        kind: "faq", id: "faq", alt: true, title: "Questions sur les paiements",
        items: [
          { q: "Les clients peuvent-ils payer par MTN Mobile Money ?", a: "Pas encore en usage réel. L'adaptateur existe et est testé avec des réponses simulées, mais les identifiants réels et l'acceptation de MTN sont encore requis." },
          { q: "QuincaiSoft prend-il en charge Orange Money ?", a: "Non. Orange Money n'est pas intégré et attend une intégration contractualisée." },
          { q: "Opesware détient-il l'argent de mes ventes ?", a: "Non. Les paiements appartiennent au commerçant et à son prestataire agréé. Opesware Technologies n'est pas dépositaire des espèces ni des règlements des commerçants." },
          { q: "Puis-je vendre à crédit et prendre des acomptes ?", a: "Oui. Les ventes à crédit, encaissements, acomptes, affectations et remboursements d'acomptes approuvés font partie de la tranche finance livrée." },
        ],
      },
    ],
    related: ["features", "security", "resources/mobile-money-reconciliation-basics", "resources/manage-customer-credit-hardware-shop", "pricing"],
    ...CTA,
  },

  "online-store": {
    icon: "store",
    kicker: "Boutique en ligne",
    metaTitle: "Boutique en ligne : publiez votre catalogue | QuincaiSoft",
    description: "La boutique en ligne optionnelle de QuincaiSoft publie un instantané choisi du catalogue sur des pages publiques. Paiement en ligne, panier et comptes clients ne sont pas encore disponibles.",
    h1: "Boutique en ligne : publiez votre catalogue, le paiement viendra plus tard",
    answer: "La boutique en ligne QuincaiSoft est un module optionnel. Un commerçant peut publier un instantané choisi du catalogue sur une page de boutique publique et des pages d'articles qui n'affichent que les champs publics approuvés. Il n'y a pas encore de panier, de paiement en ligne ni de compte client, et la boutique n'affiche aucun stock en temps réel.",
    glance: {
      title: "En un coup d'œil",
      items: [
        { label: "Pages de catalogue publiques", status: "available", note: "Une page de boutique et des pages d'articles, champs publics approuvés uniquement." },
        { label: "Contrôle de publication", status: "available", note: "Le propriétaire publie un instantané explicite du catalogue." },
        { label: "Panier, paiement", status: "planned", note: "Spécifiés dans l'architecture. Pas encore construits." },
        { label: "Domaine personnalisé", status: "planned", note: "Vérification de domaine et certificats prévus." },
      ],
    },
    blocks: [
      {
        kind: "cards", id: "available", title: "Ce qui existe aujourd'hui",
        items: [
          { icon: "store", status: "available", title: "Pages de boutique et d'articles publiques", body: "Une boutique est accessible par son nom et liste le catalogue publié. Chaque article a sa propre page. Les boutiques absentes et les pannes affichent des états clairs." },
          { icon: "list", status: "available", title: "Publication contrôlée par le propriétaire", body: "Les paramètres de la boutique sont modifiés par un propriétaire autorisé, qui publie un instantané explicite du catalogue. Seuls les champs publics approuvés sont exposés." },
          { icon: "lock", status: "available", title: "Une application distincte à faibles privilèges", body: "La boutique n'a ni jeton de connexion marchand, ni en-tête de tenant, ni connexion à la base de données. Elle lit uniquement l'API publique du catalogue." },
          { icon: "key", status: "available", title: "Optionnelle par droit d'usage", body: "La boutique est un droit commerce distinct, provisionné par un processus de déploiement privilégié. Les requêtes en exécution ne peuvent ni l'accorder ni la facturer." },
        ],
      },
      {
        kind: "steps", id: "roadmap", alt: true, title: "Prévu : du parcours d'achat à la commande livrée",
        intro: "Tout ce qui suit est spécifié dans l'architecture et n'est pas encore construit.",
        items: [
          { title: "Navigation et panier", body: "Navigation anonyme, achat invité optionnel selon la politique du commerçant, totaux calculés côté serveur pour qu'un prix ne puisse pas être modifié dans le navigateur." },
          { title: "Réservation de stock", body: "Une réservation avec expiration bloque le stock pendant le paiement. Les réservations inutilisées sont libérées automatiquement." },
          { title: "Paiement via un prestataire agréé", body: "Les rappels du fournisseur sont vérifiés indépendamment. Les données de carte n'arrivent pas sur les serveurs QuincaiSoft." },
          { title: "Exécution", body: "Retrait ou livraison, frais de livraison et notifications client, avec stock comptabilisé à la remise." },
          { title: "Retours et remboursements", body: "Traités par les mêmes services de retours et de finance que les ventes en magasin." },
        ],
      },
      {
        kind: "cards", id: "control", title: "Conçue pour le contrôle du commerçant",
        items: [
          { icon: "check", status: "planned", title: "Aperçu et approbation", body: "Le commerçant prévisualise et approuve la visibilité publique des produits avant toute publication." },
          { icon: "globe", status: "planned", title: "Visibilité en français et en anglais", body: "Champs SEO du commerçant, URL de produits, alternates hreflang, données structurées et plans de site sont spécifiés pour les pages publiques." },
          { icon: "file", status: "planned", title: "Catalogue pour demandes de devis", body: "Un commerçant peut afficher un catalogue et recevoir des demandes ou des devis sans accepter de paiement en ligne." },
        ],
      },
      {
        kind: "limits", id: "limits", alt: true, title: "Limites, en toute transparence",
        items: [
          "Il n'y a pas encore de panier, de paiement en ligne, de compte client ni de gestion des commandes.",
          "La boutique n'affiche pas les quantités de stock en temps réel.",
          "Domaines personnalisés, automatisation des certificats et zones de livraison sont prévus, pas livrés.",
          "Aucun paiement n'est réglé via Opesware Technologies. Les paiements en ligne utiliseront des prestataires agréés.",
        ],
      },
      {
        kind: "faq", id: "faq", title: "Questions sur la boutique en ligne",
        items: [
          { q: "Les clients peuvent-ils payer en ligne aujourd'hui ?", a: "Non. La boutique publie votre catalogue. Le paiement en ligne est prévu et n'est pas disponible." },
          { q: "Affiche-t-elle mes niveaux de stock au public ?", a: "Non. Les pages publiques n'affichent que les champs publics approuvés, sans stock en temps réel." },
          { q: "La boutique en ligne est-elle incluse dans toutes les formules ?", a: "Non. C'est un droit commerce optionnel. Contactez-nous pour voir si elle convient à votre activité." },
        ],
      },
    ],
    related: ["features", "payments", "security", "pricing"],
    ...CTA,
  },

  security: {
    icon: "shield",
    kicker: "Sécurité",
    metaTitle: "Sécurité, piste d'audit et isolation des données | QuincaiSoft",
    description: "Comment QuincaiSoft sépare les clients avec la sécurité par ligne de PostgreSQL, tient une piste d'audit, évite les doubles écritures et contrôle les accès. Aucune certification n'est revendiquée.",
    h1: "Sécurité : isolation des clients, piste d'audit et accès contrôlés",
    answer: "QuincaiSoft sépare les données de chaque entreprise dans PostgreSQL grâce à la sécurité au niveau des lignes, tient une piste d'audit en ajout seul, protège chaque commande contre le double enregistrement par idempotence, applique rôles et permissions côté serveur, et connecte les utilisateurs via un fournisseur d'identité OpenID Connect. Nous ne revendiquons ni ISO 27001, ni SOC 2, ni PCI DSS, ni aucune autre certification.",
    glance: {
      title: "En un coup d'œil",
      items: [
        { label: "Isolation des clients", status: "available", note: "Sécurité par ligne forcée et rôle de base de données non propriétaire." },
        { label: "Piste d'audit", status: "partial", note: "En ajout seul. Pas encore branchée sur chaque commande." },
        { label: "Idempotence", status: "available", note: "Une commande répétée renvoie le résultat d'origine." },
        { label: "Certifications", status: "planned", note: "Aucune revendiquée aujourd'hui." },
      ],
    },
    blocks: [
      {
        kind: "cards", id: "controls", title: "Contrôles présents dans le produit aujourd'hui",
        items: [
          { icon: "lock", status: "available", title: "Isolation par sécurité au niveau des lignes", body: "Chaque table propre à un client porte un identifiant de tenant et une sécurité par ligne forcée. Le rôle de base de données d'exécution n'est pas propriétaire des tables et ne peut pas contourner les politiques ; le rôle de migration est distinct. Une requête sans contexte de tenant vérifié échoue de façon sûre." },
          { icon: "scroll", status: "partial", title: "Piste d'audit en ajout seul", body: "Les entrées d'audit ne peuvent être ni modifiées ni supprimées. Les faits comptabilisés sont corrigés par une contre-passation liée à l'original, jamais écrasés. L'audit couvre aujourd'hui les changements d'identité et les événements projetés, pas encore chaque commande." },
          { icon: "fingerprint", status: "available", title: "Commandes idempotentes", body: "Chaque commande porte un identifiant d'opération stable et une empreinte de requête. La répéter renvoie le résultat d'origine au lieu de comptabiliser deux fois, et réutiliser un identifiant avec un contenu différent est rejeté." },
          { icon: "users", status: "available", title: "Rôles, permissions et approbations", body: "Les permissions sont revérifiées côté serveur même si un bouton est masqué. Les changements sensibles exigent un approbateur indépendant. Modifier un rôle incrémente une époque d'autorisation, et les anciens jetons cessent de fonctionner." },
          { icon: "key", status: "available", title: "Connexion OpenID Connect", body: "Authorization Code avec PKCE. Les jetons web restent côté serveur, et Redis ne conserve que des sessions temporaires, pas des soldes. Récupération de mot de passe, MFA et déconnexion globale relèvent du fournisseur d'identité que vous configurez. Il n'existe aucun contournement par mot de passe de démonstration en production." },
          { icon: "database", status: "available", title: "Les données métier restent dans PostgreSQL", body: "Journaux comptables, stock, documents et audit résident dans PostgreSQL 18. Les rapports lisent via des vues à droits de l'appelant, donc la sécurité par ligne s'y applique aussi." },
          { icon: "phone", status: "available", title: "Stockage chiffré sur le téléphone", body: "Les données mobiles sont conservées dans une base locale chiffrée dont la clé est dans le magasin sécurisé du système, et échouent de façon sûre si le chiffrement est indisponible." },
          { icon: "building", status: "available", title: "Opérations de plateforme séparées", body: "L'administration de la plateforme est une application distincte avec sa propre connexion, son émetteur, son audience et son cookie. Les opérateurs sont enrôlés par un processus de déploiement privilégié." },
        ],
      },
      {
        kind: "steps", id: "flow", alt: true, title: "Comment une requête est protégée",
        items: [
          { title: "Authentifier", body: "Le jeton est validé : signature, émetteur et audience." },
          { title: "Résoudre l'appartenance", body: "Une appartenance active en base décide du tenant, de la société et de la succursale. Un en-tête venant du navigateur n'est jamais cru pour cela." },
          { title: "Vérifier la permission", body: "Le service vérifie lui-même la permission, quoi qu'ait affiché l'écran." },
          { title: "Ouvrir une transaction cadrée", body: "Le tenant vérifié est positionné dans la transaction. La sécurité par ligne limite alors chaque requête à ce tenant." },
          { title: "Enregistrer une fois, de façon atomique", body: "Les effets stock, argent et documents de la commande sont validés ensemble avec son enregistrement d'idempotence et ses preuves d'audit ou d'événement." },
        ],
      },
      {
        kind: "text", id: "data", title: "Où vivent vos données",
        paras: [
          "Les données métier sont stockées dans PostgreSQL. La région d'hébergement, les sauvegardes et les engagements de reprise dépendent du mode de déploiement et du contrat, et ne sont pas publiés ici comme des niveaux de service. Dans un déploiement hébergé par le client, les données restent sur une infrastructure que le client contrôle.",
          "Vous pouvez aujourd'hui exporter les rapports en CSV, page par page. Un export complet de vos enregistrements dans un format lisible par machine est prévu.",
        ],
      },
      {
        kind: "limits", id: "limits", alt: true, title: "Ce que nous ne revendiquons pas",
        items: [
          "Aucune approbation ISO 27001, SOC 2, PCI DSS, OHADA ou DGI n'est revendiquée.",
          "Aucun rapport de test d'intrusion indépendant n'est publié.",
          "Réussir les tests d'intégration sur PostgreSQL 18 prouve un comportement. Ce n'est pas un audit de sécurité.",
          "L'attestation matérielle des clés d'appareils mobiles n'est pas établie.",
          "La MFA dépend du fournisseur d'identité que vous configurez.",
        ],
      },
      {
        kind: "faq", id: "faq", title: "Questions sur la sécurité",
        items: [
          { q: "Une entreprise peut-elle voir les données d'une autre ?", a: "La conception l'interdit. La sécurité par ligne est forcée sur les tables des clients, le rôle d'exécution ne peut pas la contourner, et un contexte de tenant absent échoue de façon sûre. L'isolation est testée sur une vraie base PostgreSQL 18." },
          { q: "Une vente comptabilisée peut-elle être supprimée ?", a: "Non. Les faits comptabilisés sont corrigés par contre-passation ou ajustement liés à l'original." },
          { q: "Qui gère les mots de passe et la MFA ?", a: "Votre fournisseur d'identité. QuincaiSoft ne stocke pas de mots de passe et n'a aucun contournement par mot de passe de démonstration en production." },
          { q: "QuincaiSoft est-il certifié ?", a: "Aucune certification n'est revendiquée. Nous décrivons les contrôles et les preuves qui les appuient." },
        ],
      },
    ],
    related: ["features", "mobile-app", "payments", "desktop", "faq"],
    ...CTA,
  },

  desktop: {
    icon: "monitor",
    kicker: "Logiciel Windows",
    metaTitle: "Logiciel Windows et adaptateur de synchronisation optionnel | QuincaiSoft",
    description: "Votre logiciel Windows existant continue de fonctionner seul. QuincaiSoft inclut un adaptateur de synchronisation optionnel dont l'intégration à l'application Windows d'origine reste en attente.",
    h1: "Logiciel Windows : votre logiciel existant reste indépendant",
    answer: "Si vous utilisez déjà l'application Windows existante, elle continue de fonctionner seule avec une base SQLite locale et sans dépendance au cloud. QuincaiSoft inclut un adaptateur de synchronisation .NET 10 optionnel, qui pourrait relier un site Windows au cloud après consentement explicite, mais il n'est pas encore intégré à l'application Windows d'origine et aucune connexion au cloud n'est proposée aujourd'hui.",
    glance: {
      title: "En un coup d'œil",
      items: [
        { label: "Windows autonome", status: "available", note: "Inchangé. Pas de compte cloud, pas de réseau au démarrage." },
        { label: "Bibliothèque de synchronisation", status: "partial", note: "Construite et testée. Intégration à l'hôte d'origine en attente." },
        { label: "Mode connecté", status: "planned", note: "Seulement après intégration et bascule explicites." },
        { label: "Compagnon téléphone pour Windows autonome", status: "planned", note: "Instantanés chiffrés et brouillons, sans écriture en direct." },
      ],
    },
    blocks: [
      {
        kind: "cards", id: "modes", title: "Deux modes pris en charge",
        items: [
          { icon: "monitor", status: "available", title: "Autonome", body: "L'application Windows et sa base SQLite restent l'unique autorité métier et fonctionnent entièrement hors ligne. Aucun compte cloud n'est nécessaire et rien ne se synchronise automatiquement. Les clients autonomes existants peuvent choisir de ne jamais se connecter." },
          { icon: "cloud", status: "planned", title: "Connecté", body: "Après intégration et bascule explicites, un poste Windows devient un site enregistré avec sa propre allocation hors ligne sous un tenant cloud. Le cloud reçoit les opérations acquittées et fournit des vues consolidées. Une même vente n'est représentée qu'une fois globalement." },
        ],
      },
      {
        kind: "cards", id: "adapter", alt: true, title: "Ce que fait l'adaptateur aujourd'hui",
        items: [
          { icon: "database", status: "available", title: "Boîte d'envoi locale résistante aux pannes", body: "La boîte d'envoi partage la transaction locale d'enregistrement : une panne entre la validation locale et l'envoi réseau ne perd rien." },
          { icon: "key", status: "available", title: "Clés d'appareil protégées", body: "Les clés d'appareil sont protégées par Windows DPAPI et les signatures correspondent à la preuve d'appareil du cloud." },
          { icon: "lock", status: "available", title: "Consentement requis, pas de réseau au démarrage", body: "La connexion exige un consentement explicite. En mode autonome, l'adaptateur n'effectue aucun appel réseau au démarrage." },
          { icon: "route", status: "available", title: "Reprise avec des identifiants identiques", body: "Les résultats incertains sont renvoyés avec les mêmes identifiants. Les reçus de revue correspondants arrêtent le renvoi automatique tout en conservant les faits d'origine." },
        ],
      },
      {
        kind: "steps", id: "cutover", title: "Protocole de bascule prévu",
        intro: "Résumé du protocole de l'architecture pour connecter un site Windows. Il est prévu et pas encore proposé.",
        items: [
          { title: "Consentement et preuve de propriété", body: "Le propriétaire choisit le tenant et la succursale, prouve sa propriété et accepte les conditions sur les données." },
          { title: "Contrôles natifs et sauvegarde vérifiée", body: "Le poste Windows passe ses propres contrôles d'acceptation et une sauvegarde chiffrée, testée en restauration, est réalisée." },
          { title: "Export à blanc", body: "Clients, fournisseurs, produits et unités, stock, soldes d'ouverture et références de documents sont exportés et contrôlés pour détecter les collisions." },
          { title: "Table de correspondance et rapprochement", body: "Chaque identifiant local est mis en correspondance, et stock d'ouverture, crédits, acomptes, dettes et caisse sont rapprochés avant toute acceptation." },
          { title: "Une seule méthode d'ouverture", body: "Choisir l'import historique ou la bascule sur soldes courants, jamais les deux, pour ne pas compter deux fois stock et chiffre d'affaires." },
          { title: "Activation avec retour arrière possible", body: "Un quota d'appareil est délivré, une vente de test est synchronisée et vérifiée, et au moins un cycle de clôture est suivi avant un déploiement sans assistance." },
        ],
      },
      {
        kind: "limits", id: "limits", alt: true, title: "Limites, en toute transparence",
        items: [
          "L'adaptateur n'a pas été injecté dans le chemin d'enregistrement de l'application Windows d'origine.",
          "La base Windows récupérée n'a pas été compilée nativement ni certifiée ici, et présente des constats ouverts d'audit des dépendances.",
          "Les contrôles automatiques de l'adaptateur ont été exécutés sous Linux. Ce n'est pas une certification native Windows.",
          "Aucune synchronisation cloud pour Windows n'est proposée aujourd'hui.",
          "Dans le logiciel Windows existant, les saisies mobile money sont des références confirmées manuellement, pas une vérification par le fournisseur.",
        ],
      },
      {
        kind: "faq", id: "faq", title: "Questions sur Windows",
        items: [
          { q: "Dois-je faire passer ma boutique Windows au cloud ?", a: "Non. Le mode autonome reste pleinement pris en charge et n'exige aucun compte cloud. La connexion est optionnelle et réversible." },
          { q: "Mon logiciel Windows s'arrêtera-t-il sans internet ?", a: "Non. Il fonctionne seul avec une base locale, indépendamment de la disponibilité du cloud." },
          { q: "Puis-je essayer la synchronisation aujourd'hui ?", a: "Non. L'adaptateur existe comme bibliothèque testée, mais son intégration à l'application Windows d'origine est en attente." },
        ],
      },
    ],
    related: ["migration-and-onboarding", "mobile-app", "security", "pricing"],
    ...CTA,
  },

  "migration-and-onboarding": {
    icon: "route",
    kicker: "Migration et mise en route",
    metaTitle: "Migration et mise en route : comment une boutique s'installe | QuincaiSoft",
    description: "Le parcours prévu pour installer une boutique sur QuincaiSoft : paramétrage, import des données d'ouverture avec essai à blanc, rapprochement, formation, pilote et mise en service.",
    h1: "Migration et mise en route : comment une boutique s'installe",
    answer: "L'arrivée d'une boutique sur QuincaiSoft est prévue comme un processus encadré et réversible : paramétrer l'entreprise, reprendre les données d'ouverture avec un essai à blanc, rapprocher stock et soldes, former l'équipe, mener un pilote puis passer en production. Les étapes ci-dessous suivent l'architecture. Le provisionnement automatisé et les outils d'import ne sont pas terminés : aujourd'hui, la mise en route s'organise directement avec Opesware Technologies.",
    glance: {
      title: "En un coup d'œil",
      items: [
        { label: "Mise en route guidée", status: "planned", note: "Paramétrage, données d'ouverture et contrôles, pas à pas." },
        { label: "Import avec essai à blanc", status: "planned", note: "Les totaux s'affichent avant toute comptabilisation." },
        { label: "Pilote d'au moins 10 jours d'activité", status: "planned", note: "Avant un déploiement plus large." },
        { label: "Aujourd'hui", status: "partial", note: "Organisée avec notre équipe. Pas encore d'import automatisé." },
      ],
    },
    blocks: [
      {
        kind: "steps", id: "path", title: "Le parcours de mise en route",
        intro: "Toutes les étapes sont prévues. Chacune se termine par un contrôle, et une étape en échec reprend au lieu de laisser un compte à moitié créé.",
        items: [
          { title: "Vérifier le propriétaire et la société", body: "Confirmer le contact du propriétaire et recueillir le profil de la société et son identité légale." },
          { title: "Créer la configuration de travail", body: "Succursale par défaut, emplacement de stock, liste de prix, numérotation des documents, rôles et droits de la formule. Les paramètres fiscaux restent provisoires jusqu'à confirmation par votre comptable." },
          { title: "Choisir comment reprendre les données", body: "Démarrer sans données, importer un CSV, ou recourir à une migration supervisée." },
          { title: "Essai à blanc", body: "Voir les totaux et la date d'activité avant toute comptabilisation." },
          { title: "Comptabiliser les soldes d'ouverture", body: "Stock, créances et dettes d'ouverture sont comptabilisés avec des références d'ouverture auditées. Le stock historique ne crée jamais de ventes en double." },
          { title: "Configurer le comptoir", body: "FCFA, langue des reçus, politique de caisse, et test d'imprimante lorsque c'est possible." },
          { title: "Confirmer", body: "La mise en route n'est confirmée qu'après réussite des contrôles d'isolation des clients, de permissions, de numérotation des documents et de données." },
        ],
      },
      {
        kind: "cards", id: "data", alt: true, title: "Ce qui peut être repris",
        items: [
          { icon: "package", status: "planned", title: "Produits, unités et catégories", body: "Votre catalogue avec ses unités de mesure, pour que le stock garde le même sens." },
          { icon: "boxes", status: "planned", title: "Instantané du stock", body: "Quantités d'ouverture par entrepôt, rapprochées d'un comptage." },
          { icon: "users", status: "planned", title: "Clients et fournisseurs", body: "Données de base, avec collisions affichées avant l'import." },
          { icon: "wallet", status: "planned", title: "Crédits, acomptes et dettes d'ouverture", body: "Soldes dus à vous et par vous, rapprochés avant d'être acceptés." },
          { icon: "file", status: "planned", title: "Références de documents existants", body: "Les numéros d'origine sont conservés comme numéro d'origine et jamais réécrits." },
        ],
      },
      {
        kind: "steps", id: "pilot", title: "Pilote et mise en service",
        items: [
          { title: "Former l'équipe", body: "Une formation documentée des utilisateurs est une condition pour terminer un pilote." },
          { title: "Piloter pendant au moins 10 jours d'activité", body: "Relever le stock et les soldes de départ et de clôture, les caisses quotidiennes, les ventes par moyen de paiement, les retours, les acomptes et les achats." },
          { title: "Critères de sortie", body: "Aucun défaut critique non résolu en finance ou isolation, aucun écart de rapprochement inexpliqué, une restauration de sauvegarde réussie et un support stable." },
        ],
      },
      {
        kind: "limits", id: "limits", alt: true, title: "Limites, en toute transparence",
        items: [
          "Les outils d'import et d'export opérationnels et le cycle complet de provisionnement ne sont pas terminés.",
          "Nous ne publions ici ni calendrier ni tarif. Contactez-nous pour les deux.",
          "Un export de vos données dans un format lisible par machine, à votre départ, est prévu et pas encore disponible.",
          "La connexion d'un site Windows existant suit son propre protocole et n'est pas encore proposée. Voir la page Logiciel Windows.",
        ],
      },
      {
        kind: "faq", id: "faq", title: "Questions sur la mise en route",
        items: [
          { q: "Puis-je importer mes produits et mes clients ?", a: "Un import avec essai à blanc est prévu. Aujourd'hui, la mise en route est organisée avec notre équipe et l'import automatisé n'est pas encore disponible." },
          { q: "Mes anciens numéros de documents seront-ils conservés ?", a: "Oui, c'est la conception retenue. Les numéros d'origine sont conservés comme numéro d'origine et ne sont pas réécrits." },
          { q: "Combien de temps dure une migration ?", a: "Nous ne publions pas de durée. Elle dépend de vos données et de vos succursales. Un pilote dure au moins 10 jours d'activité." },
        ],
      },
    ],
    related: ["desktop", "features", "pricing", "resources/buy-and-receive-stock-from-suppliers"],
    ...CTA,
  },

  compare: {
    icon: "scale",
    kicker: "Guide d'achat",
    metaTitle: "Choisir un logiciel de commerce en zone CEMAC : liste de contrôle neutre",
    description: "Une liste de contrôle neutre pour choisir un logiciel de commerce, de stock et de caisse au Cameroun et en zone CEMAC, avec la réponse actuelle de QuincaiSoft à chaque point.",
    h1: "Choisir un logiciel de commerce en zone CEMAC : liste de contrôle neutre",
    answer: "Choisissez un logiciel de commerce et de stock en le testant sur votre travail quotidien : unités et conversions, crédit, contrôle de caisse, traitement honnête du mobile money, comportement sans internet, propriété des données et support. Utilisez cette liste avec n'importe quel éditeur, nous compris, et demandez des preuves plutôt que des promesses. Nous ne citons aucun concurrent et indiquons où en est QuincaiSoft aujourd'hui, lacunes comprises.",
    blocks: [
      {
        kind: "cards", id: "checklist", title: "La liste de contrôle",
        intro: "Demandez à chaque éditeur de montrer ces points sur vos propres produits, pas sur un catalogue de démonstration.",
        items: [
          { icon: "ruler", title: "Unités et conversions", body: "Pouvez-vous vendre le même produit au sac, à la pièce, au mètre ou à la tonne avec un stock cohérent ?", points: ["Rejette-t-il les conversions qui ne tombent pas juste ?", "Pouvez-vous découper ou reconditionner du stock en gardant un solde correct ?"] },
          { icon: "wallet", title: "Crédit et acomptes", body: "Voyez-vous ce que chaque client doit et a payé d'avance ?", points: ["Les acomptes sont-ils séparés des ventes ?", "Un encaissement peut-il être appliqué deux fois par erreur ?"] },
          { icon: "banknote", title: "Contrôle de caisse", body: "Pouvez-vous rapprocher la caisse chaque jour sans modifier l'historique ?", points: ["Les espèces attendues sont-elles calculées, pas saisies ?", "Les corrections se font-elles par de nouvelles écritures ?"] },
          { icon: "card", title: "Honnêteté sur le mobile money", body: "Indique-t-il si un paiement est vérifié ou simplement une référence saisie ?", points: ["Quels fournisseurs sont réellement intégrés ?", "Qui détient l'argent ?"] },
          { icon: "walloff", title: "Connexion faible", body: "Que se passe-t-il au comptoir quand internet tombe ?", points: ["Que peut-on vendre hors ligne, et avec quelles limites ?", "Comment évite-t-on les doublons à la reconnexion ?"] },
          { icon: "shield", title: "Données et sécurité", body: "Vos données sont-elles séparées de celles des autres clients, et exportables ?", points: ["Qui peut voir quelle succursale ?", "Existe-t-il une piste d'audit ?"] },
          { icon: "landmark", title: "Fiscal et juridique", body: "Que prétend l'éditeur sur la fiscalité et l'agrément fiscal ?", points: ["Un agrément est-il revendiqué avec une référence ?", "Qui confirme les règles avec votre comptable ?"] },
          { icon: "handshake", title: "Support et coût total", body: "Qui répond, dans quelle langue, et qu'est-ce qui n'est pas inclus ?", points: ["Matériel, hébergement, formation, mises à jour ?", "Que deviennent vos données si vous partez ?"] },
        ],
      },
      {
        kind: "table", id: "ours", alt: true, title: "Comment QuincaiSoft répond aujourd'hui",
        intro: "L'état suit les propres registres de migration du projet.",
        caption: "QuincaiSoft face à la liste de contrôle",
        head: ["Point de la liste", "QuincaiSoft aujourd'hui", "État"],
        rows: [
          ["Unités et conversions", "Une unité de base par produit, quantités exactes, découpe et reconditionnement sans perte. Les unités de conditionnement alternatives avec conversion exacte sont prévues.", "Partiellement disponible"],
          ["Crédit et acomptes", "Créances, encaissements, acomptes, affectation et remboursements d'acomptes approuvés.", "Disponible"],
          ["Contrôle de caisse", "Espèces attendues calculées, clôture comptée, écarts examinés, approbations indépendantes.", "Disponible"],
          ["Honnêteté sur le mobile money", "Adaptateur MTN construit, usage réel en attente. Orange Money non intégré. Les références manuelles ne sont jamais dites vérifiées.", "Partiellement disponible"],
          ["Connexion faible", "Brouillons de vente hors ligne chiffrés. Ventes terminées hors ligne désactivées pour l'instant.", "Partiellement disponible"],
          ["Données et sécurité", "Isolation par ligne, audit en ajout seul, idempotence. Aucune certification revendiquée.", "Partiellement disponible"],
          ["Fiscal et juridique", "Paramétrable. Aucun agrément DGI ni certification n'est revendiqué.", "Non revendiqué"],
          ["Support et coût", "Contactez-nous pour les tarifs. La mise en route est organisée avec notre équipe.", "Prévu"],
        ],
      },
      {
        kind: "steps", id: "trial", title: "Faire un essai équitable en une semaine",
        items: [
          { title: "Choisir vos produits les plus difficiles", body: "Prenez des articles vendus en plusieurs unités, coupés sur mesure ou reconditionnés." },
          { title: "Rejouer une vraie journée", body: "Saisissez une journée de ventes, de crédit, un acompte, un retour et une livraison fournisseur." },
          { title: "Clôturer la caisse", body: "Vérifiez que les espèces attendues et l'écart sont calculés, pas saisis." },
          { title: "Tester le mauvais jour", body: "Coupez internet et répétez un envoi. Cherchez les doublons et les libellés honnêtes." },
          { title: "Demander les limites par écrit", body: "Demandez ce qui n'est pas encore construit et ce qui n'est pas certifié." },
        ],
      },
      {
        kind: "faq", id: "faq", alt: true, title: "Questions d'achat",
        items: [
          { q: "Dois-je choisir l'option la moins chère ?", a: "Comparez le coût total : licences ou abonnement, matériel, hébergement, formation, mises à jour, support et coût de sortie. Un prix bas peut cacher ces éléments." },
          { q: "Comment vérifier les affirmations d'un éditeur ?", a: "Demandez à voir chaque affirmation fonctionner sur vos propres données et demandez les limites par écrit. Méfiez-vous des certifications ou agréments fiscaux non justifiés." },
        ],
      },
    ],
    related: ["features", "pricing", "faq", "resources"],
    ctaTitle: "Voulez-vous parcourir cette liste avec nous ?",
    ctaBody: "Nous répondrons à chaque point pour votre activité, y compris ce qui n'est pas encore construit.",
  },
};

export const resourcesHub = {
  metaTitle: "Ressources et guides pratiques pour quincailleries et commerces | QuincaiSoft",
  description: "Guides pratiques et neutres pour commerces et distributeurs au Cameroun et en zone CEMAC : matériaux en vrac en FCFA, crédit client, réception fournisseurs, devis, succursales et mobile money.",
  kicker: "Ressources",
  h1: "Guides pratiques pour quincailleries, matériaux de construction et commerce de gros",
  answer: "Ces guides expliquent comment mener le travail quotidien d'un commerce ou d'un distributeur au Cameroun et en zone CEMAC : suivre les matériaux en vrac en FCFA, gérer le crédit, réceptionner le stock fournisseur, établir des devis pour entrepreneurs, piloter plusieurs succursales et rapprocher le mobile money. Ils sont utiles avec n'importe quel système, et indiquent où QuincaiSoft aide.",
  listTitle: "Six guides",
  related: ["features", "industries", "compare", "faq"],
};

export const titles: Record<string, string> = {
  "mobile-app": "Application mobile",
  payments: "Paiements et mobile money",
  "online-store": "Boutique en ligne",
  security: "Sécurité et données",
  desktop: "Logiciel Windows",
  "migration-and-onboarding": "Migration et mise en route",
  compare: "Choisir un logiciel de commerce en zone CEMAC",
  resources: "Ressources et guides",
  "resources/track-cement-and-bulk-materials-fcfa": "Suivre le ciment et les matériaux en vrac en FCFA",
  "resources/manage-customer-credit-hardware-shop": "Gérer le crédit client dans une quincaillerie",
  "resources/buy-and-receive-stock-from-suppliers": "Acheter et réceptionner le stock des fournisseurs",
  "resources/quotations-for-contractors": "Devis pour les entrepreneurs",
  "resources/run-multiple-branches": "Gérer plusieurs succursales",
  "resources/mobile-money-reconciliation-basics": "Bases du rapprochement mobile money",
};
