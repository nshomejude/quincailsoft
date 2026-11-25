import { abs } from "@/lib/site";

/** SiteNavigationElement list for the primary navigation (names and absolute URLs). */
export function navigationLd(items: { name: string; path: string }[]) {
  return {
    "@context": "https://schema.org",
    "@graph": items.map((it) => ({
      "@type": "SiteNavigationElement",
      "@id": `${abs(it.path)}#nav`,
      name: it.name,
      url: abs(it.path),
    })),
  };
}
