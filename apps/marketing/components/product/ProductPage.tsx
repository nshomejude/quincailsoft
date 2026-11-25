import "@/app/product.css";
import Link from "next/link";
import type { ReactNode } from "react";
import { Shell } from "@/components/Shell";
import { abs, faqLd, pagePath, type Lang } from "@/lib/site";
import { getUi, linkTitles } from "@/lib/product-content";
import type { Block, Faq, Glance, IconName, ProductPageData, ProductUi } from "@/lib/product-types";
import { Blocks, RelatedLinks, StatusTag } from "./Blocks";
import { Icon } from "./Icon";

export const PUBLISHED = "2026-11-25";

export function webPageLd(lang: Lang, path: string, name: string, description: string, type = "WebPage") {
  return {
    "@context": "https://schema.org",
    "@type": type,
    "@id": `${abs(pagePath(lang, path))}#webpage`,
    url: abs(pagePath(lang, path)),
    name,
    description,
    inLanguage: lang,
    isPartOf: { "@id": abs("/#website") },
    publisher: { "@id": abs("/#organization") },
    datePublished: PUBLISHED,
    dateModified: PUBLISHED,
  };
}

export function Toc({ blocks, label }: { blocks: Block[]; label: string }) {
  const entries = blocks.filter((b): b is Exclude<Block, { kind: "limits" }> => b.kind !== "limits" && !!b.id);
  if (entries.length < 4) return null;
  return (
    <nav className="pr-toc" aria-label={label}>
      <div className="wrap">
        <p className="pr-toc-label">{label}</p>
        <ul>
          {entries.map((b) => (
            <li key={b.id}><a href={`#${b.id}`}>{b.title}</a></li>
          ))}
        </ul>
      </div>
    </nav>
  );
}

export function Hero({
  icon, kicker, h1, answer, glance, ui, lang, children, secondary = true,
}: {
  icon: IconName; kicker: string; h1: string; answer: string; glance?: Glance; ui: ProductUi; lang: Lang; children?: ReactNode; secondary?: boolean;
}) {
  return (
    <section className="hero pr-hero">
      <div className={`wrap pr-hero-grid${glance ? " pr-has-glance" : ""}`}>
        <div>
          <p className="pr-kicker"><Icon name={icon} />{kicker}</p>
          <h1>{h1}</h1>
          <p className="lead">{answer}</p>
          {children}
          <div className="actions">
            <Link className="btn" href={pagePath(lang, "contact")}>{ui.contact}</Link>
            {secondary && <Link className="btn secondary" href={pagePath(lang, "features")}>{ui.seeFeatures}</Link>}
          </div>
        </div>
        {glance && (
          <aside className="pr-glance" aria-label={glance.title}>
            <h2>{glance.title}</h2>
            <ul>
              {glance.items.map((g) => (
                <li key={g.label}>
                  <strong>{g.label}</strong>
                  {g.status && <StatusTag status={g.status} ui={ui} />}
                  <span className="pr-note">{g.note}</span>
                </li>
              ))}
            </ul>
          </aside>
        )}
      </div>
    </section>
  );
}

export function RelatedSection({ lang, paths, title }: { lang: Lang; paths: string[]; title: string }) {
  return (
    <section className="section alt">
      <div className="wrap">
        <h2>{title}</h2>
        <RelatedLinks lang={lang} paths={paths} titles={linkTitles(lang)} />
      </div>
    </section>
  );
}

export function CtaSection({ lang, title, body, ui }: { lang: Lang; title: string; body: string; ui: ProductUi }) {
  return (
    <section className="section">
      <div className="wrap pr-cta">
        <h2>{title}</h2>
        <p>{body}</p>
        <div className="actions" style={{ marginTop: 8 }}>
          <Link className="btn" href={pagePath(lang, "contact")}>{ui.contact}</Link>
          <Link className="btn secondary" href={pagePath(lang, "pricing")}>{linkTitles(lang)["pricing"]}</Link>
        </div>
      </div>
    </section>
  );
}

export function faqsOf(blocks: Block[]): Faq[] {
  return blocks.flatMap((b) => (b.kind === "faq" ? b.items : []));
}

export function ProductPage({
  lang, path, data, crumbs, afterHero, afterBlocks, extraSchemas = [],
}: {
  lang: Lang;
  path: string;
  data: ProductPageData;
  crumbs: { name: string; path: string }[];
  afterHero?: ReactNode;
  afterBlocks?: ReactNode;
  extraSchemas?: unknown[];
}) {
  const ui = getUi(lang);
  const faqs = faqsOf(data.blocks);
  const schemas: unknown[] = [webPageLd(lang, path, data.metaTitle, data.description), ...extraSchemas];
  if (faqs.length) schemas.push(faqLd(faqs));
  return (
    <Shell lang={lang} path={path} crumbs={crumbs} schemas={schemas}>
      <Hero icon={data.icon} kicker={data.kicker} h1={data.h1} answer={data.answer} glance={data.glance} ui={ui} lang={lang} />
      {afterHero}
      <Toc blocks={data.blocks} label={ui.onThisPage} />
      <Blocks blocks={data.blocks} ui={ui} />
      {afterBlocks}
      <RelatedSection lang={lang} paths={data.related} title={ui.related} />
      <CtaSection lang={lang} title={data.ctaTitle} body={data.ctaBody} ui={ui} />
    </Shell>
  );
}
