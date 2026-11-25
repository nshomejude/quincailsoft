import { COMPANY_ROUTES } from "@/lib/company-routes";
import { EXISTING_TITLES, PRODUCT_ROUTES } from "@/lib/product-routes";
import type { Lang } from "@/lib/site";

const TITLES: Record<string, { en: string; fr: string }> = { ...EXISTING_TITLES };
for (const r of [...PRODUCT_ROUTES, ...COMPANY_ROUTES]) TITLES[r.path] = { en: r.enTitle, fr: r.frTitle };
TITLES.privacy = { en: "Privacy", fr: "Confidentialité" };
TITLES.terms = { en: "Terms", fr: "Conditions" };
TITLES.accessibility = { en: "Accessibility", fr: "Accessibilité" };
TITLES.cookies = { en: "Cookie policy", fr: "Politique de cookies" };
TITLES["migration-and-onboarding"] = { en: "Migration and onboarding", fr: "Migration et mise en route" };
TITLES.resources = { en: "Guides and resources", fr: "Guides et ressources" };
TITLES.blog = { en: "Blog", fr: "Blog" };

export function title(path: string, lang: Lang): string {
  return TITLES[path]?.[lang] ?? path;
}

/** Footer columns: language-less paths. Keep in sync with sitemap coverage. */
export const FOOTER_COLUMNS = {
  product: ["features", "mobile-app", "payments", "online-store", "security", "desktop", "pricing", "compare"],
  resources: ["blog", "resources", "migration-and-onboarding", "faq", "support"],
  company: ["about", "opesware", "partners", "contact", "request-a-demo"],
  legal: ["privacy", "terms", "cookies", "accessibility"],
} as const;

export const HEADER_ITEMS = ["features", "industries", "pricing", "resources", "about", "contact"] as const;
