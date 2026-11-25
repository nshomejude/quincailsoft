/**
 * Routes added by the company/support pages. Wire into app/sitemap.ts, PAGES in
 * lib/site.ts, llms.txt and the footer after merge.
 */
export type CompanyRoute = {
  path: string;
  priority: number;
  changefreq: "daily" | "weekly" | "monthly" | "yearly";
  enTitle: string;
  frTitle: string;
  /** Draft legal-ish pages are served with noindex until reviewed. */
  noindex?: boolean;
};

export const COMPANY_ROUTES: CompanyRoute[] = [
  { path: "about", priority: 0.6, changefreq: "monthly", enTitle: "About", frTitle: "À propos" },
  { path: "contact", priority: 0.7, changefreq: "monthly", enTitle: "Contact", frTitle: "Contact" },
  { path: "request-a-demo", priority: 0.8, changefreq: "monthly", enTitle: "Request a demo", frTitle: "Demander une démo" },
  { path: "support", priority: 0.6, changefreq: "monthly", enTitle: "Support", frTitle: "Assistance" },
  { path: "partners", priority: 0.5, changefreq: "monthly", enTitle: "Partners", frTitle: "Partenaires" },
  { path: "opesware", priority: 0.5, changefreq: "yearly", enTitle: "Opesware Technologies", frTitle: "Opesware Technologies" },
  { path: "accessibility", priority: 0.2, changefreq: "yearly", enTitle: "Accessibility (draft)", frTitle: "Accessibilité (brouillon)", noindex: true },
  { path: "cookies", priority: 0.2, changefreq: "yearly", enTitle: "Cookie policy (draft)", frTitle: "Politique de cookies (brouillon)", noindex: true },
];

export const NEW_COMPANY_SLUGS = ["request-a-demo", "support", "partners", "opesware", "accessibility", "cookies"] as const;
