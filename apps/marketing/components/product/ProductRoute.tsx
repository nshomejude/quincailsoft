import type { Metadata } from "next";
import { getLang, type LangParams } from "@/lib/route";
import { buildMetadata } from "@/lib/site";
import { getData } from "@/lib/product-content";
import type { ProductSlug } from "@/lib/product-routes";
import { ProductPage } from "./ProductPage";

export async function productMetadata(params: LangParams["params"], slug: ProductSlug): Promise<Metadata> {
  const lang = await getLang(params);
  const d = getData(lang).products[slug];
  return buildMetadata(lang, slug, d.metaTitle, d.description);
}

export async function renderProduct(params: LangParams["params"], slug: ProductSlug) {
  const lang = await getLang(params);
  const data = getData(lang);
  const d = data.products[slug];
  return <ProductPage lang={lang} path={slug} data={d} crumbs={[{ name: data.titles[slug], path: slug }]} />;
}
