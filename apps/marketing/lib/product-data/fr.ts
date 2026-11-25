import type { Dataset } from "../product-types";
import { products, resourcesHub, titles, ui } from "./fr-pages";
import { faq, features, industries, pricing } from "./fr-site";
import { guidesA } from "./fr-guides-a";
import { guidesB } from "./fr-guides-b";
import { extras, more } from "./fr-guides-extra";

export const fr: Dataset = {
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
