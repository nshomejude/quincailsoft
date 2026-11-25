import type { Metadata } from "next";
import Link from "next/link";
import { Shell } from "@/components/Shell";
import { CardGrid, Hero, Section } from "@/components/company/parts";
import { companyMeta } from "@/components/company/meta";
import { CC, OPESWARE_URL } from "@/lib/company-content";
import { getLang, type LangParams } from "@/lib/route";
import { abs, pagePath } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const m = CC[lang].opesware.meta;
  return companyMeta(lang, "opesware", m.title, m.description);
}

export default async function Opesware({ params }: LangParams) {
  const lang = await getLang(params);
  const c = CC[lang];
  const o = c.opesware;
  const url = abs(pagePath(lang, "opesware"));
  const org = {
    "@type": "Organization",
    "@id": `${OPESWARE_URL}/#organization`,
    name: "Opesware Technologies",
    url: OPESWARE_URL,
    brand: { "@type": "Brand", name: "QuincaiSoft" },
  };
  const schemas = [
    { "@context": "https://schema.org", "@type": "AboutPage", "@id": `${url}#webpage`, url, name: o.meta.h1, description: o.meta.description, inLanguage: lang, mainEntity: org },
  ];
  return (
    <Shell lang={lang} path="opesware" crumbs={[{ name: c.ui.crumbs.opesware, path: "opesware" }]} schemas={schemas}>
      <Hero h1={o.meta.h1} lead={o.meta.lead}>
        <div className="actions">
          <a className="btn" href={OPESWARE_URL} rel="noopener">{o.visit}</a>
        </div>
      </Hero>
      <Section title={o.factsTitle} id="facts">
        <CardGrid items={o.facts} lang={lang} />
      </Section>
      <Section id="sources" alt>
        <div className="co-two even">
          <div className="co-panel todo-panel">
            <h2>{o.noteTitle}</h2>
            <p>{o.note}</p>
          </div>
          <div className="co-panel">
            <h2>{o.siteTitle}</h2>
            <p>{o.siteBody} <a href={OPESWARE_URL} rel="noopener">{o.visit}</a></p>
            <h3>{o.productTitle}</h3>
            <p>{o.productBody} <Link href={pagePath(lang, "")}>{lang === "en" ? "Home" : "Accueil"}</Link> · <Link href={pagePath(lang, "about")}>{c.ui.crumbs.about}</Link></p>
          </div>
        </div>
      </Section>
    </Shell>
  );
}
