import type { Lang } from "./site";

export type IconName =
  | "Package" | "ScanBarcode" | "Warehouse" | "Store" | "Truck" | "FileText" | "ReceiptText" | "Wallet" | "Smartphone"
  | "WifiOff" | "Users" | "BarChart3" | "ShieldCheck" | "Languages" | "Banknote" | "Building2" | "ClipboardList"
  | "HandCoins" | "ShoppingCart" | "Scale" | "ClipboardCheck" | "Boxes" | "History" | "Lock" | "Repeat" | "KeyRound"
  | "Hammer" | "HardHat" | "Layers";

export type Item = { icon: IconName; title: string; body: string };
export type Problem = { icon: IconName; pain: string; fix: string; feature: string };

export type HomeContent = {
  meta: { title: string; description: string };
  hero: {
    eyebrow: string; h1: string; definition: string; lead: string; ctaDemo: string; ctaFeatures: string; status: string;
    screen: {
      caption: string; label: string; title: string; branch: string; headers: [string, string, string, string];
      lines: [string, string, string, string][]; totalLabel: string; total: string; tenders: string[]; stockNote: string;
    };
  };
  facts: { title: string; items: Item[] };
  problems: { title: string; intro: string; items: Problem[]; fixLabel: string };
  features: { title: string; intro: string; items: Item[]; listName: string; more: string };
  how: { title: string; intro: string; steps: { title: string; body: string }[] };
  industries: { title: string; intro: string; icons: Record<"hardware-shops" | "construction-materials" | "wholesale-distribution", IconName>; link: string };
  mobile: {
    title: string; intro: string; worksTitle: string; works: string[]; limitsTitle: string; limits: string[]; stagesTitle: string; stages: string[];
    phone: { title: string; rows: [string, string][]; note: string };
  };
  payments: { title: string; intro: string; items: { name: string; status: string; body: string }[]; more: string };
  security: { title: string; intro: string; items: Item[]; disclaimer: string };
  faq: { title: string; intro: string; items: { q: string; a: string }[]; all: string };
  cta: { title: string; body: string; primary: string; secondary: string };
  itemListName: string;
};

const en: HomeContent = {
  meta: {
    title: "QuincaiSoft | ERP and POS for Hardware Shops, Cameroon",
    description: "Bilingual ERP and point of sale in FCFA for hardware shops, construction-material retailers and wholesalers in Cameroon: stock, quotes, purchasing, cash.",
  },
  hero: {
    eyebrow: "ERP and point of sale for trade in Cameroon and CEMAC",
    h1: "Know your stock, sales and cash, branch by branch",
    definition: "QuincaiSoft is a bilingual ERP and point of sale, in FCFA (XAF), for hardware shops, construction-material retailers and wholesalers in Cameroon and the CEMAC region. It connects the counter, the warehouse, suppliers and the cash desk in one product.",
    lead: "Sell by the bag, the bar, the metre or the tonne. Quote contractors, take deposits, receive supplier deliveries and close the day with cash that reconciles.",
    ctaDemo: "Request a demo",
    ctaFeatures: "See features",
    status: "In active development, with pilots under discussion. We say plainly what is finished and what is not, further down this page.",
    screen: {
      caption: "Illustration of a counter sale screen. The items and amounts are examples, not real data or customers.",
      label: "Illustrative example",
      title: "Counter sale",
      branch: "Branch: Main shop",
      headers: ["Item", "Unit", "Qty", "Amount (FCFA)"],
      lines: [
        ["Cement, 50 kg bag", "bag", "20", "130 000"],
        ["Iron bar, 12 mm", "bar", "30", "165 000"],
        ["Roofing sheet", "piece", "12", "96 000"],
        ["Binding wire", "kg", "2.5", "4 500"],
      ],
      totalLabel: "Total",
      total: "395 500 FCFA",
      tenders: ["Cash", "Deposit", "Customer credit"],
      stockNote: "Stock updates in the branch when the sale is posted.",
    },
  },
  facts: {
    title: "The basics, stated plainly",
    items: [
      { icon: "Banknote", title: "FCFA (XAF)", body: "Amounts, prices and documents are in FCFA, the working currency of the product." },
      { icon: "Languages", title: "English and French", body: "Interface and business documents are designed to be bilingual." },
      { icon: "Building2", title: "Several branches", body: "Stock, sales and cash are organised per branch and warehouse." },
      { icon: "Smartphone", title: "Mobile app in every plan", body: "Every commercial package includes the mobile app. What each person can do is set by permissions." },
      { icon: "WifiOff", title: "Built for weak connectivity", body: "The mobile client is designed for poor networks and released in stages, with limits explained below." },
      { icon: "ShieldCheck", title: "Honest status", body: "No fiscal approval or certification is claimed, and no customer figures are invented." },
    ],
  },
  problems: {
    title: "Problems we are built to solve",
    intro: "Hardware and wholesale trade has its own daily headaches. These are the ones QuincaiSoft is designed around.",
    fixLabel: "How QuincaiSoft helps",
    items: [
      { icon: "Boxes", pain: "Stock on the shelf never matches the books", fix: "Every stock movement is recorded and kept, and stock counts show where differences come from.", feature: "Stock and movement history" },
      { icon: "Scale", pain: "One product, many units", fix: "Sell cement by the bag, iron by the bar or the tonne, wire by the kilo, with fractional quantities and one consistent stock.", feature: "Units and fractional quantities" },
      { icon: "ClipboardList", pain: "Contractors want a quote first", fix: "Prepare a quotation, follow it up and convert it to a sale when accepted.", feature: "Quotations" },
      { icon: "HandCoins", pain: "Deposits and credit are tracked on paper", fix: "Record customer deposits, staged payments and credit, with a clear balance per customer.", feature: "Customer accounts and deposits" },
      { icon: "Truck", pain: "Supplier deliveries and payables get lost", fix: "Record supplier orders and goods receipts so purchases, stock and what you owe stay connected.", feature: "Purchasing and suppliers" },
      { icon: "Wallet", pain: "The till does not reconcile at day end", fix: "Open and close cash shifts and record expenses, so cash can be followed from the till to the books.", feature: "Cash shifts and expenses" },
    ],
  },
  features: {
    title: "Everything a trade counter and its back office need",
    intro: "One product for the counter, the warehouse and the office. This is the intended scope; the status notes below say what is still in progress.",
    listName: "QuincaiSoft features",
    more: "See all features and their status",
    items: [
      { icon: "ShoppingCart", title: "Sales and point of sale", body: "Counter sales with cash, split payments, customer credit and deposits, plus returns and refunds." },
      { icon: "ScanBarcode", title: "Catalogue and barcodes", body: "Products, categories, barcodes and price lists for walk-in and regular customers." },
      { icon: "Package", title: "Stock, units and fractions", body: "Stock per branch and warehouse, several units per product, fractional quantities, counts and reservations." },
      { icon: "Truck", title: "Purchasing and suppliers", body: "Supplier orders, goods receipts and supplier balances connected to stock." },
      { icon: "HandCoins", title: "Customer credit and deposits", body: "Customer accounts, deposits and staged payments with clear balances." },
      { icon: "ClipboardList", title: "Quotations", body: "Quotations for contractors, followed through to sale." },
      { icon: "Wallet", title: "Cash and expenses", body: "Cash shifts, expenses and reconciliation to follow money from the till to the books." },
      { icon: "ReceiptText", title: "Documents and receipts", body: "Receipts and business documents in English or French, in FCFA, with preview and reprint marking." },
      { icon: "BarChart3", title: "Reports", body: "Operational reports for stock, sales, purchasing and cash across branches." },
      { icon: "Smartphone", title: "Mobile money status", body: "MTN adapter exists and still needs live acceptance. Orange Money is not integrated yet. Details below." },
    ],
  },
  how: {
    title: "How it works",
    intro: "From first conversation to daily use, in four steps.",
    steps: [
      { title: "Tell us about your business", body: "Share your type of business, the number of branches and what you want to improve. We reply by email, in English or French." },
      { title: "See it on your own cases", body: "We walk through a demonstration around your products, units, quotations and cash routine." },
      { title: "Set up your catalogue and branches", body: "Products, units, prices, branches, warehouses and user roles are prepared with you." },
      { title: "Sell, receive, reconcile", body: "Run the counter, receive supplier deliveries and close each day with stock and cash you can check." },
    ],
  },
  industries: {
    title: "Made for trade and distribution",
    intro: "Businesses that sell many products in many units, serve walk-in and trade customers, and often run more than one location.",
    icons: { "hardware-shops": "Hammer", "construction-materials": "HardHat", "wholesale-distribution": "Warehouse" },
    link: "Read more",
  },
  mobile: {
    title: "Mobile and offline, with honest limits",
    intro: "The same QuincaiSoft mobile app serves owners, managers, cashiers, stock staff and delivery agents, and shows each person only what their permissions allow. It is designed for weak connectivity, and released in stages.",
    worksTitle: "What the mobile app is for",
    works: [
      "Owners: dashboard, branch summaries, approvals and alerts.",
      "Cashiers: point of sale, shifts, receipts and product lookup.",
      "Stock staff: counts, receiving, transfers and barcode scanning.",
      "Delivery staff: pickups, manifests and handover.",
    ],
    stagesTitle: "Release stages",
    stages: [
      "Stage 1: online use with a read-only cached catalogue.",
      "Stage 2: offline stock counts.",
      "Stage 3: controlled offline cash sales.",
    ],
    limitsTitle: "Limits, stated plainly",
    limits: [
      "Offline posting of sales is currently disabled. It will stay disabled until stock quotas and native-device evidence are complete.",
      "A phone working without a connection cannot claim live balances or completed sale or stock postings.",
      "Stages are released only when the evidence is in, not on a calendar date.",
    ],
    phone: {
      title: "Mobile app",
      rows: [["Catalogue (read-only cache)", "Online"], ["Stock counts", "Planned, offline"], ["Offline sales posting", "Disabled"]],
      note: "Illustration of release status, not a screenshot.",
    },
  },
  payments: {
    title: "Payments and mobile money, as they stand today",
    intro: "Mobile money matters in Cameroon, so here is exactly where each provider stands. A payment is not shown as verified until the provider confirms it.",
    items: [
      { name: "MTN Mobile Money", status: "Adapter built, live acceptance pending", body: "A durable payment-intent and reconciliation adapter exists. Live provider credentials, acceptance and verification are still required." },
      { name: "Orange Money", status: "Not integrated yet", body: "Pending a contracted integration. It is not marked as integrated." },
      { name: "Manual references", status: "Recorded, never labelled verified", body: "You can record a mobile money, bank transfer or cash reference at the counter. References entered by hand are never shown as verified." },
    ],
    more: "Read the full status and limits",
  },
  security: {
    title: "Security and reliability you can check",
    intro: "These are design rules from our architecture specification. They are not certifications.",
    disclaimer: "No security certification, DGI fiscal approval or OHADA conformity is claimed.",
    items: [
      { icon: "Lock", title: "Tenant isolation", body: "Companies share the platform but not data: every business record carries a tenant ID enforced in the database with row-level security." },
      { icon: "History", title: "Audit trail", body: "Sensitive actions are recorded with who did them, and stock movements are immutable history." },
      { icon: "Repeat", title: "Idempotent postings", body: "Retried requests do not post the same sale or movement twice, thanks to idempotency keys." },
      { icon: "KeyRound", title: "Roles and permissions", body: "Per-user roles and branch-level permissions decide who can see and do what." },
      { icon: "ShieldCheck", title: "Fails closed", body: "When a rule cannot be applied safely, such as closing the books of a multi-branch company, the system refuses rather than guesses." },
    ],
  },
  faq: {
    title: "Quick answers",
    intro: "The five questions we hear first.",
    all: "Read all questions and answers",
    items: [
      { q: "What is QuincaiSoft?", a: "QuincaiSoft is an ERP and point of sale for retail, trade and distribution businesses, developed by Opesware Technologies. It covers catalogue, stock, quotations, purchasing, sales, cash and business documents, in FCFA and in English and French." },
      { q: "Who is QuincaiSoft for?", a: "Hardware shops, construction-material retailers and wholesalers or distributors in Cameroon and the CEMAC region, including businesses with several branches." },
      { q: "Does it support MTN and Orange Mobile Money?", a: "Partly. An MTN adapter exists but live provider credentials and acceptance are still required. Orange Money is not integrated yet. A payment is not shown as verified until the provider confirms it." },
      { q: "Does it work offline?", a: "The mobile app is designed for weak connectivity and released in stages. Offline posting of sales is currently disabled until stock quotas and device evidence are complete." },
      { q: "How much does it cost, and is it ready for production?", a: "We do not publish prices; contact us, as the cost depends on branches, users and modules. QuincaiSoft is in active development and no production certification is claimed, so we discuss pilots." },
    ],
  },
  cta: {
    title: "Talk to us about your shop or warehouse",
    body: "Tell us your type of business, the number of branches and what you want to improve. We reply by email, in English or French.",
    primary: "Request a demo",
    secondary: "Ask about pricing",
  },
  itemListName: "QuincaiSoft features",
};

const fr: HomeContent = {
  meta: {
    title: "QuincaiSoft | ERP et caisse pour quincailleries, Cameroun",
    description: "ERP et point de vente bilingue en FCFA pour quincailleries, matériaux de construction et grossistes au Cameroun : stock, devis, achats, caisse.",
  },
  hero: {
    eyebrow: "ERP et point de vente pour le négoce au Cameroun et en CEMAC",
    h1: "Maîtrisez stock, ventes et caisse, agence par agence",
    definition: "QuincaiSoft est un ERP et point de vente bilingue, en FCFA (XAF), pour les quincailleries, les vendeurs de matériaux de construction et les grossistes du Cameroun et de la zone CEMAC. Il relie le comptoir, l'entrepôt, les fournisseurs et la caisse dans un seul produit.",
    lead: "Vendez au sac, à la barre, au mètre ou à la tonne. Établissez des devis pour les entrepreneurs, encaissez des acomptes, réceptionnez les livraisons et clôturez la journée avec une caisse qui concorde.",
    ctaDemo: "Demander une démo",
    ctaFeatures: "Voir les fonctionnalités",
    status: "En développement actif, des pilotes sont en discussion. Plus bas, nous disons clairement ce qui est terminé et ce qui ne l'est pas.",
    screen: {
      caption: "Illustration d'un écran de vente au comptoir. Les articles et montants sont des exemples, pas des données ni des clients réels.",
      label: "Exemple illustratif",
      title: "Vente au comptoir",
      branch: "Agence : Magasin principal",
      headers: ["Article", "Unité", "Qté", "Montant (FCFA)"],
      lines: [
        ["Ciment, sac de 50 kg", "sac", "20", "130 000"],
        ["Fer à béton, 12 mm", "barre", "30", "165 000"],
        ["Tôle de toiture", "pièce", "12", "96 000"],
        ["Fil d'attache", "kg", "2,5", "4 500"],
      ],
      totalLabel: "Total",
      total: "395 500 FCFA",
      tenders: ["Espèces", "Acompte", "Crédit client"],
      stockNote: "Le stock de l'agence est mis à jour à la validation de la vente.",
    },
  },
  facts: {
    title: "L'essentiel, sans détour",
    items: [
      { icon: "Banknote", title: "FCFA (XAF)", body: "Montants, prix et documents sont en FCFA, la devise de travail du produit." },
      { icon: "Languages", title: "Français et anglais", body: "L'interface et les documents commerciaux sont conçus pour être bilingues." },
      { icon: "Building2", title: "Plusieurs agences", body: "Stock, ventes et caisse sont organisés par agence et par entrepôt." },
      { icon: "Smartphone", title: "Application mobile dans chaque offre", body: "Chaque offre commerciale inclut l'application mobile. Les permissions définissent ce que chacun peut faire." },
      { icon: "WifiOff", title: "Pensé pour une connexion faible", body: "Le client mobile est conçu pour les réseaux médiocres et livré par étapes, avec des limites expliquées plus bas." },
      { icon: "ShieldCheck", title: "Statut honnête", body: "Aucun agrément fiscal ni certification n'est revendiqué, et aucun chiffre client n'est inventé." },
    ],
  },
  problems: {
    title: "Les problèmes que nous voulons résoudre",
    intro: "Le négoce de quincaillerie et de gros a ses difficultés quotidiennes. QuincaiSoft est conçu autour de celles-ci.",
    fixLabel: "Ce que fait QuincaiSoft",
    items: [
      { icon: "Boxes", pain: "Le stock en rayon ne correspond jamais aux écritures", fix: "Chaque mouvement de stock est enregistré et conservé, et les inventaires montrent d'où viennent les écarts.", feature: "Stock et historique des mouvements" },
      { icon: "Scale", pain: "Un produit, plusieurs unités", fix: "Vendez le ciment au sac, le fer à la barre ou à la tonne, le fil au kilo, avec des quantités fractionnaires et un stock cohérent.", feature: "Unités et quantités fractionnaires" },
      { icon: "ClipboardList", pain: "Les entrepreneurs veulent d'abord un devis", fix: "Préparez un devis, suivez-le et transformez-le en vente une fois accepté.", feature: "Devis" },
      { icon: "HandCoins", pain: "Acomptes et crédits suivis sur papier", fix: "Enregistrez acomptes, paiements échelonnés et crédits, avec un solde clair par client.", feature: "Comptes clients et acomptes" },
      { icon: "Truck", pain: "Livraisons et dettes fournisseurs qui se perdent", fix: "Enregistrez commandes et réceptions pour garder achats, stock et dettes reliés.", feature: "Achats et fournisseurs" },
      { icon: "Wallet", pain: "La caisse ne concorde pas en fin de journée", fix: "Ouvrez et clôturez les sessions de caisse et saisissez les dépenses pour suivre l'argent de la caisse jusqu'aux écritures.", feature: "Sessions de caisse et dépenses" },
    ],
  },
  features: {
    title: "Tout ce qu'il faut au comptoir et au bureau",
    intro: "Un seul produit pour le comptoir, l'entrepôt et le bureau. Voici le périmètre visé ; les notes de statut disent ce qui reste en cours.",
    listName: "Fonctionnalités de QuincaiSoft",
    more: "Voir toutes les fonctionnalités et leur statut",
    items: [
      { icon: "ShoppingCart", title: "Ventes et point de vente", body: "Ventes au comptoir en espèces, paiements fractionnés, crédit client et acomptes, avec retours et remboursements." },
      { icon: "ScanBarcode", title: "Catalogue et codes-barres", body: "Produits, catégories, codes-barres et listes de prix pour clients de passage et clients réguliers." },
      { icon: "Package", title: "Stock, unités et fractions", body: "Stock par agence et entrepôt, plusieurs unités par produit, quantités fractionnaires, inventaires et réservations." },
      { icon: "Truck", title: "Achats et fournisseurs", body: "Commandes fournisseurs, réceptions et soldes fournisseurs reliés au stock." },
      { icon: "HandCoins", title: "Crédit client et acomptes", body: "Comptes clients, acomptes et paiements échelonnés avec des soldes clairs." },
      { icon: "ClipboardList", title: "Devis", body: "Devis pour les entrepreneurs, suivis jusqu'à la vente." },
      { icon: "Wallet", title: "Caisse et dépenses", body: "Sessions de caisse, dépenses et rapprochement pour suivre l'argent de la caisse jusqu'aux écritures." },
      { icon: "ReceiptText", title: "Documents et reçus", body: "Reçus et documents en français ou en anglais, en FCFA, avec aperçu et marquage des réimpressions." },
      { icon: "BarChart3", title: "Rapports", body: "Rapports opérationnels sur le stock, les ventes, les achats et la caisse, toutes agences." },
      { icon: "Smartphone", title: "Mobile money : statut", body: "L'adaptateur MTN existe et attend la validation en réel. Orange Money n'est pas encore intégré. Détails plus bas." },
    ],
  },
  how: {
    title: "Comment ça marche",
    intro: "De la première discussion à l'usage quotidien, en quatre étapes.",
    steps: [
      { title: "Parlez-nous de votre activité", body: "Indiquez votre type d'activité, le nombre d'agences et ce que vous voulez améliorer. Nous répondons par e-mail, en français ou en anglais." },
      { title: "Voyez-le sur vos propres cas", body: "Nous faisons une démonstration autour de vos produits, unités, devis et de votre routine de caisse." },
      { title: "Paramétrez catalogue et agences", body: "Produits, unités, prix, agences, entrepôts et rôles des utilisateurs sont préparés avec vous." },
      { title: "Vendez, réceptionnez, rapprochez", body: "Tenez le comptoir, réceptionnez les livraisons et clôturez chaque jour avec un stock et une caisse vérifiables." },
    ],
  },
  industries: {
    title: "Conçu pour le négoce et la distribution",
    intro: "Des entreprises qui vendent beaucoup de produits en plusieurs unités, servent clients de passage et professionnels, et gèrent souvent plusieurs sites.",
    icons: { "hardware-shops": "Hammer", "construction-materials": "HardHat", "wholesale-distribution": "Warehouse" },
    link: "En savoir plus",
  },
  mobile: {
    title: "Mobile et hors ligne, avec des limites claires",
    intro: "La même application mobile QuincaiSoft sert propriétaires, gérants, caissiers, magasiniers et livreurs, et ne montre à chacun que ce que ses permissions autorisent. Elle est conçue pour une connexion faible et livrée par étapes.",
    worksTitle: "À quoi sert l'application mobile",
    works: [
      "Propriétaires : tableau de bord, synthèses par agence, validations et alertes.",
      "Caissiers : point de vente, sessions de caisse, reçus et recherche de produits.",
      "Magasiniers : inventaires, réceptions, transferts et lecture de codes-barres.",
      "Livreurs : enlèvements, bordereaux et remise.",
    ],
    stagesTitle: "Étapes de livraison",
    stages: [
      "Étape 1 : usage en ligne avec un catalogue en cache, en lecture seule.",
      "Étape 2 : inventaires hors ligne.",
      "Étape 3 : ventes en espèces hors ligne, sous contrôle.",
    ],
    limitsTitle: "Limites, sans détour",
    limits: [
      "L'enregistrement des ventes hors ligne est actuellement désactivé. Il le restera tant que les quotas de stock et les preuves sur appareil ne sont pas complets.",
      "Un téléphone sans connexion ne peut pas afficher de soldes en direct ni de ventes ou mouvements de stock définitivement enregistrés.",
      "Chaque étape est livrée quand les preuves sont réunies, pas à une date de calendrier.",
    ],
    phone: {
      title: "Application mobile",
      rows: [["Catalogue (cache en lecture seule)", "En ligne"], ["Inventaires", "Prévu, hors ligne"], ["Ventes hors ligne", "Désactivé"]],
      note: "Illustration du statut de livraison, pas une capture d'écran.",
    },
  },
  payments: {
    title: "Paiements et mobile money : la situation actuelle",
    intro: "Le mobile money compte au Cameroun, voici donc précisément où en est chaque opérateur. Un paiement n'est pas affiché comme vérifié tant que l'opérateur ne l'a pas confirmé.",
    items: [
      { name: "MTN Mobile Money", status: "Adaptateur construit, validation en réel en attente", body: "Un adaptateur durable d'intention de paiement et de rapprochement existe. Les identifiants réels, la validation et la vérification par l'opérateur restent nécessaires." },
      { name: "Orange Money", status: "Pas encore intégré", body: "En attente d'une intégration contractuelle. Il n'est pas marqué comme intégré." },
      { name: "Références manuelles", status: "Enregistrées, jamais marquées vérifiées", body: "Vous pouvez saisir au comptoir une référence de mobile money, de virement ou d'espèces. Une référence saisie à la main n'est jamais affichée comme vérifiée." },
    ],
    more: "Lire le statut complet et les limites",
  },
  security: {
    title: "Sécurité et fiabilité vérifiables",
    intro: "Ce sont des règles de conception issues de notre spécification d'architecture. Ce ne sont pas des certifications.",
    disclaimer: "Aucune certification de sécurité, aucun agrément fiscal DGI ni conformité OHADA n'est revendiqué.",
    items: [
      { icon: "Lock", title: "Isolation des clients", body: "Les entreprises partagent la plateforme, pas les données : chaque enregistrement porte un identifiant de locataire appliqué dans la base par la sécurité au niveau des lignes." },
      { icon: "History", title: "Piste d'audit", body: "Les actions sensibles sont tracées avec leur auteur, et les mouvements de stock sont un historique non modifiable." },
      { icon: "Repeat", title: "Enregistrements idempotents", body: "Une requête renvoyée n'enregistre pas deux fois la même vente ou le même mouvement, grâce aux clés d'idempotence." },
      { icon: "KeyRound", title: "Rôles et permissions", body: "Des rôles par utilisateur et des permissions par agence décident qui voit et fait quoi." },
      { icon: "ShieldCheck", title: "Refus par défaut", body: "Quand une règle ne peut pas être appliquée en sécurité, comme la clôture comptable d'une entreprise multi-agences, le système refuse plutôt que de deviner." },
    ],
  },
  faq: {
    title: "Réponses rapides",
    intro: "Les cinq questions qu'on nous pose en premier.",
    all: "Lire toutes les questions et réponses",
    items: [
      { q: "Qu'est-ce que QuincaiSoft ?", a: "QuincaiSoft est un ERP et point de vente pour le commerce, le négoce et la distribution, développé par Opesware Technologies. Il couvre catalogue, stock, devis, achats, ventes, caisse et documents commerciaux, en FCFA, en français et en anglais." },
      { q: "À qui s'adresse QuincaiSoft ?", a: "Aux quincailleries, aux vendeurs de matériaux de construction et aux grossistes ou distributeurs du Cameroun et de la zone CEMAC, y compris les entreprises à plusieurs agences." },
      { q: "Gère-t-il MTN et Orange Mobile Money ?", a: "En partie. Un adaptateur MTN existe, mais les identifiants réels et la validation par l'opérateur restent nécessaires. Orange Money n'est pas encore intégré. Un paiement n'est pas affiché comme vérifié tant que l'opérateur ne l'a pas confirmé." },
      { q: "Fonctionne-t-il hors ligne ?", a: "L'application mobile est conçue pour une connexion faible et livrée par étapes. L'enregistrement des ventes hors ligne est actuellement désactivé, en attendant les quotas de stock et les preuves sur appareil." },
      { q: "Combien ça coûte, et est-il prêt pour la production ?", a: "Nous ne publions pas de prix ; contactez-nous, car le coût dépend des agences, des utilisateurs et des modules. QuincaiSoft est en développement actif et aucune certification de production n'est revendiquée : nous discutons de pilotes." },
    ],
  },
  cta: {
    title: "Parlons de votre magasin ou de votre entrepôt",
    body: "Indiquez votre type d'activité, le nombre d'agences et ce que vous voulez améliorer. Nous répondons par e-mail, en français ou en anglais.",
    primary: "Demander une démo",
    secondary: "Demander les tarifs",
  },
  itemListName: "Fonctionnalités de QuincaiSoft",
};

export function homeContent(lang: Lang): HomeContent {
  return lang === "fr" ? fr : en;
}
