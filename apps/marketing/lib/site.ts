import type { Metadata } from "next";

export const LANGS = ["en", "fr"] as const;
export type Lang = (typeof LANGS)[number];

export const SITE_URL = (process.env.NEXT_PUBLIC_SITE_URL || "https://quincaisoft.com").replace(/\/+$/, "");
// TODO(Opesware): confirm the real contact inbox before launch.
export const CONTACT_EMAIL = process.env.NEXT_PUBLIC_CONTACT_EMAIL || "contact@quincaisoft.com";

export const PAGES = ["", "features", "industries", "pricing", "about", "contact", "faq", "privacy", "terms"] as const;
export const INDUSTRY_SLUGS = ["hardware-shops", "construction-materials", "wholesale-distribution"] as const;
export type IndustrySlug = (typeof INDUSTRY_SLUGS)[number];

export function isLang(v: string): v is Lang {
  return (LANGS as readonly string[]).includes(v);
}

export function abs(path: string): string {
  return `${SITE_URL}${path}`;
}

export function pagePath(lang: Lang, path: string): string {
  return path ? `/${lang}/${path}` : `/${lang}`;
}

/** Every public path (without language) for sitemap and llms. */
export const EXTRA_PAGES = [
  "request-a-demo", "support", "partners", "opesware",
  "mobile-app", "payments", "online-store", "security", "desktop", "migration-and-onboarding", "compare",
  "resources",
  "resources/track-cement-and-bulk-materials-fcfa", "resources/manage-customer-credit-hardware-shop",
  "resources/buy-and-receive-stock-from-suppliers", "resources/quotations-for-contractors",
  "resources/run-multiple-branches", "resources/mobile-money-reconciliation-basics",
  // Draft pages: reachable, noindex, excluded from the sitemap.
  "accessibility", "cookies",
] as const;

export function allPaths(): string[] {
  return [...PAGES, ...INDUSTRY_SLUGS.map((s) => `industries/${s}`), ...EXTRA_PAGES];
}

export function buildMetadata(lang: Lang, path: string, title: string, description: string): Metadata {
  const url = abs(pagePath(lang, path));
  const other: Lang = lang === "en" ? "fr" : "en";
  return {
    title,
    description,
    alternates: {
      canonical: url,
      languages: {
        en: abs(pagePath("en", path)),
        fr: abs(pagePath("fr", path)),
        "x-default": abs(pagePath("en", path)),
      },
    },
    openGraph: {
      type: "website",
      siteName: "QuincaiSoft",
      title,
      description,
      url,
      locale: lang === "en" ? "en_US" : "fr_FR",
      alternateLocale: other === "en" ? "en_US" : "fr_FR",
      images: [{ url: abs("/logo.png"), width: 1536, height: 512, alt: "QuincaiSoft" }],
    },
    twitter: {
      card: "summary_large_image",
      title,
      description,
      images: [abs("/logo.png")],
    },
  };
}

export function organizationLd() {
  return {
    "@context": "https://schema.org",
    "@type": "Organization",
    "@id": abs("/#organization"),
    name: "Opesware Technologies",
    brand: { "@type": "Brand", name: "QuincaiSoft" },
    url: SITE_URL,
    logo: abs("/logo.png"),
    areaServed: { "@type": "Country", name: "Cameroon", identifier: "CM" },
    contactPoint: [
      {
        "@type": "ContactPoint",
        contactType: "sales",
        email: CONTACT_EMAIL,
        availableLanguage: ["English", "French"],
      },
    ],
  };
}

export function websiteLd(lang: Lang) {
  return {
    "@context": "https://schema.org",
    "@type": "WebSite",
    "@id": abs("/#website"),
    name: "QuincaiSoft",
    url: abs(`/${lang}`),
    inLanguage: lang,
    publisher: { "@id": abs("/#organization") },
  };
}

export function softwareLd(lang: Lang, description: string) {
  return {
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    "@id": abs("/#software"),
    name: "QuincaiSoft",
    applicationCategory: "BusinessApplication",
    applicationSubCategory: "ERP, point of sale",
    operatingSystem: "Web",
    description,
    inLanguage: lang,
    url: abs(`/${lang}`),
    publisher: { "@id": abs("/#organization") },
    areaServed: { "@type": "Country", name: "Cameroon", identifier: "CM" },
  };
}

export function breadcrumbLd(items: { name: string; path: string }[]) {
  return {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: items.map((it, i) => ({
      "@type": "ListItem",
      position: i + 1,
      name: it.name,
      item: abs(it.path),
    })),
  };
}

export function faqLd(items: { q: string; a: string }[]) {
  return {
    "@context": "https://schema.org",
    "@type": "FAQPage",
    mainEntity: items.map((it) => ({
      "@type": "Question",
      name: it.q,
      acceptedAnswer: { "@type": "Answer", text: it.a },
    })),
  };
}
