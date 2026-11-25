import type { Metadata } from "next";
import Link from "next/link";
import { CompanyForm } from "@/components/company/CompanyForm";
import { Shell } from "@/components/Shell";
import { CardGrid, Checklist, Faq, Hero, Section, Steps } from "@/components/company/parts";
import { companyMeta } from "@/components/company/meta";
import { CC } from "@/lib/company-content";
import { getLang, type LangParams } from "@/lib/route";
import { abs, faqLd, pagePath } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const m = CC[lang].demo.meta;
  return companyMeta(lang, "request-a-demo", m.title, m.description);
}

export default async function RequestDemo({ params }: LangParams) {
  const lang = await getLang(params);
  const c = CC[lang];
  const d = c.demo;
  const url = abs(pagePath(lang, "request-a-demo"));
  const schemas = [
    { "@context": "https://schema.org", "@type": "WebPage", "@id": `${url}#webpage`, url, name: d.meta.h1, description: d.meta.description, inLanguage: lang, isPartOf: { "@id": abs("/#website") }, about: { "@id": abs("/#software") } },
    faqLd(d.faq),
  ];
  return (
    <Shell lang={lang} path="request-a-demo" crumbs={[{ name: c.ui.crumbs.demo, path: "request-a-demo" }]} schemas={schemas}>
      <Hero h1={d.meta.h1} lead={d.meta.lead} />
      <Section title={d.coversTitle} intro={d.coversIntro} id="covers">
        <CardGrid items={d.covers} lang={lang} />
      </Section>
      <Section title={d.formTitle} id="form" alt>
        <div className="co-two">
          <div className="co-panel">
            <CompanyForm lang={lang} variant="demo" />
          </div>
          <div className="co-panel">
            <h3>{d.prepareTitle}</h3>
            <Checklist items={d.prepare} />
          </div>
        </div>
      </Section>
      <Section title={d.stepsTitle} id="steps">
        <Steps items={d.steps} />
      </Section>
      <Section title={d.honestTitle} id="honest" alt>
        <div className="co-panel todo-panel"><Checklist items={d.honest} /></div>
        <p className="co-intro" style={{ marginTop: 16 }}>
          <Link href={pagePath(lang, "features")}>{lang === "en" ? "See current features and status" : "Voir les fonctionnalités et leur état"}</Link>
        </p>
      </Section>
      <Section title={c.ui.faqTitle} id="faq">
        <Faq items={d.faq} />
      </Section>
    </Shell>
  );
}
