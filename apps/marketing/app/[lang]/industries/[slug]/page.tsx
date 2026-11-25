import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ProductPage } from "@/components/product/ProductPage";
import { t } from "@/lib/content";
import { getData } from "@/lib/product-content";
import { INDUSTRY_SLUGS, LANGS, buildMetadata, type IndustrySlug } from "@/lib/site";
import { getLang } from "@/lib/route";

type P = { params: Promise<{ lang: string; slug: string }> };

export const dynamicParams = false;

export function generateStaticParams() {
  return LANGS.flatMap((lang) => INDUSTRY_SLUGS.map((slug) => ({ lang, slug })));
}

async function resolve(params: P["params"]) {
  const p = await params;
  const lang = await getLang(Promise.resolve(p));
  if (!(INDUSTRY_SLUGS as readonly string[]).includes(p.slug)) notFound();
  return { lang, slug: p.slug as IndustrySlug };
}

export async function generateMetadata({ params }: P): Promise<Metadata> {
  const { lang, slug } = await resolve(params);
  const page = getData(lang).industries.items[slug];
  return buildMetadata(lang, `industries/${slug}`, page.metaTitle, page.description);
}

export default async function Industry({ params }: P) {
  const { lang, slug } = await resolve(params);
  const d = t(lang);
  const page = getData(lang).industries.items[slug];
  return (
    <ProductPage
      lang={lang}
      path={`industries/${slug}`}
      data={page}
      crumbs={[
        { name: d.nav.industries, path: "industries" },
        { name: d.industries.items[slug].name, path: `industries/${slug}` },
      ]}
    />
  );
}
