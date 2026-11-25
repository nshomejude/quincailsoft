import type { Lang } from "./site";
import { EXISTING_TITLES } from "./product-routes";
import type { Dataset, GuideData } from "./product-types";
import { en } from "./product-data/en";
import { fr } from "./product-data/fr";

export type * from "./product-types";

export function getData(lang: Lang): Dataset {
  return lang === "fr" ? fr : en;
}

export function getUi(lang: Lang) {
  return getData(lang).ui;
}

export function getGuides(lang: Lang): GuideData[] {
  return getData(lang).guides;
}

export function getGuide(lang: Lang, slug: string): GuideData | undefined {
  return getData(lang).guides.find((g) => g.slug === slug);
}

/** Titles for internal links, by language-less path. */
export function linkTitles(lang: Lang): Record<string, string> {
  const out: Record<string, string> = {};
  for (const [p, t] of Object.entries(EXISTING_TITLES)) out[p] = t[lang];
  Object.assign(out, getData(lang).titles);
  return out;
}

export function guideWords(g: GuideData): number {
  return [g.answer, ...g.sections.flatMap((s) => [s.h2, ...(s.paras ?? []), ...(s.list ?? [])]), ...(g.faq ?? []).flatMap((f) => [f.q, f.a])]
    .join(" ")
    .split(/\s+/).length;
}

/** Rough reading time in minutes for a guide. */
export function readingMinutes(g: GuideData): number {
  return Math.max(1, Math.round(guideWords(g) / 200));
}
