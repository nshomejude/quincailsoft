import type { Dataset } from "../product-types";
import { products, resourcesHub, titles, ui } from "./en-pages";
import { faq, features, industries, pricing } from "./en-site";
import { guidesA } from "./en-guides-a";
import { guidesB } from "./en-guides-b";
import { extras, more } from "./en-guides-extra";

export const en: Dataset = {
  ui,
  products,
  resourcesHub,
  titles,
  features,
  industries,
  pricing,
  faq,
  guides: [...guidesA, ...guidesB].map((g) => {
    const add = [...(extras[g.slug] ?? []), ...(more[g.slug] ?? [])];
    const last = g.sections[g.sections.length - 1];
    return { ...g, sections: [...g.sections.slice(0, -1), ...add, last] };
  }),
};
