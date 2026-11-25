import type { Metadata } from "next";
import Link from "next/link";
import { CompanyForm } from "@/components/company/CompanyForm";
import { Shell } from "@/components/Shell";
import { CardGrid, Hero, Section, Steps } from "@/components/company/parts";
import { companyMeta } from "@/components/company/meta";
import { CC } from "@/lib/company-content";
import { getLang, type LangParams } from "@/lib/route";
import { CONTACT_EMAIL, abs, pagePath } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const m = CC[lang].contact.meta;
  return companyMeta(lang, "contact", m.title, m.description);
}

export default async function Contact({ params }: LangParams) {
  const lang = await getLang(params);
  const c = CC[lang];
  const k = c.contact;
  const url = abs(pagePath(lang, "contact"));
  const options = k.options.map((o) => (o.href ? o : { ...o, href: `mailto:${CONTACT_EMAIL}?subject=${encodeURIComponent(k.pressSubject)}` }));
  const schemas = [
    {
      "@context": "https://schema.org",
      "@type": "ContactPage",
      "@id": `${url}#webpage`,
      url,
      name: k.meta.h1,
      description: k.meta.description,
      inLanguage: lang,
      about: { "@id": abs("/#organization") },
      mainEntity: {
        "@type": "Organization",
        "@id": abs("/#organization"),
        name: "Opesware Technologies",
        contactPoint: ["sales", "customer support", "partnerships", "press"].map((t) => ({
          "@type": "ContactPoint", contactType: t, email: CONTACT_EMAIL, availableLanguage: ["English", "French"],
        })),
      },
    },
  ];
  return (
    <Shell lang={lang} path="contact" crumbs={[{ name: c.ui.crumbs.contact, path: "contact" }]} schemas={schemas}>
      <Hero h1={k.meta.h1} lead={k.meta.lead} />
      <Section title={k.optionsTitle} intro={k.optionsIntro} id="options">
        <CardGrid items={options} lang={lang} />
      </Section>
      <Section title={k.formTitle} id="form" alt>
        <div className="co-two">
          <div className="co-panel">
            <CompanyForm lang={lang} variant="contact" />
          </div>
          <div className="co-panel">
            <h3>{k.asideTitle}</h3>
            <p>{k.asideBody}</p>
            <p><a className="co-mail" href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a></p>
            <p className="small">{k.asideNote}</p>
            <p>{k.faqLead} <Link href={pagePath(lang, "faq")}>{c.ui.faqLink}</Link>.</p>
          </div>
        </div>
      </Section>
      <Section title={k.expectTitle} intro={k.expectIntro} id="expect">
        <Steps items={k.expect} />
      </Section>
    </Shell>
  );
}
