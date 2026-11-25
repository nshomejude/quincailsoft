import "@/app/product.css";
import Link from "next/link";
import type { Metadata } from "next";
import { ArrowRight } from "lucide-react";
import { Shell } from "@/components/Shell";
import { CtaSection, Hero, RelatedSection, webPageLd } from "@/components/product/ProductPage";
import { SectionHead } from "@/components/product/Blocks";
import { Icon } from "@/components/product/Icon";
import { getData, getGuides, readingMinutes } from "@/lib/product-content";
import { getLang, type LangParams } from "@/lib/route";
import { abs, buildMetadata, pagePath } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const h = getData(lang).resourcesHub;
  return buildMetadata(lang, "resources", h.metaTitle, h.description);
}

export default async function Resources({ params }: LangParams) {
  const lang = await getLang(params);
  const data = getData(lang);
  const h = data.resourcesHub;
  const ui = data.ui;
  const guides = getGuides(lang);
  const list = {
    "@context": "https://schema.org",
    "@type": "ItemList",
    itemListElement: guides.map((g, i) => ({
      "@type": "ListItem",
      position: i + 1,
      name: g.h1,
      url: abs(pagePath(lang, `resources/${g.slug}`)),
    })),
  };
  return (
    <Shell
      lang={lang}
      path="resources"
      crumbs={[{ name: data.titles.resources, path: "resources" }]}
      schemas={[webPageLd(lang, "resources", h.metaTitle, h.description, "CollectionPage"), list]}
    >
      <Hero icon="book" kicker={h.kicker} h1={h.h1} answer={h.answer} ui={ui} lang={lang} secondary={false} />
      <section className="section pr-sec" id="guides">
        <div className="wrap">
          <SectionHead id="guides-title" title={h.listTitle} />
          <ul className="pr-guide-list">
            {guides.map((g) => (
              <li key={g.slug}>
                <Link className="pr-card" href={pagePath(lang, `resources/${g.slug}`)}>
                  <h3>
                    <Icon name={g.icon} />
                    <span>{g.h1}</span>
                  </h3>
                  <p>{g.description}</p>
                  <span className="small">{readingMinutes(g)} {ui.minutes}</span>
                  <span className="pr-readmore">
                    {ui.readGuide}
                    <ArrowRight size={18} strokeWidth={1.75} color="currentColor" fill="none" aria-hidden="true" focusable="false" />
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </div>
      </section>
      <RelatedSection lang={lang} paths={h.related} title={ui.related} />
      <CtaSection lang={lang} title={data.products["mobile-app"].ctaTitle} body={data.products["mobile-app"].ctaBody} ui={ui} />
    </Shell>
  );
}
