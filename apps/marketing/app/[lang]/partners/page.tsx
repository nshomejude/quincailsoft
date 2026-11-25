import type { Metadata } from "next";
import Link from "next/link";
import { Shell } from "@/components/Shell";
import { CardGrid, Checklist, Faq, Hero, Section, Steps } from "@/components/company/parts";
import { companyMeta } from "@/components/company/meta";
import { CC } from "@/lib/company-content";
import { getLang, type LangParams } from "@/lib/route";
import { abs, faqLd, pagePath } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const m = CC[lang].partners.meta;
  return companyMeta(lang, "partners", m.title, m.description);
}

export default async function Partners({ params }: LangParams) {
  const lang = await getLang(params);
  const c = CC[lang];
  const p = c.partners;
  const url = abs(pagePath(lang, "partners"));
  const schemas = [
    { "@context": "https://schema.org", "@type": "WebPage", "@id": `${url}#webpage`, url, name: p.meta.h1, description: p.meta.description, inLanguage: lang, isPartOf: { "@id": abs("/#website") } },
    faqLd(p.faq),
  ];
  return (
    <Shell lang={lang} path="partners" crumbs={[{ name: c.ui.crumbs.partners, path: "partners" }]} schemas={schemas}>
      <Hero h1={p.meta.h1} lead={p.meta.lead}>
        <div className="actions">
          <Link className="btn" href={pagePath(lang, "contact")}>{p.formCta}</Link>
        </div>
      </Hero>
      <Section title={p.typesTitle} id="types">
        <CardGrid items={p.types} lang={lang} />
      </Section>
      <Section title={p.lookTitle} id="look" alt>
        <div className="co-two even">
          <div className="co-panel"><Checklist items={p.look} /></div>
          <div className="co-panel todo-panel">
            <h3>{p.notTitle}</h3>
            <p>{p.not}</p>
          </div>
        </div>
      </Section>
      <Section title={p.applyTitle} id="apply">
        <Steps items={p.apply} />
        <div className="actions">
          <Link className="btn" href={pagePath(lang, "contact")}>{p.formCta}</Link>
        </div>
      </Section>
      <Section title={c.ui.faqTitle} id="faq" alt>
        <Faq items={p.faq} />
      </Section>
    </Shell>
  );
}
