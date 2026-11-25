import type { MetadataRoute } from "next";
import { LANGS, abs, allPaths, pagePath } from "@/lib/site";

export const dynamic = "force-static";

// Draft legal pages are noindex and left out of the sitemap until reviewed.
const EXCLUDED = new Set(["privacy", "terms", "accessibility", "cookies"]);

export default function sitemap(): MetadataRoute.Sitemap {
  return allPaths()
    .filter((p) => !EXCLUDED.has(p))
    .flatMap((path) =>
      LANGS.map((lang) => ({
        url: abs(pagePath(lang, path)),
        changeFrequency: "monthly" as const,
        priority: path === "" ? 1 : 0.7,
        alternates: {
          languages: {
            en: abs(pagePath("en", path)),
            fr: abs(pagePath("fr", path)),
            "x-default": abs(pagePath("en", path)),
          },
        },
      })),
    );
}
