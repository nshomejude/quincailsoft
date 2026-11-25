import type { IndustrySlug, Lang } from "./site";

export type Feature = { title: string; body: string };
export type Faq = { q: string; a: string };
export type Industry = {
  name: string;
  summary: string;
  metaDescription: string;
  intro: string;
  needs: Feature[];
};

export type Dict = {
  nav: { home: string; features: string; industries: string; pricing: string; about: string; contact: string; faq: string; privacy: string; terms: string };
  ui: {
    skip: string; menu: string; language: string; switchTo: string; breadcrumb: string; home: string; cta: string; ctaSecondary: string;
    footerTagline: string; footerLegal: string; draft: string; learnMore: string; statusTitle: string;
  };
  home: {
    title: string; description: string; h1: string; lead: string;
    valueTitle: string; value: Feature[];
    industriesTitle: string; honestyTitle: string; honesty: string[];
    ctaTitle: string; ctaBody: string;
  };
  features: { title: string; description: string; h1: string; lead: string; groups: Feature[]; statusTitle: string; status: Feature[] };
  industries: { title: string; description: string; h1: string; lead: string; needsTitle: string; items: Record<IndustrySlug, Industry> };
  pricing: {
    title: string; description: string; h1: string; lead: string; contactForPricing: string; body: string[];
    formTitle: string; name: string; email: string; company: string; phone: string; branches: string; message: string; submit: string; todo: string; note: string;
  };
  about: { title: string; description: string; h1: string; lead: string; sections: Feature[] };
  contact: { title: string; description: string; h1: string; lead: string; emailLabel: string; body: string };
  faq: { title: string; description: string; h1: string; lead: string; items: Faq[] };
  privacy: { title: string; description: string; h1: string; draftNotice: string; sections: Feature[] };
  terms: { title: string; description: string; h1: string; draftNotice: string; sections: Feature[] };
  notFound: { title: string; body: string; back: string };
  softwareDescription: string;
};

const en: Dict = {
  nav: { home: "Home", features: "Features", industries: "Industries", pricing: "Pricing", about: "About", contact: "Contact", faq: "FAQ", privacy: "Privacy", terms: "Terms" },
  ui: {
    skip: "Skip to main content", menu: "Menu", language: "Language", switchTo: "Français", breadcrumb: "Breadcrumb", home: "Home",
    cta: "Talk to us", ctaSecondary: "See features",
    footerTagline: "Retail, trade and distribution ERP and point of sale for Cameroon and the CEMAC region.",
    footerLegal: "QuincaiSoft is a product of Opesware Technologies.", draft: "Draft", learnMore: "Learn more",
    statusTitle: "Current status",
  },
  home: {
    title: "QuincaiSoft | ERP and POS for hardware shops and distributors in Cameroon",
    description: "QuincaiSoft by Opesware Technologies is a bilingual ERP and point of sale for hardware shops, construction-material retailers and wholesalers in Cameroon and CEMAC: stock, quotations, purchasing, cash and FCFA documents.",
    h1: "One system for stock, sales and cash in your hardware or trade business",
    lead: "QuincaiSoft is an ERP and point of sale built for hardware shops, construction-material retailers and wholesalers in Cameroon and the CEMAC region. It works in FCFA (XAF), in English and French, across several branches.",
    valueTitle: "What it covers",
    value: [
      { title: "Stock across branches", body: "Track products, units, barcodes and stock per branch and warehouse, with stock counts and an auditable movement history." },
      { title: "Quotations to invoices", body: "Prepare quotations for contractors and trade customers, convert them to sales, and manage customer accounts, deposits and returns." },
      { title: "Purchasing and suppliers", body: "Record supplier orders and receipts so that what you buy and what is on the shelf stay reconciled." },
      { title: "Cash and payments", body: "Cash shifts, expenses, split payments and customer credit, with mobile money payments recorded under strict rules." },
      { title: "Bilingual documents", body: "Quotations, receipts and other business documents in English or French, in FCFA." },
      { title: "Built for local conditions", body: "The mobile client is designed for weak connectivity; offline selling is released only in stages, as described under Current status." },
    ],
    industriesTitle: "Who it is for",
    honestyTitle: "What we state plainly",
    honesty: [
      "QuincaiSoft is in active development. Not every module and workflow is complete, and we do not claim production certification.",
      "MTN Mobile Money has a payment adapter that still needs live credentials and provider acceptance. Orange Money is not integrated yet; it is pending a contracted integration.",
      "We do not claim DGI-approved fiscal invoicing. Fiscal rules are configurable, and approvals must come from the competent authorities and your accountants.",
    ],
    ctaTitle: "Interested? Tell us about your business",
    ctaBody: "We are talking with hardware shops, building-material retailers and wholesalers about pilots. Send us a message and we will reply by email.",
  },
  features: {
    title: "Features | QuincaiSoft ERP and POS",
    description: "Catalog, multi-branch stock, quotations, purchasing, point of sale, cash, bilingual documents and reports in QuincaiSoft, with an honest view of what is complete and what is still in progress.",
    h1: "Features",
    lead: "QuincaiSoft combines the back office and the counter in one product. The list below describes the intended scope; the status section says what is still in progress.",
    groups: [
      { title: "Catalog and pricing", body: "Products, categories, units of measure, barcodes and price lists suited to selling by the piece, by the bag, by the metre or by the tonne." },
      { title: "Multi-branch stock", body: "Stock per branch and warehouse, immutable stock movements, stock counts and reservations, so each branch sees reliable availability." },
      { title: "Quotations", body: "Quotations for contractors and trade customers that can be followed through to sale, with return and refund handling." },
      { title: "Purchasing and suppliers", body: "Supplier orders, goods receipts and supplier balances connected to stock." },
      { title: "Point of sale", body: "Counter sales with cash, bank transfer references, mobile money references, split payments, customer credit and deposits." },
      { title: "Cash management", body: "Cash shifts, expenses and reconciliation to follow money from the till to the books." },
      { title: "Documents and printing", body: "Business documents and receipts in English or French, in FCFA, with preview, reprint marking and printer profiles." },
      { title: "Reports", body: "Operational reports for stock, sales, purchasing and cash across branches." },
      { title: "Roles and audit", body: "Per-user roles and permissions with an audit trail of sensitive actions." },
      { title: "Bilingual, FCFA", body: "Interface and documents in English and French, with FCFA (XAF) as the working currency." },
    ],
    statusTitle: "Status and limits",
    status: [
      { title: "Development stage", body: "QuincaiSoft is being built module by module. Several domain slices exist, but not every workflow is complete and no production release certification is claimed." },
      { title: "MTN Mobile Money", body: "A payment intent and reconciliation adapter exists. Live provider credentials and acceptance are still required. Until a payment is confirmed by the provider, it is shown as unverified." },
      { title: "Orange Money", body: "Not available yet. Orange Money is pending a contracted integration and is not marked as integrated. Manual reference recording is possible but is never labelled as verified." },
      { title: "Offline mobile", body: "The mobile client is being released in stages: first online with a read-only cached catalogue, then offline stock counts, then controlled offline cash sales. Offline posting of sales is currently disabled until stock-quota and device evidence are complete." },
      { title: "Fiscal and legal", body: "No DGI fiscal approval, OHADA conformity or other certification is claimed. Fiscal rules are configurable and need approval from the competent authorities and your accountants." },
      { title: "Windows desktop", body: "A desktop bridge for existing Windows installations is planned. Its integration is pending, and existing Windows software keeps working independently of the cloud." },
    ],
  },
  industries: {
    title: "Industries | QuincaiSoft",
    description: "How QuincaiSoft supports hardware shops, construction-material retailers and wholesale distributors in Cameroon and CEMAC.",
    h1: "Industries",
    lead: "QuincaiSoft is designed for businesses that sell many products in many units, serve both walk-in and trade customers, and often run more than one location.",
    needsTitle: "What matters in this business",
    items: {
      "hardware-shops": {
        name: "Hardware shops",
        summary: "Large catalogues, fast counter sales and tight control of small stock.",
        metaDescription: "QuincaiSoft for hardware shops in Cameroon: barcodes, units, fast point of sale, stock counts and cash control in FCFA.",
        intro: "A hardware shop may carry thousands of small items. QuincaiSoft is designed to keep the catalogue organised, the counter quick and the stock honest.",
        needs: [
          { title: "Big catalogue, small margins", body: "Products with barcodes, categories and multiple units, and price lists for walk-in and regular customers." },
          { title: "Fast counter", body: "Point of sale with cash and split payments, with receipts in English or French." },
          { title: "Stock accuracy", body: "Stock counts and movement history to find where differences arise." },
          { title: "End-of-day cash", body: "Cash shifts and expenses so the till can be reconciled each day." },
        ],
      },
      "construction-materials": {
        name: "Construction materials",
        summary: "Quotations, deposits and customer accounts for contractors and project buyers.",
        metaDescription: "QuincaiSoft for construction-material retailers in Cameroon: quotations, deposits, customer accounts, bulk units and multi-branch stock.",
        intro: "Cement, iron bars, roofing and tiles are often sold against a quotation, a deposit or an account. QuincaiSoft is designed around that way of selling.",
        needs: [
          { title: "Quotations", body: "Prepare quotations, follow them and convert accepted ones into sales." },
          { title: "Deposits and accounts", body: "Customer deposits, staged payments and customer credit with clear balances." },
          { title: "Bulk and varied units", body: "Selling by bag, piece, bar, metre or tonne with consistent stock keeping." },
          { title: "Several yards", body: "Stock per branch and warehouse, visible together to the right people." },
        ],
      },
      "wholesale-distribution": {
        name: "Wholesale and distribution",
        summary: "Purchasing, supplier balances and stock across branches and warehouses.",
        metaDescription: "QuincaiSoft for wholesalers and distributors in Cameroon and CEMAC: purchasing, supplier balances, multi-branch stock, price lists and reports.",
        intro: "Wholesalers live on purchasing discipline and stock visibility. QuincaiSoft connects supplier orders and receipts to stock and to sales.",
        needs: [
          { title: "Purchasing", body: "Supplier orders, receipts and supplier balances connected to stock." },
          { title: "Multi-branch visibility", body: "Availability per branch and warehouse, with reservations to avoid overselling." },
          { title: "Trade pricing", body: "Price lists for different customer groups, and customer accounts." },
          { title: "Reporting", body: "Reports on stock, sales, purchases and cash across locations." },
        ],
      },
    },
  },
  pricing: {
    title: "Pricing | QuincaiSoft",
    description: "QuincaiSoft pricing is tailored to your business. Contact us for pricing.",
    h1: "Pricing",
    lead: "We have not published a price list. Contact us for pricing.",
    contactForPricing: "Contact us for pricing",
    body: [
      "Pricing depends on the number of branches and users, the modules you need and how you deploy. Tell us about your business and we will reply with a proposal.",
      "We do not publish prices, free trials or discounts that are not yet defined.",
    ],
    formTitle: "Request a quote",
    name: "Your name", email: "Email", company: "Company", phone: "Phone (optional)", branches: "Number of branches", message: "Tell us about your business",
    submit: "Send by email",
    todo: "TODO: this form opens your email application (mailto). Replace it with a server-side form handler and confirm the receiving inbox before launch.",
    note: "Submitting opens your email application with the message prepared. Nothing is stored by this website.",
  },
  about: {
    title: "About | QuincaiSoft by Opesware Technologies",
    description: "QuincaiSoft is developed by Opesware Technologies for retail, trade and distribution businesses in Cameroon and the CEMAC region.",
    h1: "About QuincaiSoft",
    lead: "QuincaiSoft is a product of Opesware Technologies, built for retail, trade and distribution businesses in Cameroon and the CEMAC region.",
    sections: [
      { title: "Why we are building it", body: "Hardware shops, construction-material retailers and wholesalers handle many products, units and customers across branches. We are building an ERP and point of sale that fits this way of working, in English and French, in FCFA." },
      { title: "How we work", body: "We build in stages and release a capability only when it has evidence behind it. This is why we say plainly what is complete and what is still in progress." },
      { title: "What we do not claim", body: "We do not claim DGI fiscal approval, certifications, customers, or figures that we cannot demonstrate." },
    ],
  },
  contact: {
    title: "Contact | QuincaiSoft",
    description: "Contact Opesware Technologies about QuincaiSoft: pilots, demonstrations and pricing for businesses in Cameroon and CEMAC.",
    h1: "Contact us",
    lead: "Ask about a demonstration, a pilot or pricing. We answer in English or French.",
    emailLabel: "Email",
    body: "Write to us with your business type, the number of branches and what you want to improve. The form below opens your email application.",
  },
  faq: {
    title: "FAQ | QuincaiSoft",
    description: "Answers about QuincaiSoft: what it is, who it is for, currencies, mobile money, offline use, fiscal compliance, pricing and availability.",
    h1: "Frequently asked questions",
    lead: "Short, direct answers about QuincaiSoft.",
    items: [
      { q: "What is QuincaiSoft?", a: "QuincaiSoft is an ERP and point of sale for retail, trade and distribution businesses, developed by Opesware Technologies. It covers catalogue, stock, quotations, purchasing, sales, cash and business documents." },
      { q: "Who is QuincaiSoft for?", a: "It is designed for hardware shops, construction-material retailers and wholesalers or distributors in Cameroon and the CEMAC region, including businesses with several branches." },
      { q: "Does it work in FCFA?", a: "Yes. FCFA (XAF) is the working currency, and amounts and documents are presented accordingly." },
      { q: "Is it available in French and English?", a: "Yes. The interface and business documents are designed to be bilingual, English and French." },
      { q: "Can it manage several branches and warehouses?", a: "Yes. Stock, sales and cash are organised per branch and warehouse, with consolidated reporting for authorised users." },
      { q: "Does it support MTN Mobile Money?", a: "Partly. An MTN payment intent and reconciliation adapter exists, but live provider credentials and acceptance are still required. A payment is not shown as verified until the provider confirms it." },
      { q: "Does it support Orange Money?", a: "Not yet. Orange Money is pending a contracted integration and is not marked as integrated. References can be recorded manually, but they are never labelled as verified." },
      { q: "Does it work offline?", a: "The mobile client is designed for weak connectivity and is released in stages: online with a read-only cached catalogue first, then offline stock counts, then controlled offline cash sales. Offline posting of sales is currently disabled until the required quota and device evidence is complete." },
      { q: "Is QuincaiSoft approved by the DGI for fiscal invoicing?", a: "No approval is claimed. Fiscal rules are configurable, but approval must come from the competent authorities and your accountants." },
      { q: "Does it have OHADA, ISO or other certifications?", a: "No certification is claimed. Compliance designations require review by counsel and accountants and have not been obtained." },
      { q: "Can I prepare quotations?", a: "Yes. Quotations are part of the product, including follow-up and conversion to sales, together with customer accounts, deposits and returns." },
      { q: "Does it handle purchasing and suppliers?", a: "Yes. Supplier orders, goods receipts and supplier balances are connected to stock." },
      { q: "How much does it cost?", a: "We have not published prices. Contact us for pricing; it depends on the number of branches, users and modules." },
      { q: "Is QuincaiSoft ready for production use?", a: "It is in active development. Several modules exist, but not every workflow is complete and no production release certification is claimed. Contact us to discuss a pilot." },
      { q: "Can it connect to my existing Windows software?", a: "A desktop bridge for existing Windows installations is planned but its integration is pending. Existing Windows software continues to work on its own." },
      { q: "Where can I get a demonstration?", a: "Use the contact page to request a demonstration or a pilot discussion. We reply by email in English or French." },
    ],
  },
  privacy: {
    title: "Privacy notice (draft) | QuincaiSoft",
    description: "Draft privacy notice for the QuincaiSoft marketing website. Requires legal review.",
    h1: "Privacy notice",
    draftNotice: "DRAFT: this text has not been reviewed by counsel and must be reviewed and approved before it is relied upon.",
    sections: [
      { title: "Scope", body: "This notice concerns the QuincaiSoft marketing website operated by Opesware Technologies. It does not cover the QuincaiSoft product, which will be governed by its own terms and data processing agreements." },
      { title: "Information we collect", body: "This website does not set advertising cookies and does not store form submissions. When you use the contact form, your email application opens with a message you can edit and send; we receive the information you choose to send." },
      { title: "How we use it", body: "We use messages sent to us to reply to your request about demonstrations, pilots and pricing." },
      { title: "Your rights", body: "TODO (legal): describe applicable data protection rights and the retention period under Cameroonian law, and name the data controller and contact." },
      { title: "Changes", body: "TODO (legal): describe how changes to this notice are announced." },
    ],
  },
  terms: {
    title: "Terms of use (draft) | QuincaiSoft",
    description: "Draft terms of use for the QuincaiSoft marketing website. Requires legal review.",
    h1: "Terms of use",
    draftNotice: "DRAFT: this text has not been reviewed by counsel and must be reviewed and approved before it is relied upon.",
    sections: [
      { title: "Purpose of this website", body: "This website presents QuincaiSoft for information only. It is not an offer, a contract or a commitment about availability, features or price." },
      { title: "Accuracy", body: "We describe the product's intended scope and its current status as accurately as we can. Features can change, and some are in development." },
      { title: "No certification claim", body: "Nothing on this website states or implies fiscal, legal or regulatory approval of the product." },
      { title: "Intellectual property", body: "TODO (legal): trademark and copyright notice for QuincaiSoft and Opesware Technologies." },
      { title: "Liability and governing law", body: "TODO (legal): limitation of liability, governing law and jurisdiction." },
    ],
  },
  notFound: { title: "Page not found", body: "The page you are looking for does not exist or has moved.", back: "Go to the home page" },
  softwareDescription: "ERP and point of sale for hardware shops, construction-material retailers and wholesalers in Cameroon and CEMAC: multi-branch stock, quotations, purchasing, cash and bilingual documents in FCFA.",
};

const fr: Dict = {
  nav: { home: "Accueil", features: "Fonctionnalités", industries: "Secteurs", pricing: "Tarifs", about: "À propos", contact: "Contact", faq: "FAQ", privacy: "Confidentialité", terms: "Conditions" },
  ui: {
    skip: "Aller au contenu principal", menu: "Menu", language: "Langue", switchTo: "English", breadcrumb: "Fil d'Ariane", home: "Accueil",
    cta: "Contactez-nous", ctaSecondary: "Voir les fonctionnalités",
    footerTagline: "ERP et point de vente pour le commerce, la quincaillerie et la distribution au Cameroun et en zone CEMAC.",
    footerLegal: "QuincaiSoft est un produit d'Opesware Technologies.", draft: "Brouillon", learnMore: "En savoir plus",
    statusTitle: "État actuel",
  },
  home: {
    title: "QuincaiSoft | ERP et caisse pour quincailleries et distributeurs au Cameroun",
    description: "QuincaiSoft, d'Opesware Technologies, est un ERP et point de vente bilingue pour quincailleries, vendeurs de matériaux de construction et grossistes au Cameroun et en zone CEMAC : stock, devis, achats, caisse et documents en FCFA.",
    h1: "Un seul système pour le stock, les ventes et la caisse de votre quincaillerie ou commerce",
    lead: "QuincaiSoft est un ERP et point de vente conçu pour les quincailleries, les vendeurs de matériaux de construction et les grossistes du Cameroun et de la zone CEMAC. Il fonctionne en FCFA (XAF), en français et en anglais, sur plusieurs succursales.",
    valueTitle: "Ce qu'il couvre",
    value: [
      { title: "Stock multi-succursales", body: "Suivez produits, unités, codes-barres et stock par succursale et par entrepôt, avec inventaires et historique de mouvements vérifiable." },
      { title: "Du devis à la facture", body: "Préparez des devis pour les entrepreneurs et clients professionnels, transformez-les en ventes, gérez comptes clients, acomptes et retours." },
      { title: "Achats et fournisseurs", body: "Enregistrez commandes et réceptions fournisseurs pour que ce que vous achetez et ce qui est en rayon restent rapprochés." },
      { title: "Caisse et paiements", body: "Sessions de caisse, dépenses, paiements mixtes et crédit client, avec les paiements mobile money enregistrés selon des règles strictes." },
      { title: "Documents bilingues", body: "Devis, reçus et autres documents commerciaux en français ou en anglais, en FCFA." },
      { title: "Pensé pour le terrain", body: "Le client mobile est conçu pour une connexion fragile ; la vente hors ligne n'est ouverte que par étapes, comme décrit dans « État actuel »." },
    ],
    industriesTitle: "Pour qui",
    honestyTitle: "Ce que nous disons clairement",
    honesty: [
      "QuincaiSoft est en développement actif. Tous les modules et flux ne sont pas terminés, et nous ne revendiquons aucune certification de mise en production.",
      "MTN Mobile Money dispose d'un adaptateur de paiement qui nécessite encore des identifiants réels et la validation du fournisseur. Orange Money n'est pas encore intégré : il attend une intégration contractuelle.",
      "Nous ne revendiquons pas d'agrément fiscal de la DGI. Les règles fiscales sont configurables ; les approbations relèvent des autorités compétentes et de vos comptables.",
    ],
    ctaTitle: "Intéressé ? Parlez-nous de votre activité",
    ctaBody: "Nous échangeons avec des quincailleries, des vendeurs de matériaux et des grossistes au sujet de pilotes. Envoyez-nous un message, nous répondrons par e-mail.",
  },
  features: {
    title: "Fonctionnalités | QuincaiSoft ERP et caisse",
    description: "Catalogue, stock multi-succursales, devis, achats, point de vente, caisse, documents bilingues et rapports dans QuincaiSoft, avec une vision honnête de ce qui est terminé et de ce qui est en cours.",
    h1: "Fonctionnalités",
    lead: "QuincaiSoft réunit le back-office et le comptoir dans un seul produit. La liste décrit le périmètre visé ; la section sur l'état dit ce qui est encore en cours.",
    groups: [
      { title: "Catalogue et prix", body: "Produits, catégories, unités de mesure, codes-barres et listes de prix adaptés à la vente à la pièce, au sac, au mètre ou à la tonne." },
      { title: "Stock multi-succursales", body: "Stock par succursale et entrepôt, mouvements immuables, inventaires et réservations, pour une disponibilité fiable dans chaque succursale." },
      { title: "Devis", body: "Devis pour entrepreneurs et clients professionnels, suivis jusqu'à la vente, avec gestion des retours et remboursements." },
      { title: "Achats et fournisseurs", body: "Commandes fournisseurs, réceptions et soldes fournisseurs reliés au stock." },
      { title: "Point de vente", body: "Ventes au comptoir en espèces, références de virement, références mobile money, paiements mixtes, crédit client et acomptes." },
      { title: "Gestion de caisse", body: "Sessions de caisse, dépenses et rapprochement pour suivre l'argent du tiroir-caisse jusqu'aux écritures." },
      { title: "Documents et impression", body: "Documents commerciaux et reçus en français ou en anglais, en FCFA, avec aperçu, marquage des réimpressions et profils d'imprimante." },
      { title: "Rapports", body: "Rapports opérationnels sur le stock, les ventes, les achats et la caisse, toutes succursales confondues." },
      { title: "Rôles et audit", body: "Rôles et permissions par utilisateur avec piste d'audit des actions sensibles." },
      { title: "Bilingue, FCFA", body: "Interface et documents en français et en anglais, avec le FCFA (XAF) comme monnaie de travail." },
    ],
    statusTitle: "État et limites",
    status: [
      { title: "Stade de développement", body: "QuincaiSoft est construit module par module. Plusieurs tranches fonctionnelles existent, mais tous les flux ne sont pas terminés et aucune certification de mise en production n'est revendiquée." },
      { title: "MTN Mobile Money", body: "Un adaptateur d'intention de paiement et de rapprochement existe. Les identifiants réels et la validation du fournisseur restent nécessaires. Tant que le fournisseur n'a pas confirmé un paiement, il est affiché comme non vérifié." },
      { title: "Orange Money", body: "Pas encore disponible. Orange Money attend une intégration contractuelle et n'est pas marqué comme intégré. La saisie manuelle de références est possible mais n'est jamais présentée comme vérifiée." },
      { title: "Mobile hors ligne", body: "Le client mobile est ouvert par étapes : d'abord en ligne avec un catalogue en cache en lecture seule, puis inventaires hors ligne, puis ventes comptant hors ligne contrôlées. L'enregistrement hors ligne des ventes est actuellement désactivé tant que les quotas de stock et les preuves sur appareil ne sont pas complets." },
      { title: "Fiscal et juridique", body: "Aucun agrément fiscal de la DGI, conformité OHADA ou autre certification n'est revendiqué. Les règles fiscales sont configurables et doivent être approuvées par les autorités compétentes et vos comptables." },
      { title: "Bureau Windows", body: "Une passerelle pour les installations Windows existantes est prévue. Son intégration est en attente, et les logiciels Windows existants continuent de fonctionner indépendamment du cloud." },
    ],
  },
  industries: {
    title: "Secteurs | QuincaiSoft",
    description: "Comment QuincaiSoft accompagne les quincailleries, les vendeurs de matériaux de construction et les grossistes au Cameroun et en zone CEMAC.",
    h1: "Secteurs",
    lead: "QuincaiSoft s'adresse aux entreprises qui vendent de nombreux produits en plusieurs unités, servent à la fois des clients de passage et des professionnels, et exploitent souvent plusieurs sites.",
    needsTitle: "Ce qui compte dans cette activité",
    items: {
      "hardware-shops": {
        name: "Quincailleries",
        summary: "Grands catalogues, ventes rapides au comptoir et contrôle serré du petit stock.",
        metaDescription: "QuincaiSoft pour les quincailleries au Cameroun : codes-barres, unités, point de vente rapide, inventaires et contrôle de caisse en FCFA.",
        intro: "Une quincaillerie peut référencer des milliers de petits articles. QuincaiSoft vise à garder le catalogue organisé, le comptoir rapide et le stock fiable.",
        needs: [
          { title: "Grand catalogue, petites marges", body: "Produits avec codes-barres, catégories et plusieurs unités, et listes de prix pour clients de passage et habituels." },
          { title: "Comptoir rapide", body: "Point de vente avec paiements en espèces et mixtes, reçus en français ou en anglais." },
          { title: "Exactitude du stock", body: "Inventaires et historique de mouvements pour trouver l'origine des écarts." },
          { title: "Caisse de fin de journée", body: "Sessions de caisse et dépenses pour rapprocher le tiroir-caisse chaque jour." },
        ],
      },
      "construction-materials": {
        name: "Matériaux de construction",
        summary: "Devis, acomptes et comptes clients pour entrepreneurs et acheteurs de chantier.",
        metaDescription: "QuincaiSoft pour les vendeurs de matériaux de construction au Cameroun : devis, acomptes, comptes clients, unités en gros et stock multi-succursales.",
        intro: "Ciment, fer à béton, tôles et carreaux se vendent souvent sur devis, avec acompte ou sur compte. QuincaiSoft est conçu autour de cette manière de vendre.",
        needs: [
          { title: "Devis", body: "Préparez et suivez des devis, puis transformez ceux qui sont acceptés en ventes." },
          { title: "Acomptes et comptes", body: "Acomptes clients, paiements échelonnés et crédit client avec des soldes clairs." },
          { title: "Unités en gros et variées", body: "Vente au sac, à la pièce, à la barre, au mètre ou à la tonne avec un suivi de stock cohérent." },
          { title: "Plusieurs dépôts", body: "Stock par succursale et entrepôt, visible globalement par les bonnes personnes." },
        ],
      },
      "wholesale-distribution": {
        name: "Gros et distribution",
        summary: "Achats, soldes fournisseurs et stock entre succursales et entrepôts.",
        metaDescription: "QuincaiSoft pour grossistes et distributeurs au Cameroun et en zone CEMAC : achats, soldes fournisseurs, stock multi-succursales, listes de prix et rapports.",
        intro: "Les grossistes vivent de la discipline d'achat et de la visibilité du stock. QuincaiSoft relie commandes et réceptions fournisseurs au stock et aux ventes.",
        needs: [
          { title: "Achats", body: "Commandes fournisseurs, réceptions et soldes fournisseurs reliés au stock." },
          { title: "Visibilité multi-succursales", body: "Disponibilité par succursale et entrepôt, avec réservations pour éviter la survente." },
          { title: "Prix professionnels", body: "Listes de prix par groupe de clients, et comptes clients." },
          { title: "Rapports", body: "Rapports sur le stock, les ventes, les achats et la caisse, tous sites confondus." },
        ],
      },
    },
  },
  pricing: {
    title: "Tarifs | QuincaiSoft",
    description: "Les tarifs de QuincaiSoft sont adaptés à votre activité. Contactez-nous pour connaître les tarifs.",
    h1: "Tarifs",
    lead: "Nous n'avons pas publié de grille tarifaire. Contactez-nous pour connaître les tarifs.",
    contactForPricing: "Contactez-nous pour connaître les tarifs",
    body: [
      "Le tarif dépend du nombre de succursales et d'utilisateurs, des modules souhaités et du mode de déploiement. Décrivez-nous votre activité et nous vous répondrons avec une proposition.",
      "Nous ne publions ni prix, ni essai gratuit, ni remise qui ne sont pas encore définis.",
    ],
    formTitle: "Demander un devis",
    name: "Votre nom", email: "E-mail", company: "Entreprise", phone: "Téléphone (facultatif)", branches: "Nombre de succursales", message: "Parlez-nous de votre activité",
    submit: "Envoyer par e-mail",
    todo: "À FAIRE : ce formulaire ouvre votre application de messagerie (mailto). Le remplacer par un traitement côté serveur et confirmer la boîte de réception avant le lancement.",
    note: "L'envoi ouvre votre application de messagerie avec le message préparé. Ce site ne conserve rien.",
  },
  about: {
    title: "À propos | QuincaiSoft par Opesware Technologies",
    description: "QuincaiSoft est développé par Opesware Technologies pour les entreprises de commerce, négoce et distribution au Cameroun et en zone CEMAC.",
    h1: "À propos de QuincaiSoft",
    lead: "QuincaiSoft est un produit d'Opesware Technologies, conçu pour les entreprises de commerce, de négoce et de distribution du Cameroun et de la zone CEMAC.",
    sections: [
      { title: "Pourquoi nous le construisons", body: "Quincailleries, vendeurs de matériaux de construction et grossistes gèrent de nombreux produits, unités et clients sur plusieurs succursales. Nous construisons un ERP et point de vente adapté à cette réalité, en français et en anglais, en FCFA." },
      { title: "Notre méthode", body: "Nous construisons par étapes et n'ouvrons une capacité que lorsqu'elle est appuyée par des preuves. C'est pourquoi nous disons clairement ce qui est terminé et ce qui est en cours." },
      { title: "Ce que nous ne revendiquons pas", body: "Nous ne revendiquons ni agrément fiscal de la DGI, ni certifications, ni clients, ni chiffres que nous ne pouvons pas démontrer." },
    ],
  },
  contact: {
    title: "Contact | QuincaiSoft",
    description: "Contactez Opesware Technologies au sujet de QuincaiSoft : pilotes, démonstrations et tarifs pour les entreprises du Cameroun et de la zone CEMAC.",
    h1: "Contactez-nous",
    lead: "Demandez une démonstration, un pilote ou les tarifs. Nous répondons en français ou en anglais.",
    emailLabel: "E-mail",
    body: "Écrivez-nous en indiquant votre type d'activité, le nombre de succursales et ce que vous souhaitez améliorer. Le formulaire ci-dessous ouvre votre application de messagerie.",
  },
  faq: {
    title: "FAQ | QuincaiSoft",
    description: "Réponses sur QuincaiSoft : ce que c'est, pour qui, monnaie, mobile money, usage hors ligne, conformité fiscale, tarifs et disponibilité.",
    h1: "Questions fréquentes",
    lead: "Des réponses courtes et directes sur QuincaiSoft.",
    items: [
      { q: "Qu'est-ce que QuincaiSoft ?", a: "QuincaiSoft est un ERP et point de vente pour les entreprises de commerce, négoce et distribution, développé par Opesware Technologies. Il couvre catalogue, stock, devis, achats, ventes, caisse et documents commerciaux." },
      { q: "À qui s'adresse QuincaiSoft ?", a: "Aux quincailleries, vendeurs de matériaux de construction et grossistes ou distributeurs du Cameroun et de la zone CEMAC, y compris ceux qui ont plusieurs succursales." },
      { q: "Fonctionne-t-il en FCFA ?", a: "Oui. Le FCFA (XAF) est la monnaie de travail, et les montants et documents sont présentés en conséquence." },
      { q: "Est-il disponible en français et en anglais ?", a: "Oui. L'interface et les documents commerciaux sont conçus pour être bilingues, français et anglais." },
      { q: "Peut-il gérer plusieurs succursales et entrepôts ?", a: "Oui. Stock, ventes et caisse sont organisés par succursale et entrepôt, avec des rapports consolidés pour les utilisateurs autorisés." },
      { q: "Prend-il en charge MTN Mobile Money ?", a: "Partiellement. Un adaptateur d'intention de paiement et de rapprochement MTN existe, mais les identifiants réels et la validation du fournisseur sont encore nécessaires. Un paiement n'est pas affiché comme vérifié tant que le fournisseur ne l'a pas confirmé." },
      { q: "Prend-il en charge Orange Money ?", a: "Pas encore. Orange Money attend une intégration contractuelle et n'est pas marqué comme intégré. Des références peuvent être saisies manuellement, mais elles ne sont jamais présentées comme vérifiées." },
      { q: "Fonctionne-t-il hors ligne ?", a: "Le client mobile est conçu pour une connexion fragile et ouvert par étapes : d'abord en ligne avec un catalogue en cache en lecture seule, puis inventaires hors ligne, puis ventes comptant hors ligne contrôlées. L'enregistrement hors ligne des ventes est actuellement désactivé tant que les preuves de quotas et d'appareils ne sont pas complètes." },
      { q: "QuincaiSoft est-il agréé par la DGI pour la facturation fiscale ?", a: "Aucun agrément n'est revendiqué. Les règles fiscales sont configurables, mais l'approbation relève des autorités compétentes et de vos comptables." },
      { q: "A-t-il des certifications OHADA, ISO ou autres ?", a: "Aucune certification n'est revendiquée. Les désignations de conformité exigent l'examen de juristes et de comptables et n'ont pas été obtenues." },
      { q: "Puis-je préparer des devis ?", a: "Oui. Les devis font partie du produit, avec suivi et transformation en ventes, ainsi que comptes clients, acomptes et retours." },
      { q: "Gère-t-il les achats et les fournisseurs ?", a: "Oui. Commandes fournisseurs, réceptions et soldes fournisseurs sont reliés au stock." },
      { q: "Combien ça coûte ?", a: "Nous n'avons pas publié de prix. Contactez-nous pour connaître les tarifs ; ils dépendent du nombre de succursales, d'utilisateurs et de modules." },
      { q: "QuincaiSoft est-il prêt pour la production ?", a: "Il est en développement actif. Plusieurs modules existent, mais tous les flux ne sont pas terminés et aucune certification de mise en production n'est revendiquée. Contactez-nous pour discuter d'un pilote." },
      { q: "Peut-il se connecter à mon logiciel Windows existant ?", a: "Une passerelle pour les installations Windows existantes est prévue, mais son intégration est en attente. Les logiciels Windows existants continuent de fonctionner seuls." },
      { q: "Où obtenir une démonstration ?", a: "Utilisez la page de contact pour demander une démonstration ou discuter d'un pilote. Nous répondons par e-mail en français ou en anglais." },
    ],
  },
  privacy: {
    title: "Politique de confidentialité (brouillon) | QuincaiSoft",
    description: "Projet de politique de confidentialité du site vitrine QuincaiSoft. Nécessite une revue juridique.",
    h1: "Politique de confidentialité",
    draftNotice: "BROUILLON : ce texte n'a pas été revu par un juriste et doit être revu et approuvé avant toute utilisation.",
    sections: [
      { title: "Périmètre", body: "Cette politique concerne le site vitrine QuincaiSoft exploité par Opesware Technologies. Elle ne couvre pas le produit QuincaiSoft, qui sera régi par ses propres conditions et accords de traitement des données." },
      { title: "Informations collectées", body: "Ce site ne dépose pas de cookies publicitaires et ne conserve pas les envois de formulaire. Lorsque vous utilisez le formulaire de contact, votre application de messagerie s'ouvre avec un message que vous pouvez modifier et envoyer ; nous recevons les informations que vous choisissez d'envoyer." },
      { title: "Utilisation", body: "Nous utilisons les messages reçus pour répondre à votre demande de démonstration, de pilote ou de tarifs." },
      { title: "Vos droits", body: "À FAIRE (juridique) : décrire les droits applicables en matière de protection des données et la durée de conservation selon le droit camerounais, et nommer le responsable du traitement et son contact." },
      { title: "Modifications", body: "À FAIRE (juridique) : décrire comment les modifications de cette politique sont annoncées." },
    ],
  },
  terms: {
    title: "Conditions d'utilisation (brouillon) | QuincaiSoft",
    description: "Projet de conditions d'utilisation du site vitrine QuincaiSoft. Nécessite une revue juridique.",
    h1: "Conditions d'utilisation",
    draftNotice: "BROUILLON : ce texte n'a pas été revu par un juriste et doit être revu et approuvé avant toute utilisation.",
    sections: [
      { title: "Objet de ce site", body: "Ce site présente QuincaiSoft à titre d'information uniquement. Il ne constitue ni une offre, ni un contrat, ni un engagement sur la disponibilité, les fonctionnalités ou le prix." },
      { title: "Exactitude", body: "Nous décrivons le périmètre visé du produit et son état actuel aussi précisément que possible. Les fonctionnalités peuvent évoluer, et certaines sont en développement." },
      { title: "Aucune revendication de certification", body: "Rien sur ce site n'affirme ni n'implique un agrément fiscal, juridique ou réglementaire du produit." },
      { title: "Propriété intellectuelle", body: "À FAIRE (juridique) : mentions de marque et de droit d'auteur pour QuincaiSoft et Opesware Technologies." },
      { title: "Responsabilité et droit applicable", body: "À FAIRE (juridique) : limitation de responsabilité, droit applicable et juridiction." },
    ],
  },
  notFound: { title: "Page introuvable", body: "La page que vous cherchez n'existe pas ou a été déplacée.", back: "Retour à l'accueil" },
  softwareDescription: "ERP et point de vente pour quincailleries, vendeurs de matériaux de construction et grossistes au Cameroun et en zone CEMAC : stock multi-succursales, devis, achats, caisse et documents bilingues en FCFA.",
};

export const DICT: Record<Lang, Dict> = { en, fr };
export function t(lang: Lang): Dict {
  return DICT[lang];
}
