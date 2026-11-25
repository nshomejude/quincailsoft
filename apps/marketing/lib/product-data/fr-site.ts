import type { FaqData, FeaturesData, IndustriesData, PricingData } from "../product-types";

const CTA = {
  ctaTitle: "Envie de voir si cela convient à votre activité ?",
  ctaBody: "Parlez-nous de votre quincaillerie ou de votre activité de distribution. Nous répondons par e-mail en français ou en anglais, en disant clairement ce qui est prêt et ce qui est en cours de construction.",
};

export const features: FeaturesData = {
  metaTitle: "Fonctionnalités, module par module | QuincaiSoft ERP et caisse",
  description: "Chaque module QuincaiSoft expliqué : ventes et caisse, stock et unités, achats, crédit client et acomptes, devis, retours, caisse, documents, rapports, succursales et audit, chacun avec un état honnête.",
  kicker: "Fonctionnalités",
  h1: "Fonctionnalités, module par module, avec un état honnête",
  answer: "QuincaiSoft est un ERP et point de vente pour les quincailleries, les vendeurs de matériaux de construction et les grossistes du Cameroun et de la zone CEMAC. Cette page explique ce que fait chaque module et comment il fonctionne, et indique pour chacun s'il est disponible dans la tranche de migration actuelle, partiellement disponible ou prévu. Nous construisons par étapes et ne prétendons pas offrir un produit complet ni certifié.",
  statusLegendTitle: "Comment lire les étiquettes d'état",
  statusLegend: [
    { status: "available", body: "Implémenté et testé dans la tranche de migration actuelle. Ce n'est pas une revendication de couverture complète du module ni de certification de production." },
    { status: "partial", body: "Certains parcours existent, d'autres restent à venir. La note sous chaque module précise lesquels." },
    { status: "planned", body: "Spécifié dans l'architecture et pas encore construit." },
    { status: "pending", body: "Construit mais désactivé ou en attente d'une étape externe, comme l'acceptation d'un fournisseur ou des preuves sur appareils." },
  ],
  summaryTitle: "Les modules en un coup d'œil",
  summaryCaption: "Modules QuincaiSoft et leur état actuel",
  summaryHead: ["Module", "État"],
  modules: [
    {
      id: "sales-pos", icon: "cart", title: "Ventes et point de vente", status: "available",
      what: "Vendre au comptoir ou sur commande assistée. Chaque vente est tarifée depuis le catalogue, liée à un client et à un entrepôt, et comptabilisée en une seule transaction avec ses effets de stock et comptables et sa facture numérotée.",
      how: [
        "Les lignes sont tarifées par le serveur depuis le catalogue, pas par le navigateur, et les quantités sont exactes.",
        "La comptabilisation exige un motif et un contrôle de version : deux personnes ne peuvent pas écraser l'une l'autre.",
        "La facture est émise dans la même transaction que les écritures de stock et de journal. Si les paramètres de facturation manquent, rien n'est comptabilisé.",
        "Envoyer deux fois la même commande renvoie le premier résultat au lieu de créer une seconde vente.",
      ],
      statusNote: "Disponible pour les ventes immédiates sans taxe. Les ventes avec taxe, les paiements fractionnés au comptoir et la facturation fiscale sont prévus.",
    },
    {
      id: "stock-units", icon: "boxes", title: "Stock, unités et transformations", status: "available",
      what: "Savoir ce que vous détenez dans chaque entrepôt, le transférer entre succursales, l'inventorier, et transformer un produit en un autre par découpe ou reconditionnement.",
      how: [
        "Chaque produit a une unité de base qui ne peut plus changer après sa création. Les quantités sont exactes, jamais arrondies en silence.",
        "Les transferts passent par brouillon, approbation indépendante, expédition en transit puis réception partielle ou finale. Le stock de destination ne change qu'à la réception.",
        "Les inventaires physiques prennent un instantané, saisissent l'observé, exigent une approbation indépendante et comptabilisent un gain ou une perte avec une écriture de journal.",
        "Découpe et reconditionnement sont des tâches approuvées. La découpe garde la longueur exacte en millimètres, et toute la valeur comptable passe au produit obtenu.",
      ],
      statusNote: "Disponible, avec découpe et reconditionnement sans perte seulement. Les unités de conditionnement alternatives (sac et tonne) avec conversion exacte, le suivi des chutes et déchets, les lots et numéros de série sont prévus.",
    },
    {
      id: "purchasing", icon: "truck", title: "Achats et fournisseurs", status: "available",
      what: "Commander auprès des fournisseurs, réceptionner les marchandises contre la commande et enregistrer la facture fournisseur, avec stock et dettes cohérents.",
      how: [
        "Un brouillon d'achat reprend des instantanés d'origine du fournisseur, du coût et de la quantité. Commander ne modifie pas le stock.",
        "Un approbateur indépendant valide la commande. Les réceptions peuvent être partielles et ne peuvent dépasser la quantité commandée.",
        "La réception augmente le stock et enregistre les marchandises reçues non encore facturées. La facture fournisseur d'une réception crée ensuite la dette.",
        "Le retour de marchandises non facturées à un fournisseur est limité à ce qui a réellement été reçu.",
      ],
      statusNote: "Disponible pour les commandes, réceptions, factures fournisseurs et retours non facturés. Demandes d'achat, frais d'approche, paiements fournisseurs et balance âgée sont prévus.",
    },
    {
      id: "customers-credit", icon: "wallet", title: "Clients, crédit et acomptes", status: "available",
      what: "Vendre à crédit, prendre des acomptes, encaisser et voir à tout moment ce que chaque client doit ou a payé d'avance.",
      how: [
        "Une vente à crédit crée une créance. Un encaissement s'applique à la dette reconnue, et un trop-perçu est rejeté.",
        "Un acompte est conservé comme dette envers le client et affecté à une vente sans enregistrer un second encaissement.",
        "Le remboursement en espèces d'un acompte inutilisé exige sa propre permission et ne peut dépasser le solde restant.",
        "Chacune de ces commandes est idempotente et écrit son écriture de journal dans la même transaction.",
      ],
      statusNote: "Disponible. Plafonds de crédit client, balance âgée et relevés sont prévus.",
    },
    {
      id: "quotations", icon: "file", title: "Devis", status: "available",
      what: "Préparer un devis pour un entrepreneur ou un client professionnel, l'émettre comme document numéroté et convertir le devis accepté en vente.",
      how: [
        "Un brouillon est tarifé depuis le catalogue et porte une date d'expiration.",
        "L'émission fige un instantané numéroté que des modifications ultérieures ne peuvent pas changer.",
        "La conversion crée un brouillon de vente lié. Elle est rejetée si le prix a changé, si le devis a expiré ou si une taxe s'applique, au lieu de modifier le montant devisé.",
      ],
      statusNote: "Disponible. L'acceptation partielle en plusieurs commandes, les budgets de projet et les sous-plafonds de crédit sont prévus.",
    },
    {
      id: "returns", icon: "undo", title: "Retours", status: "available",
      what: "Reprendre des marchandises contre une vente d'origine, les inspecter, et contre-passer correctement chiffre d'affaires et coût avec un avoir.",
      how: [
        "Un brouillon de retour renvoie à la vente d'origine et ne peut dépasser la quantité vendue moins les retours précédents.",
        "Les preuves d'inspection sont enregistrées, et une autre personne approuve avant la comptabilisation.",
        "La comptabilisation remet en stock les articles revendables, contre-passe chiffre d'affaires et coût, répartit la valeur entre créance et crédit client, et émet un avoir lié à la vente d'origine.",
      ],
      statusNote: "Disponible pour les retours clients d'articles revendables. Un retour ne paie pas à lui seul d'espèces ni de remboursement via un fournisseur. Échanges et garanties sont prévus.",
    },
    {
      id: "cash-expenses", icon: "banknote", title: "Caisse et dépenses", status: "available",
      what: "Ouvrir et clôturer les caisses, enregistrer les dépenses avec approbation, et clôturer les périodes comptables avec des contrôles.",
      how: [
        "Une session de caisse s'ouvre avec un fonds déclaré. Les espèces attendues viennent des enregistrements immuables de la session, et l'écart est le montant compté moins l'attendu.",
        "Les dépenses passent par brouillon, soumission et approbation ou rejet indépendants, puis sont payées depuis une session de caisse ouverte.",
        "Les périodes sont clôturées ou rouvertes par proposition et approbation indépendante. Les caisses ouvertes et les éléments non approuvés bloquent une clôture.",
      ],
      statusNote: "Disponible. Les paiements bancaires et autres canaux de paiement pour les dépenses ne sont pas construits.",
    },
    {
      id: "documents", icon: "filecheck", title: "Documents et impression", status: "partial",
      what: "Émettre des documents commerciaux numérotés qui restent identiques pour toujours, et les imprimer en français ou en anglais.",
      how: [
        "Chaque document est un instantané avec l'identité de l'émetteur, les lignes, les montants et une empreinte SHA-256. Des règles de base de données empêchent modification et suppression.",
        "Les numéros sont attribués par tenant, société, succursale, type, année et série, un à la fois.",
        "Les réimpressions portent la mention COPIE et ne comptabilisent jamais stock ou argent à nouveau. Les avoirs renvoient à la vente d'origine.",
      ],
      statusNote: "Partiellement disponible : vues d'impression A4 en français et en anglais et paramètres de l'émetteur. Imprimantes de tickets thermiques et autres types de documents sont prévus, et la compatibilité des imprimantes ou balances exige des tests sur appareils réels.",
    },
    {
      id: "reports", icon: "chart", title: "Rapports", status: "partial",
      what: "Consulter ventes, stock, créances, caisse et demandes mobile money sur une période, avec totaux et export CSV.",
      how: [
        "Cinq rapports existent : ventes comptabilisées, stock, créances, caisse et demandes de paiement au fournisseur.",
        "Les rapports datés acceptent une plage inclusive d'au plus 367 jours, et les totaux couvrent tout le filtre, pas seulement la page affichée.",
        "Les montants sont des valeurs exactes. L'export CSV couvre la page demandée, et les cellules ressemblant à des formules de tableur sont neutralisées.",
      ],
      statusNote: "Partiellement disponible : cinq rapports. Les autres rapports du plan, la balance âgée et les vues consolidées sont prévus.",
    },
    {
      id: "multi-branch", icon: "building", title: "Multi-succursales et permissions", status: "partial",
      what: "Travailler sur plusieurs sociétés et succursales, chaque personne ne voyant et ne faisant que ce que son rôle permet.",
      how: [
        "Chaque enregistrement porte son périmètre tenant, société et succursale. Le périmètre vient d'une appartenance vérifiée, jamais d'un en-tête du navigateur.",
        "Les changements de rôle et d'activation exigent une demande d'un propriétaire et l'approbation d'un autre. L'application incrémente une époque d'autorisation : les anciens jetons cessent de fonctionner.",
        "Les permissions sont revérifiées côté serveur quel que soit ce que l'écran affiche.",
      ],
      statusNote: "Partiellement disponible : lectures de l'organisation, demandes et approbations d'identité. La création en libre-service de sociétés et succursales et le provisionnement complet des tenants sont prévus.",
    },
    {
      id: "audit", icon: "scroll", title: "Piste d'audit", status: "partial",
      what: "Voir qui a modifié quoi, et pouvoir compter sur le fait que les faits comptabilisés restent tels qu'ils étaient.",
      how: [
        "Le registre d'audit est en ajout seul. Des déclencheurs en base empêchent les modifications.",
        "Les faits comptabilisés sont corrigés par contre-passation ou ajustement liés à l'original, jamais par écrasement.",
        "Un traitement de fond projette les événements dans le registre d'audit sans jamais comptabiliser à nouveau d'effets métier.",
      ],
      statusNote: "Partiellement disponible : les changements d'identité et les événements projetés sont audités. L'instrumentation de chaque commande est prévue.",
    },
  ],
  moreTitle: "Au-delà des modules de base",
  more: [
    { icon: "phone", status: "partial", title: "Application mobile", body: "Incluse dans chaque formule. Connexion, catalogue, ventes et brouillons de vente hors ligne fonctionnent aujourd'hui. La comptabilisation hors ligne est désactivée." },
    { icon: "card", status: "partial", title: "Paiements", body: "Espèces, crédit et acomptes sont disponibles. L'adaptateur MTN attend des identifiants réels. Orange Money n'est pas intégré." },
    { icon: "store", status: "partial", title: "Boutique en ligne", body: "Un catalogue public optionnel. Le paiement en ligne n'est pas encore disponible." },
    { icon: "monitor", status: "pending", title: "Logiciel Windows", body: "Le logiciel Windows existant reste autonome. L'adaptateur de synchronisation optionnel attend son intégration." },
  ],
  limits: [
    "QuincaiSoft est en développement actif. La présence du code source n'est pas l'acceptation d'un parcours, et aucune certification de production n'est revendiquée.",
    "Aucun agrément fiscal DGI, conformité OHADA ou autre certification n'est revendiqué.",
    "La comptabilisation hors ligne des ventes terminées sur mobile est désactivée tant que quotas signés et preuves sur appareils n'existent pas.",
  ],
  related: ["industries", "pricing", "mobile-app", "payments", "security", "faq"],
  ...CTA,
};

export const industries: IndustriesData = {
  index: {
    icon: "hardhat",
    kicker: "Secteurs",
    metaTitle: "Secteurs : quincaillerie, matériaux de construction et commerce de gros | QuincaiSoft",
    description: "Des parcours réels pour quincailleries, vendeurs de matériaux de construction et grossistes au Cameroun et en zone CEMAC : unités, découpe et reconditionnement, crédit, devis, achats, avec un état honnête pour chacun.",
    h1: "Secteurs : parcours pour la quincaillerie, les matériaux de construction et le commerce de gros",
    answer: "QuincaiSoft est conçu pour les entreprises qui vendent beaucoup de produits en beaucoup d'unités, servent des clients de passage et des professionnels, et gèrent souvent plusieurs sites. Les pages ci-dessous décrivent des parcours réels par secteur, comme découper une barre sur mesure, vendre à crédit ou établir un devis pour un entrepreneur, et marquent chacun comme disponible ou prévu.",
    blocks: [
      {
        kind: "table", id: "matrix", title: "Parcours par secteur",
        caption: "Parcours, leur état et les secteurs où ils comptent le plus",
        head: ["Parcours", "État", "Compte surtout pour"],
        rows: [
          ["Ventes au comptoir et factures numérotées", "Disponible", "Tous les secteurs"],
          ["Devis convertis en ventes", "Disponible", "Matériaux de construction"],
          ["Crédit client et acomptes", "Disponible", "Matériaux de construction, gros"],
          ["Inventaires et transferts avec transit", "Disponible", "Tous les secteurs, surtout multi-succursales"],
          ["Achats, réception, factures fournisseurs", "Disponible", "Gros, quincaillerie"],
          ["Découpe sur mesure (sans perte)", "Partiellement disponible", "Quincaillerie, matériaux de construction"],
          ["Reconditionnement du vrac en colis (sans perte)", "Partiellement disponible", "Quincaillerie, gros"],
          ["Unités de conditionnement alternatives, comme sac et tonne", "Prévu", "Matériaux de construction"],
          ["Listes de prix par groupe de clients", "Prévu", "Gros"],
          ["Remise en livraison et plafonds de crédit par projet", "Prévu", "Matériaux de construction"],
        ],
      },
      {
        kind: "text", id: "how", alt: true, title: "Comment nous décrivons les parcours",
        paras: [
          "Nous décrivons ce qui existe dans la tranche de migration actuelle et marquons tout le reste comme prévu. Quand un parcours a une limite actuelle, comme la découpe sans perte uniquement, nous le disons sur la page du secteur.",
        ],
      },
    ],
    related: ["features", "compare", "resources", "pricing"],
    sectorsTitle: "Choisissez votre secteur",
    sectorLink: "Voir les parcours",
    sectors: {
      "hardware-shops": { summary: "Grands catalogues en petites unités, ventes rapides au comptoir, découpe et reconditionnement, contrôle de caisse quotidien." },
      "construction-materials": { summary: "Devis, acomptes et crédit pour entrepreneurs, matériaux en vrac et plusieurs dépôts." },
      "wholesale-distribution": { summary: "Rigueur d'achat, factures fournisseurs, transferts entre entrepôts et clients professionnels à crédit." },
    },
    ...CTA,
  },
  items: {
    "hardware-shops": {
      icon: "hammer",
      kicker: "Quincailleries",
      metaTitle: "Logiciel pour quincailleries au Cameroun | QuincaiSoft",
      description: "QuincaiSoft pour les quincailleries du Cameroun : unités, découpe sur mesure, reconditionnement, ventes rapides au comptoir, inventaires, clients à crédit et contrôle de caisse quotidien en FCFA.",
      h1: "Logiciel de quincaillerie : comptoir rapide, stock fiable, caisse propre",
      answer: "Une quincaillerie vend des milliers de petits articles en différentes unités, coupe des tuyaux et des barres sur mesure, reconditionne vis et clous, et sert des habitués à crédit. QuincaiSoft prend en charge ventes au comptoir, quantités de stock exactes, tâches de découpe et de reconditionnement, inventaires, crédit et contrôle de caisse quotidien en FCFA, et marque comme prévu ce qui ne l'est pas encore, comme la conversion d'unités de conditionnement.",
      glance: {
        title: "Dans une quincaillerie",
        items: [
          { label: "Vente au comptoir et reçu", status: "available", note: "Facture numérotée, réimpressions COPIE." },
          { label: "Découpe et reconditionnement", status: "partial", note: "Tâches sans perte avec approbation." },
          { label: "Inventaires", status: "available", note: "Instantané, comptage, approbation, comptabilisation." },
          { label: "Scan de codes-barres", status: "planned", note: "Spécifié, pas livré." },
        ],
      },
      blocks: [
        {
          kind: "cards", id: "workflows", title: "Les parcours qui comptent dans une quincaillerie",
          items: [
            { icon: "package", status: "available", title: "Catalogue et prix", body: "Créer des produits avec référence, nom, une unité de base et un prix de vente en FCFA entiers.", points: ["Choisissez comme unité de base la plus petite unité que vous stockez. Elle ne pourra pas être changée.", "Les unités de conditionnement alternatives, comme la boîte ou le carton, avec conversion exacte, sont prévues."] },
            { icon: "cart", status: "available", title: "Vente au comptoir", body: "Tarifée par le serveur, comptabilisée avec son effet sur le stock et une facture numérotée en une seule étape.", points: ["Réimprimez une copie marquée COPIE sans comptabiliser à nouveau.", "Répéter un envoi ne peut pas créer une seconde vente."] },
            { icon: "scissors", status: "partial", title: "Découpe sur mesure", body: "Transformer une pièce en pièces entières d'un autre produit, avec la longueur exacte en millimètres.", points: ["Un approbateur indépendant valide la tâche.", "Le suivi des traits de coupe, déchets et chutes est prévu."] },
            { icon: "package", status: "partial", title: "Reconditionnement", body: "Transformer du stock en vrac en produits conditionnés grâce à une recette approuvée aux quantités exactes.", points: ["Par exemple, des clous reçus en vrac et vendus en paquets.", "La perte mesurée au reconditionnement est prévue."] },
            { icon: "clipboard", status: "available", title: "Inventaires", body: "Compter un produit dans un entrepôt et trouver l'origine des écarts.", points: ["Une modification depuis l'instantané bloque la comptabilisation.", "Un manque ne peut pas consommer en silence du stock réservé."] },
            { icon: "wallet", status: "available", title: "Habitués à crédit", body: "Vendre à crédit et encaisser plus tard avec des reçus, en voyant toujours le solde.", points: ["Un trop-perçu est rejeté."] },
            { icon: "banknote", status: "available", title: "Caisse de fin de journée", body: "Ouvrir la caisse avec un fonds et la clôturer avec un montant compté face aux espèces attendues calculées." },
          ],
        },
        {
          kind: "steps", id: "example", alt: true, title: "Exemple : découper une barre",
          intro: "Une illustration avec des quantités inventées, montrant le comportement actuel d'une tâche de découpe.",
          items: [
            { title: "Recevoir des barres de 6 000 mm", body: "Les barres sont stockées comme un produit, par exemple une barre de 6 m, comptée en pièces." },
            { title: "Créer une tâche de découpe", body: "Source : une barre de 6 m. Résultat : un produit distinct de 2 m, trois pièces entières. La tâche vérifie que 3 x 2 000 mm égale exactement 6 000 mm." },
            { title: "L'approuver", body: "Une autre personne approuve la recette et la référence de preuve." },
            { title: "Comptabiliser", body: "Le stock passe de la source au résultat, avec toute la valeur comptable. Le trait de coupe n'est pas encore comptabilisé : la tâche est donc sans perte." },
          ],
        },
        {
          kind: "limits", id: "limits", title: "Limites, en toute transparence",
          items: [
            "Découpe et reconditionnement sont sans perte. Le suivi des traits de coupe, déchets et chutes est prévu.",
            "Le scan de codes-barres n'est pas dans la tranche livrée.",
            "La compatibilité imprimantes, lecteurs et balances exige des tests sur appareils réels et n'est pas revendiquée.",
          ],
        },
        {
          kind: "faq", id: "faq", alt: true, title: "Questions de quincailliers",
          items: [
            { q: "Puis-je couper des tuyaux ou des barres de fer sur mesure ?", a: "Oui, par une tâche de découpe approuvée et sans perte : une pièce source devient des pièces entières d'un autre produit, avec une longueur exacte au millimètre. Le suivi du trait de coupe et des déchets est prévu." },
            { q: "Puis-je vendre des clous au kilo et en paquets ?", a: "Vous pouvez reconditionner du stock en vrac en produits conditionnés avec une recette approuvée. Sans perte pour l'instant." },
            { q: "Prend-il en charge les lecteurs de codes-barres ?", a: "Les codes-barres sont dans l'architecture, mais le scan n'est pas dans la tranche livrée." },
          ],
        },
      ],
      related: ["industries/construction-materials", "features", "resources/track-cement-and-bulk-materials-fcfa", "resources/manage-customer-credit-hardware-shop"],
      ...CTA,
    },
    "construction-materials": {
      icon: "hardhat",
      kicker: "Matériaux de construction",
      metaTitle: "Logiciel pour vendeurs de matériaux de construction au Cameroun | QuincaiSoft",
      description: "QuincaiSoft pour les vendeurs de matériaux de construction du Cameroun : devis pour entrepreneurs, acomptes, comptes clients, unités en vrac comme sacs de ciment et tonnes, et stock sur plusieurs dépôts.",
      h1: "Logiciel pour matériaux de construction : devis, acomptes et unités en vrac",
      answer: "Ciment, fer à béton, tôles et carreaux se vendent souvent sur devis, avec un acompte ou sur compte, et en unités comme le sac, la barre, le mètre et la tonne. QuincaiSoft prend déjà en charge les devis convertis en ventes, les acomptes, les comptes clients et le stock sur plusieurs dépôts, et marque comme prévus la conversion sac-tonne, la remise en livraison et les plafonds de crédit par projet.",
      glance: {
        title: "Dans un commerce de matériaux",
        items: [
          { label: "Du devis à la vente", status: "available", note: "Instantané numéroté, conversion liée." },
          { label: "Acomptes et crédit", status: "available", note: "Soldes et affectations en FCFA." },
          { label: "Plusieurs dépôts", status: "available", note: "Stock par entrepôt et transferts." },
          { label: "Conversion sac et tonne", status: "planned", note: "La conversion exacte est spécifiée." },
        ],
      },
      blocks: [
        {
          kind: "cards", id: "workflows", title: "Les parcours qui comptent pour les matériaux de construction",
          items: [
            { icon: "file", status: "available", title: "Devis pour entrepreneurs", body: "Brouillon avec prix du catalogue et date d'expiration, émission d'un instantané numéroté et conversion du devis accepté en vente liée.", points: ["La conversion est rejetée si le prix a changé ou si le devis a expiré."] },
            { icon: "landmark", status: "available", title: "Acomptes et paiements échelonnés", body: "Prendre un acompte, le conserver comme dette envers le client et l'affecter à la vente sans autre encaissement.", points: ["Le remboursement en espèces d'un acompte inutilisé exige une permission distincte."] },
            { icon: "wallet", status: "available", title: "Comptes clients", body: "Vendre à crédit et appliquer les encaissements à la dette reconnue, avec le solde toujours visible." },
            { icon: "ruler", status: "planned", title: "Sac, tonne et autres unités", body: "Vendre la même matière au sac ou à la tonne avec une conversion exacte.", points: ["Aujourd'hui chaque produit a une seule unité de base.", "Les conversions non exactes sont spécifiées comme rejetées, pas arrondies."] },
            { icon: "warehouse", status: "available", title: "Plusieurs dépôts", body: "Stock par entrepôt, avec des transferts passant par approbation, transit et réception." },
            { icon: "undo", status: "available", title: "Retours avec inspection", body: "Reprendre des matériaux inutilisés contre la vente d'origine, avec un avoir." },
            { icon: "truck", status: "planned", title: "Remise en livraison et plafonds par projet", body: "Preuves de livraison, budgets de projet et sous-plafonds de crédit sont spécifiés et pas construits." },
          ],
        },
        {
          kind: "text", id: "units", alt: true, title: "Exemple : le ciment en sacs et en tonnes",
          paras: [
            "Si un sac pèse 50 kg, une tonne vaut 20 sacs. Aujourd'hui, choisissez le sac comme unité de base, recevez et vendez en sacs, et tenez un produit en vrac distinct si vous stockez aussi de la matière libre, que vous passez en sacs par une recette de reconditionnement approuvée.",
            "La conversion automatique sur les lignes d'achat et de vente, pour qu'une livraison en tonnes devienne exactement des sacs, est prévue. La spécification rejette toute conversion qui ne tombe pas juste au lieu d'arrondir le stock.",
          ],
        },
        {
          kind: "steps", id: "quotation", title: "Exemple : du devis à la vente encaissée",
          items: [
            { title: "Devis", body: "Établissez le devis avec les prix du catalogue et une date d'expiration, puis émettez-le comme instantané numéroté." },
            { title: "Prendre un acompte", body: "Quand l'entrepreneur accepte, enregistrez l'acompte. Il est conservé comme dette envers le client." },
            { title: "Convertir", body: "Convertissez le devis accepté en un brouillon de vente lié. Un prix modifié ou un devis expiré est rejeté." },
            { title: "Comptabiliser et affecter", body: "Comptabilisez la vente et affectez-y l'acompte sans enregistrer un second encaissement." },
            { title: "Encaisser le solde", body: "Appliquez les encaissements suivants à la créance restante jusqu'à son apurement." },
          ],
        },
        {
          kind: "limits", id: "limits", alt: true, title: "Limites, en toute transparence",
          items: [
            "La conversion de devis rejette pour l'instant les devis comportant une taxe.",
            "L'acceptation partielle en plusieurs commandes et les plafonds de crédit par projet sont prévus.",
            "Les preuves de remise en livraison sont prévues.",
          ],
        },
        {
          kind: "faq", id: "faq", title: "Questions sur les matériaux de construction",
          items: [
            { q: "Puis-je vendre le ciment au sac et à la tonne ?", a: "Aujourd'hui chaque produit a une seule unité de base : choisissez le sac et vendez en sacs. La conversion automatique sac-tonne est prévue." },
            { q: "Un entrepreneur peut-il verser d'abord un acompte ?", a: "Oui. Un acompte est conservé comme dette envers le client et affecté plus tard à la vente, sans second encaissement." },
          ],
        },
      ],
      related: ["industries/hardware-shops", "industries/wholesale-distribution", "resources/quotations-for-contractors", "resources/track-cement-and-bulk-materials-fcfa"],
      ...CTA,
    },
    "wholesale-distribution": {
      icon: "warehouse",
      kicker: "Gros et distribution",
      metaTitle: "Logiciel pour grossistes et distributeurs en zone CEMAC | QuincaiSoft",
      description: "QuincaiSoft pour les grossistes et distributeurs du Cameroun et de la zone CEMAC : achats avec approbation, réceptions partielles, factures fournisseurs, transferts entre entrepôts et clients professionnels à crédit.",
      h1: "Logiciel de gros et distribution : achats, stock et comptes professionnels",
      answer: "Un grossiste vit de la rigueur d'achat, de la visibilité du stock et du crédit commercial. QuincaiSoft prend déjà en charge commandes d'achat approuvées, réceptions partielles, factures fournisseurs, transferts via transit, crédit client et rapports, et marque comme prévus les listes de prix par groupe de clients et les paiements fournisseurs.",
      glance: {
        title: "Dans un commerce de gros",
        items: [
          { label: "Cycle d'achat", status: "available", note: "Commander, approuver, réceptionner, facturer." },
          { label: "Transferts entre entrepôts", status: "available", note: "Approbation, transit et réception." },
          { label: "Clients professionnels à crédit", status: "available", note: "Créances et encaissements." },
          { label: "Listes de prix par groupe de clients", status: "planned", note: "Spécifié, pas livré." },
        ],
      },
      blocks: [
        {
          kind: "cards", id: "workflows", title: "Les parcours qui comptent dans le commerce de gros",
          items: [
            { icon: "truck", status: "available", title: "Cycle d'achat", body: "Créer un brouillon d'achat, le soumettre, l'approuver de façon indépendante et réceptionner en une ou plusieurs livraisons.", points: ["Commander ne modifie pas le stock. Réceptionner, si.", "Une réception ne peut dépasser la quantité commandée."] },
            { icon: "receipt", status: "available", title: "Factures fournisseurs", body: "Enregistrer la facture fournisseur contre une réception, ce qui crée la dette.", points: ["Une facture par réception, avec une référence fournisseur unique.", "Paiements fournisseurs et balance âgée sont prévus."] },
            { icon: "transfer", status: "available", title: "Transferts entre entrepôts", body: "Déplacer du stock avec approbation, expédition en transit et réception partielle ou finale.", points: ["Le stock n'est pas disponible aux deux bouts à la fois."] },
            { icon: "wallet", status: "available", title: "Clients professionnels à crédit", body: "Vendre à crédit, appliquer les encaissements et voir les créances restantes par vente." },
            { icon: "chart", status: "partial", title: "Rapports", body: "Ventes comptabilisées avec marge, valeur du stock, créances et caisse, avec totaux sur l'ensemble du filtre." },
            { icon: "layers", status: "planned", title: "Listes de prix par groupe de clients", body: "Des prix différents pour clients détail, professionnels et grossistes.", points: ["Aujourd'hui le catalogue contient un seul prix de vente par produit."] },
          ],
        },
        {
          kind: "steps", id: "example", alt: true, title: "Exemple : de la commande d'achat à la dette",
          items: [
            { title: "Rédiger et soumettre", body: "Un responsable des achats rédige la commande avec fournisseur, quantités et coûts unitaires et la soumet." },
            { title: "Approuver", body: "Une autre personne approuve. L'initiateur ne peut pas approuver sa propre commande." },
            { title: "Réceptionner par parties", body: "Chaque livraison est une réception. Le stock augmente et les marchandises reçues non encore facturées sont enregistrées." },
            { title: "Enregistrer la facture", body: "Quand le fournisseur facture une réception, la dette est créée à partir d'elle." },
            { title: "Retourner ce qui ne va pas", body: "Les marchandises non facturées peuvent être retournées dans la limite de ce qui a été reçu, et le stock diminue à la valeur moyenne pondérée courante." },
          ],
        },
        {
          kind: "limits", id: "limits", title: "Limites, en toute transparence",
          items: [
            "L'affectation des paiements fournisseurs, la balance âgée, les frais d'approche et les demandes d'achat sont prévus.",
            "Les listes de prix par groupe de clients sont prévues.",
            "Le commerce entre vos propres entités juridiques est prévu.",
          ],
        },
        {
          kind: "faq", id: "faq", alt: true, title: "Questions sur le commerce de gros",
          items: [
            { q: "Puis-je réceptionner une livraison en plusieurs parties ?", a: "Oui. Les réceptions peuvent être partielles et la commande ne se clôt que lorsque tout ce qui était commandé a été reçu." },
            { q: "Puis-je donner des prix spéciaux aux clients professionnels ?", a: "Les listes de prix par groupe de clients sont prévues. Aujourd'hui le catalogue contient un seul prix de vente par produit." },
          ],
        },
      ],
      related: ["industries/construction-materials", "resources/buy-and-receive-stock-from-suppliers", "resources/run-multiple-branches", "features"],
      ...CTA,
    },
  },
};

export const pricing: PricingData = {
  icon: "calculator",
  kicker: "Tarifs",
  metaTitle: "Tarifs et structure des formules | QuincaiSoft",
  description: "Structure des formules QuincaiSoft : Starter, Growth, Enterprise et boutique en ligne optionnelle, en plusieurs modes de déploiement, application mobile incluse. Contactez-nous pour les tarifs.",
  h1: "Tarifs : structure des formules, sans prix inventés",
  answer: "Nous n'avons pas publié de grille tarifaire. Contactez-nous pour les tarifs. Cette page montre comment QuincaiSoft est structuré : formules Starter, Growth et Enterprise, une boutique en ligne optionnelle, plusieurs modes de déploiement, et l'application mobile incluse dans chaque formule. Les prix dépendent de vos succursales, utilisateurs et modules et de votre mode de déploiement, et aucun prix, essai ou remise n'est sous-entendu ici.",
  glance: {
    title: "En un coup d'œil",
    items: [
      { label: "Prix", note: "Contactez-nous pour les tarifs." },
      { label: "Application mobile", note: "Incluse dans chaque formule." },
      { label: "Boutique en ligne", status: "planned", note: "Option complémentaire." },
      { label: "Déploiement", note: "Cloud, dédié, hébergé par le client ou Windows." },
    ],
  },
  blocks: [
    {
      kind: "cards", id: "plans", title: "Structure des formules",
      intro: "Les formules ci-dessous décrivent la structure visée. Les limites et le contenu peuvent évoluer avec la maturité du produit, et rien ici n'est une offre.",
      items: [
        { icon: "store", status: "planned", title: "Starter", body: "Pour une ou quelques succursales.", points: ["Stock et point de vente", "Fiches clients et rapports", "Application mobile", "Support de base"] },
        { icon: "building", status: "planned", title: "Growth", body: "Pour plusieurs succursales.", points: ["Exploitation multi-succursales", "Achats et dettes fournisseurs", "Crédit, acomptes et approbations par rôle", "Mobile hors ligne contrôlé", "Rapports avancés et intégrations"] },
        { icon: "server", status: "planned", title: "Enterprise / Dédié", body: "Pour des déploiements plus grands ou plus exigeants.", points: ["Base de données et exécution isolées", "Périmètre d'API avancé", "Niveau de service contractuel", "Mise en route et support dédiés", "Droits sectoriels sur mesure"] },
        { icon: "cart", status: "planned", title: "Boutique en ligne (optionnelle)", body: "Une boutique du tenant, ajoutée lorsque contractualisée.", points: ["Séparée du cœur ERP", "Les frais du prestataire de paiement du commerçant sont distincts"] },
      ],
    },
    {
      kind: "table", id: "compare", alt: true, title: "Comparer les formules",
      caption: "Ce que chaque formule est destinée à inclure",
      head: ["Capacité", "Starter", "Growth", "Enterprise / Dédié"],
      rows: [
        ["Succursales", "Une ou quelques-unes", "Plusieurs", "Selon contrat"],
        ["Stock et point de vente", "Inclus", "Inclus", "Inclus"],
        ["Fiches clients et rapports", "Inclus", "Inclus", "Inclus"],
        ["Application mobile", "Incluse", "Incluse", "Incluse"],
        ["Achats et dettes fournisseurs", "Non inclus", "Inclus", "Selon contrat"],
        ["Crédit, acomptes et approbations par rôle", "Non inclus", "Inclus", "Selon contrat"],
        ["Mobile hors ligne contrôlé", "Non inclus", "Inclus (à sa sortie)", "Selon contrat"],
        ["Rapports avancés et intégrations", "Non inclus", "Inclus", "Selon contrat"],
        ["Base de données et exécution isolées", "Non inclus", "Non inclus", "Inclus"],
        ["Niveau de service contractuel", "Non inclus", "Non inclus", "Inclus"],
        ["Boutique en ligne", "Optionnelle", "Optionnelle", "Optionnelle"],
      ],
    },
    {
      kind: "table", id: "deployment", title: "Modes de déploiement",
      intro: "Une formule se combine avec un mode de déploiement. Tous les modes ne sont pas disponibles aujourd'hui.",
      caption: "Modes de déploiement et forme de licence",
      head: ["Mode", "Où cela fonctionne", "Forme commerciale", "État"],
      rows: [
        ["Cloud standard", "Service cloud partagé, isolé par tenant", "Abonnement", "En développement"],
        ["Cloud dédié", "Déploiement isolé avec sa propre base de données", "Abonnement entreprise", "Prévu"],
        ["Hébergé par le client", "Votre cloud privé ou serveur sur site", "Licence logicielle plus maintenance", "Prévu"],
        ["Windows autonome", "Votre PC, base locale, sans cloud", "Licence perpétuelle de bureau", "Logiciel existant"],
        ["Windows connecté au cloud", "Site Windows coordonné avec le cloud", "Bureau plus droit de connecteur", "Prévu"],
      ],
    },
    {
      kind: "cards", id: "included", alt: true, title: "Ce qui est identique dans chaque formule",
      items: [
        { icon: "phone", title: "Application mobile", body: "Chaque formule inclut l'accès mobile aux fonctions auxquelles vous souscrivez. Le prix suit l'échelle et les droits, pas une application séparée." },
        { icon: "layers", title: "Un seul produit", body: "Une formule est un ensemble de droits. Il n'y a pas de code distinct par formule." },
        { icon: "scroll", title: "La facturation ne modifie jamais les enregistrements", body: "Un changement de formule est un événement audité. Les enregistrements financiers historiques ne sont pas modifiés à cause de la facturation." },
        { icon: "globe", title: "Français, anglais et FCFA", body: "Interface et documents en français et en anglais, avec le FCFA comme monnaie de travail." },
      ],
    },
    {
      kind: "cards", id: "factors", title: "Ce qui influence le prix",
      items: [
        { icon: "users", title: "L'échelle", body: "Nombre de succursales, d'entrepôts et de postes utilisateurs." },
        { icon: "list", title: "Les modules", body: "Les modules et intégrations dont vous avez besoin." },
        { icon: "cloud", title: "Le déploiement", body: "Cloud partagé, cloud dédié, hébergé par le client ou Windows." },
        { icon: "handshake", title: "Support et mise en route", body: "Niveau de mise en route, de formation et de support." },
        { icon: "server", title: "Coûts distincts", body: "Hébergement, matériel, frais du prestataire de paiement pour les ventes en ligne et déplacements de mise en œuvre sont détaillés séparément." },
      ],
    },
    {
      kind: "faq", id: "faq", alt: true, title: "Questions sur les tarifs",
      items: [
        { q: "Combien coûte QuincaiSoft ?", a: "Nous n'avons pas publié de prix. Contactez-nous pour les tarifs. Cela dépend des succursales, utilisateurs, modules et du déploiement." },
        { q: "Y a-t-il un essai gratuit ou une remise ?", a: "Nous ne publions pas d'essais ni de remises non définis. Interrogez-nous sur un pilote." },
        { q: "L'application mobile est-elle en supplément ?", a: "Non. Chaque formule inclut l'accès mobile aux fonctions auxquelles vous souscrivez." },
        { q: "La boutique en ligne est-elle incluse ?", a: "Elle est optionnelle et ajoutée lorsque contractualisée. Le paiement en ligne n'est pas encore disponible." },
        { q: "Ai-je besoin du cloud ?", a: "Pas forcément. Windows autonome fonctionne sans compte cloud, et les modes hébergé par le client et dédié sont prévus." },
        { q: "Puis-je changer de formule plus tard ?", a: "C'est la conception retenue. Un changement de formule est une modification de droits datée et auditée, et les enregistrements historiques ne sont pas altérés." },
      ],
    },
  ],
  related: ["features", "compare", "faq", "migration-and-onboarding"],
  ctaTitle: "Contactez-nous pour les tarifs",
  ctaBody: "Parlez-nous de vos succursales, de vos utilisateurs et des modules dont vous avez besoin, et nous vous répondons avec une proposition.",
  formTitle: "Demander un devis",
};

const G = {
  about: "À propos de QuincaiSoft",
  stock: "Stock, unités et achats",
  sales: "Ventes, crédit et documents",
  payments: "Paiements et mobile money",
  mobile: "Mobile et hors ligne",
  security: "Sécurité, données et conformité",
  deployment: "Déploiement et Windows",
  pricing: "Tarifs et disponibilité",
};

export const faq: FaqData = {
  metaTitle: "FAQ : réponses sur QuincaiSoft | QuincaiSoft",
  description: "Réponses sur QuincaiSoft : stock et unités, sacs et tonnes de ciment, crédit et acomptes, MTN et Orange Money, usage hors ligne, sécurité, Windows, tarifs et disponibilité.",
  kicker: "FAQ",
  h1: "Questions fréquentes",
  answer: "Des réponses courtes et directes sur QuincaiSoft, regroupées par thème. Chaque réponse dit clairement ce qui est disponible aujourd'hui et ce qui est prévu.",
  groups: [
    {
      id: "about", title: G.about,
      items: [
        { q: "Qu'est-ce que QuincaiSoft ?", a: "QuincaiSoft est un ERP et point de vente pour les commerces de détail, de négoce et de distribution, développé par Opesware Technologies. Il couvre catalogue, stock, devis, achats, ventes, caisse et documents commerciaux." },
        { q: "À qui s'adresse QuincaiSoft ?", a: "Il est conçu pour les quincailleries, les vendeurs de matériaux de construction et les grossistes ou distributeurs du Cameroun et de la zone CEMAC, y compris les entreprises à plusieurs succursales." },
        { q: "Qui développe QuincaiSoft ?", a: "Opesware Technologies développe QuincaiSoft pour les commerces de détail, de négoce et de distribution du Cameroun et de la zone CEMAC." },
        { q: "QuincaiSoft est-il prêt pour la production ?", a: "Il est en développement actif. Plusieurs modules existent, mais tous les parcours ne sont pas terminés et aucune certification de production n'est revendiquée. Contactez-nous pour discuter d'un pilote." },
        { q: "Où obtenir une démonstration ?", a: "Utilisez la page de contact pour demander une démonstration ou un échange sur un pilote. Nous répondons par e-mail en français ou en anglais." },
      ],
    },
    {
      id: "stock", title: G.stock,
      items: [
        { q: "Peut-il gérer plusieurs succursales et entrepôts ?", a: "Oui. Stock, ventes et caisse sont organisés par succursale et entrepôt, avec des rapports consolidés pour les utilisateurs autorisés." },
        { q: "Puis-je vendre du ciment au sac et à la tonne ?", a: "Aujourd'hui chaque produit a une seule unité de base : vous vendriez le ciment en sacs. La conversion automatique entre sacs et tonnes, avec une arithmétique exacte, est prévue." },
        { q: "Puis-je découper ou reconditionner des produits ?", a: "Oui, par des tâches approuvées. La découpe transforme une pièce en pièces entières d'un autre produit avec une longueur exacte au millimètre, et le reconditionnement transforme du vrac en colis. Les deux sont sans perte pour l'instant ; trait de coupe, déchets et perte mesurée sont prévus." },
        { q: "Puis-je inventorier le stock et le transférer entre succursales ?", a: "Oui. Les inventaires prennent un instantané, exigent une approbation indépendante et comptabilisent gains et pertes. Les transferts passent par approbation, transit et réception." },
        { q: "Gère-t-il les achats et les fournisseurs ?", a: "Oui. Commandes fournisseurs, réceptions de marchandises et soldes fournisseurs sont reliés au stock." },
      ],
    },
    {
      id: "sales", title: G.sales,
      items: [
        { q: "Fonctionne-t-il en FCFA ?", a: "Oui. Le FCFA (XAF) est la monnaie de travail, et les montants et documents sont présentés en conséquence." },
        { q: "Est-il disponible en français et en anglais ?", a: "Oui. L'interface et les documents commerciaux sont conçus pour être bilingues, français et anglais." },
        { q: "Puis-je établir des devis ?", a: "Oui. Les devis font partie du produit, avec suivi et conversion en ventes, ainsi que comptes clients, acomptes et retours." },
        { q: "Puis-je vendre à crédit et prendre des acomptes ?", a: "Oui. Ventes à crédit, encaissements, acomptes, affectations aux ventes et remboursements d'acomptes approuvés sont disponibles. Plafonds de crédit et balance âgée sont prévus." },
        { q: "Puis-je imprimer des reçus et des factures ?", a: "Les documents numérotés ont une vue d'impression A4 en français et en anglais, et les réimpressions portent la mention COPIE. Les imprimantes de tickets thermiques exigent des tests sur appareils réels et ne sont pas revendiquées pour l'instant." },
        { q: "Puis-je gérer des retours ?", a: "Oui. Un retour renvoie à la vente d'origine, est inspecté et approuvé de façon indépendante, et émet un avoir. Il ne paie pas à lui seul d'espèces ni de remboursement via un fournisseur." },
      ],
    },
    {
      id: "payments", title: G.payments,
      items: [
        { q: "Prend-il en charge MTN Mobile Money ?", a: "Partiellement. Un adaptateur MTN d'intention de paiement et de rapprochement existe, mais les identifiants réels et l'acceptation du fournisseur sont encore requis. Un paiement n'est pas affiché comme vérifié tant que le fournisseur ne l'a pas confirmé." },
        { q: "Prend-il en charge Orange Money ?", a: "Pas encore. Orange Money attend une intégration contractualisée et n'est pas indiqué comme intégré. Des références peuvent être saisies manuellement, mais elles ne sont jamais présentées comme vérifiées." },
        { q: "QuincaiSoft détient-il l'argent de mes clients ?", a: "Non. Les paiements appartiennent au commerçant et à son prestataire de paiement agréé. Opesware Technologies n'est pas dépositaire des espèces ni des règlements des commerçants." },
      ],
    },
    {
      id: "mobile", title: G.mobile,
      items: [
        { q: "Existe-t-il une application mobile ?", a: "Oui. Une application mobile native est incluse dans chaque formule. Elle se connecte de façon sécurisée, recherche dans le catalogue, affiche les ventes et conserve des brouillons de vente hors ligne." },
        { q: "Fonctionne-t-il hors ligne ?", a: "Le client mobile est conçu pour une connexion faible et se déploie par étapes : d'abord en ligne avec catalogue en cache en lecture seule, puis inventaires hors ligne, puis ventes au comptant hors ligne contrôlées. La comptabilisation hors ligne des ventes est désactivée tant que le quota et les preuves sur appareils ne sont pas complets." },
      ],
    },
    {
      id: "security", title: G.security,
      items: [
        { q: "Mes données sont-elles séparées de celles des autres entreprises ?", a: "C'est la conception retenue. Les données des clients sont isolées dans PostgreSQL par une sécurité au niveau des lignes forcée, et une requête sans tenant vérifié échoue de façon sûre." },
        { q: "Puis-je limiter ce que chaque personne peut faire ?", a: "Oui. Rôles et permissions sont appliqués côté serveur, et les changements sensibles exigent un approbateur indépendant." },
        { q: "QuincaiSoft est-il agréé par la DGI pour la facturation fiscale ?", a: "Aucun agrément n'est revendiqué. Les règles fiscales sont paramétrables, mais l'approbation doit venir des autorités compétentes et de vos comptables." },
        { q: "A-t-il des certifications OHADA, ISO ou autres ?", a: "Aucune certification n'est revendiquée. Les désignations de conformité exigent la revue de juristes et de comptables et n'ont pas été obtenues." },
        { q: "Puis-je exporter mes données ?", a: "Les rapports peuvent aujourd'hui être exportés en CSV, page par page. Un export complet de vos enregistrements dans un format lisible par machine est prévu." },
      ],
    },
    {
      id: "deployment", title: G.deployment,
      items: [
        { q: "Peut-il se connecter à mon logiciel Windows existant ?", a: "Un pont pour les installations Windows existantes est prévu, mais son intégration est en attente. Le logiciel Windows existant continue de fonctionner seul." },
        { q: "Dois-je utiliser le cloud ?", a: "Non. Windows autonome n'exige aucun compte cloud. Les modes hébergé par le client et dédié sont prévus." },
        { q: "Puis-je vendre en ligne ?", a: "Une boutique en ligne optionnelle peut publier votre catalogue. Le paiement en ligne n'est pas encore disponible." },
        { q: "Puis-je importer mes produits et clients existants ?", a: "Un import supervisé avec essai à blanc est prévu. Aujourd'hui la mise en route est organisée avec notre équipe." },
      ],
    },
    {
      id: "pricing", title: G.pricing,
      items: [
        { q: "Combien cela coûte-t-il ?", a: "Nous n'avons pas publié de prix. Contactez-nous pour les tarifs ; cela dépend du nombre de succursales, d'utilisateurs et de modules." },
        { q: "Y a-t-il un essai gratuit ?", a: "Nous ne publions pas d'essais non définis. Interrogez-nous sur un pilote." },
        { q: "Quelles formules existent ?", a: "La structure prévue est Starter, Growth et Enterprise, avec une boutique en ligne optionnelle. L'application mobile est incluse dans chaque formule." },
      ],
    },
  ],
  related: ["features", "pricing", "compare", "resources"],
  ...CTA,
};
