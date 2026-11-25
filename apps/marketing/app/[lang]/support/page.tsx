import type { Metadata } from "next";
import Link from "next/link";
import { Shell } from "@/components/Shell";
import { CardGrid, Checklist, Faq, Hero, Section } from "@/components/company/parts";
import { companyMeta } from "@/components/company/meta";
import { CC } from "@/lib/company-content";
import { getLang, type LangParams } from "@/lib/route";
import { abs, faqLd, pagePath } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const m = CC[lang].support.meta;
  return companyMeta(lang, "support", m.title, m.description);
}

export default async function Support({ params }: LangParams) {
  const lang = await getLang(params);
  const c = CC[lang];
  const s = c.support;
  const url = abs(pagePath(lang, "support"));
  const schemas = [
    { "@context": "https://schema.org", "@type": "WebPage", "@id": `${url}#webpage`, url, name: s.meta.h1, description: s.meta.description, inLanguage: lang, isPartOf: { "@id": abs("/#website") } },
    faqLd(s.faq),
  ];
  return (
    <Shell lang={lang} path="support" crumbs={[{ name: c.ui.crumbs.support, path: "support" }]} schemas={schemas}>
      <Hero h1={s.meta.h1} lead={s.meta.lead}>
        <div className="actions">
          <Link className="btn" href={pagePath(lang, "contact")}>{s.formCta}</Link>
          <Link className="btn secondary" href={pagePath(lang, "faq")}>{c.ui.faqLink}</Link>
        </div>
      </Hero>
      <Section title={s.waysTitle} id="ways">
        <CardGrid items={s.ways} lang={lang} />
      </Section>
      <Section title={s.topicsTitle} id="topics" alt>
        <CardGrid items={s.topics} lang={lang} />
      </Section>
      <Section title={s.includeTitle} id="include">
        <div className="co-two even">
          <div className="co-panel"><Checklist items={s.include} /></div>
          <div className="co-panel todo-panel">
            <h3>{s.statusTitle}</h3>
            {s.status.map((p) => <p key={p}>{p}</p>)}
          </div>
        </div>
      </Section>
      <Section title={c.ui.faqTitle} id="faq" alt>
        <Faq items={s.faq} />
      </Section>
    </Shell>
  );
}
