import "@/app/product.css";
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Shell } from "@/components/Shell";
import { CtaSection, Hero, PUBLISHED, RelatedSection, webPageLd } from "@/components/product/ProductPage";
import { getData, getGuide, guideWords, readingMinutes } from "@/lib/product-content";
import { GUIDE_SLUGS } from "@/lib/product-routes";
import { getLang } from "@/lib/route";
import { LANGS, abs, buildMetadata, faqLd, pagePath } from "@/lib/site";

type P = { params: Promise<{ lang: string; slug: string }> };

export const dynamicParams = false;

export function generateStaticParams() {
  return LANGS.flatMap((lang) => GUIDE_SLUGS.map((slug) => ({ lang, slug })));
}

async function resolve(params: P["params"]) {
  const p = await params;
  const lang = await getLang(Promise.resolve(p));
  const guide = getGuide(lang, p.slug);
  if (!guide) notFound();
  return { lang, guide };
}

export async function generateMetadata({ params }: P): Promise<Metadata> {
  const { lang, guide } = await resolve(params);
  const path = `resources/${guide.slug}`;
  const m = buildMetadata(lang, path, guide.metaTitle, guide.description);
  return {
    ...m,
    keywords: guide.keywords,
    authors: [{ name: "Opesware Technologies", url: "https://opesware.com" }],
    openGraph: { ...m.openGraph, type: "article", publishedTime: PUBLISHED, authors: ["Opesware Technologies"] },
  };
}

const ORG = { "@type": "Organization", name: "Opesware Technologies", url: "https://opesware.com" };

export default async function Guide({ params }: P) {
  const { lang, guide } = await resolve(params);
  const data = getData(lang);
  const ui = data.ui;
  const path = `resources/${guide.slug}`;
  const url = abs(pagePath(lang, path));
  const article = {
    "@context": "https://schema.org",
    "@type": "Article",
    "@id": `${url}#article`,
    headline: guide.h1,
    description: guide.description,
    inLanguage: lang,
    url,
    mainEntityOfPage: url,
    datePublished: PUBLISHED,
    dateModified: PUBLISHED,
    author: ORG,
    publisher: { ...ORG, "@id": abs("/#organization"), logo: { "@type": "ImageObject", url: abs("/logo.png") } },
    keywords: guide.keywords.join(", "),
    wordCount: guideWords(guide),
  };
  const schemas: unknown[] = [webPageLd(lang, path, guide.metaTitle, guide.description), article];
  if (guide.howTo) {
    schemas.push({
      "@context": "https://schema.org",
      "@type": "HowTo",
      name: guide.howTo.name,
      description: guide.description,
      inLanguage: lang,
      datePublished: PUBLISHED,
      author: ORG,
      publisher: ORG,
      step: guide.howTo.steps.map((s, i) => ({ "@type": "HowToStep", position: i + 1, name: s.name, text: s.text })),
    });
  }
  if (guide.faq?.length) schemas.push(faqLd(guide.faq));
  return (
    <Shell
      lang={lang}
      path={path}
      crumbs={[
        { name: data.titles.resources, path: "resources" },
        { name: guide.h1, path },
      ]}
      schemas={schemas}
    >
      <Hero icon={guide.icon} kicker={ui.guidesTitle} h1={guide.h1} answer={guide.answer} ui={ui} lang={lang} secondary={false}>
        <ul className="pr-meta">
          <li>{ui.author}</li>
          <li><time dateTime={PUBLISHED}>{ui.published}</time></li>
          <li>{readingMinutes(guide)} {ui.minutes}</li>
        </ul>
      </Hero>
      <article className="section pr-article">
        <div className="wrap">
          <div className="pr-article-body">
            {guide.sections.map((s) => (
              <section key={s.h2}>
                <h2>{s.h2}</h2>
                {s.paras?.map((p) => <p key={p}>{p}</p>)}
                {s.list && (s.ordered ? <ol>{s.list.map((x) => <li key={x}>{x}</li>)}</ol> : <ul>{s.list.map((x) => <li key={x}>{x}</li>)}</ul>)}
              </section>
            ))}
            {guide.faq?.map((f) => (
              <section key={f.q}>
                <h2>{f.q}</h2>
                <p>{f.a}</p>
              </section>
            ))}
          </div>
        </div>
      </article>
      <RelatedSection lang={lang} paths={guide.related} title={ui.relatedGuides} />
      <CtaSection lang={lang} title={data.products["mobile-app"].ctaTitle} body={data.products["mobile-app"].ctaBody} ui={ui} />
    </Shell>
  );
}
