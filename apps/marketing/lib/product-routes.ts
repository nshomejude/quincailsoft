/**
 * Routes added by the product-pages branch, for wiring into app/sitemap.ts and the navigation after merge.
 * Paths are language-less; each exists under /en and /fr.
 */
export type ProductRoute = {
  path: string;
  priority: number;
  changefreq: "daily" | "weekly" | "monthly" | "yearly";
  enTitle: string;
  frTitle: string;
};

export const GUIDE_SLUGS = [
  "track-cement-and-bulk-materials-fcfa",
  "manage-customer-credit-hardware-shop",
  "buy-and-receive-stock-from-suppliers",
  "quotations-for-contractors",
  "run-multiple-branches",
  "mobile-money-reconciliation-basics",
] as const;
export type GuideSlug = (typeof GUIDE_SLUGS)[number];

export const PRODUCT_SLUGS = ["mobile-app", "payments", "online-store", "security", "desktop", "migration-and-onboarding", "compare"] as const;
export type ProductSlug = (typeof PRODUCT_SLUGS)[number];

export const PRODUCT_ROUTES: ProductRoute[] = [
  { path: "mobile-app", priority: 0.7, changefreq: "monthly", enTitle: "Mobile app", frTitle: "Application mobile" },
  { path: "payments", priority: 0.7, changefreq: "monthly", enTitle: "Payments and mobile money", frTitle: "Paiements et mobile money" },
  { path: "online-store", priority: 0.6, changefreq: "monthly", enTitle: "Online store", frTitle: "Boutique en ligne" },
  { path: "security", priority: 0.7, changefreq: "monthly", enTitle: "Security and data", frTitle: "Sécurité et données" },
  { path: "desktop", priority: 0.6, changefreq: "monthly", enTitle: "Windows desktop", frTitle: "Logiciel Windows" },
  { path: "migration-and-onboarding", priority: 0.6, changefreq: "monthly", enTitle: "Migration and onboarding", frTitle: "Migration et mise en route" },
  { path: "compare", priority: 0.6, changefreq: "monthly", enTitle: "Choosing retail software for CEMAC", frTitle: "Choisir un logiciel de commerce en zone CEMAC" },
  { path: "resources", priority: 0.7, changefreq: "weekly", enTitle: "Resources and guides", frTitle: "Ressources et guides" },
  { path: "resources/track-cement-and-bulk-materials-fcfa", priority: 0.6, changefreq: "monthly", enTitle: "How to track cement and bulk materials in FCFA", frTitle: "Suivre le ciment et les matériaux en vrac en FCFA" },
  { path: "resources/manage-customer-credit-hardware-shop", priority: 0.6, changefreq: "monthly", enTitle: "Managing customer credit in a hardware shop", frTitle: "Gérer le crédit client dans une quincaillerie" },
  { path: "resources/buy-and-receive-stock-from-suppliers", priority: 0.6, changefreq: "monthly", enTitle: "Buying and receiving stock from suppliers", frTitle: "Acheter et réceptionner le stock des fournisseurs" },
  { path: "resources/quotations-for-contractors", priority: 0.6, changefreq: "monthly", enTitle: "Quotations for contractors", frTitle: "Devis pour les entrepreneurs" },
  { path: "resources/run-multiple-branches", priority: 0.6, changefreq: "monthly", enTitle: "Running multiple branches", frTitle: "Gérer plusieurs succursales" },
  { path: "resources/mobile-money-reconciliation-basics", priority: 0.6, changefreq: "monthly", enTitle: "Mobile money reconciliation basics", frTitle: "Bases du rapprochement mobile money" },
];

/** Titles of pages that already exist elsewhere in the site, used for related-link labels. */
export const EXISTING_TITLES: Record<string, { en: string; fr: string }> = {
  "": { en: "Home", fr: "Accueil" },
  features: { en: "Features", fr: "Fonctionnalités" },
  industries: { en: "Industries", fr: "Secteurs" },
  pricing: { en: "Pricing", fr: "Tarifs" },
  faq: { en: "FAQ", fr: "FAQ" },
  contact: { en: "Contact", fr: "Contact" },
  about: { en: "About", fr: "À propos" },
  "industries/hardware-shops": { en: "Hardware shops", fr: "Quincailleries" },
  "industries/construction-materials": { en: "Construction materials", fr: "Matériaux de construction" },
  "industries/wholesale-distribution": { en: "Wholesale and distribution", fr: "Gros et distribution" },
};
