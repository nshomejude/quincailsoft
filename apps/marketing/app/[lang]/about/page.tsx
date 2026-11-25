import type { Metadata } from "next";
import Link from "next/link";
import { Shell } from "@/components/Shell";
import { CardGrid, Checklist, Hero, Section, Steps } from "@/components/company/parts";
import { companyMeta } from "@/components/company/meta";
import { CC, OPESWARE_URL } from "@/lib/company-content";
import { getLang, type LangParams } from "@/lib/route";
import { abs, pagePath } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const m = CC[lang].about.meta;
  return companyMeta(lang, "about", m.title, m.description);
}

export default async function About({ params }: LangParams) {
  const lang = await getLang(params);
  const c = CC[lang];
  const a = c.about;
  const url = abs(pagePath(lang, "about"));
  const opesware = { "@type": "Organization", "@id": `${OPESWARE_URL}/#organization`, name: "Opesware Technologies", url: OPESWARE_URL };
  const schemas = [
    {
      "@context": "https://schema.org",
      "@type": "AboutPage",
      "@id": `${url}#webpage`,
      url,
      name: a.jsonName,
      description: a.meta.description,
      inLanguage: lang,
      mainEntity: {
        "@type": "Organization",
        "@id": abs("/#organization"),
        name: "QuincaiSoft",
        url: abs(`/${lang}`),
        description: a.meta.description,
        parentOrganization: opesware,
        areaServed: { "@type": "Country", name: "Cameroon" },
        knowsLanguage: ["en", "fr"],
      },
      creator: opesware,
      publisher: opesware,
    },
  ];
  return (
    <Shell lang={lang} path="about" crumbs={[{ name: c.ui.crumbs.about, path: "about" }]} schemas={schemas}>
      <Hero h1={a.meta.h1} lead={a.meta.lead} />
      <Section title={a.whoTitle} id="who">
        <div className="co-two">
          <div className="co-prose">
            {a.who.map((p) => <p key={p}>{p}</p>)}
            <p>
              <a href={OPESWARE_URL} rel="noopener">{a.visit}</a> · <Link href={pagePath(lang, "opesware")}>{a.opesPage}</Link>
            </p>
          </div>
          <div className="co-panel">
            <h3>{a.missionTitle}</h3>
            <p>{a.mission}</p>
          </div>
        </div>
      </Section>
      <Section title={a.problemTitle} id="problem" alt>
        <div className="co-prose">{a.problem.map((p) => <p key={p}>{p}</p>)}</div>
      </Section>
      <Section title={a.principlesTitle} intro={a.principlesIntro} id="principles">
        <CardGrid items={a.principles} lang={lang} />
      </Section>
      <Section title={a.statusTitle} intro={a.statusIntro} id="status" alt>
        <div className="co-two even">
          <div className="co-panel">
            <h3>{a.existsTitle}</h3>
            <Checklist items={a.exists} />
          </div>
          <div className="co-panel todo-panel">
            <h3>{a.pendingTitle}</h3>
            <Checklist items={a.pending} />
          </div>
        </div>
      </Section>
      <Section title={a.pilotsTitle} intro={a.pilotsIntro} id="pilots">
        <Steps items={a.pilots} />
      </Section>
      <Section title={a.ctaTitle} id="cta" alt>
        <p className="co-intro">{a.ctaBody}</p>
        <div className="actions">
          <Link className="btn" href={pagePath(lang, "request-a-demo")}>{c.ui.demo}</Link>
          <Link className="btn secondary" href={pagePath(lang, "contact")}>{c.ui.send}</Link>
        </div>
      </Section>
    </Shell>
  );
}
