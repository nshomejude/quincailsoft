import type { Metadata } from "next";
import { ContactForm } from "@/components/ContactForm";
import { Shell } from "@/components/Shell";
import { t } from "@/lib/content";
import { getLang, type LangParams } from "@/lib/route";
import { CONTACT_EMAIL, buildMetadata } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const c = t(lang).contact;
  return buildMetadata(lang, "contact", c.title, c.description);
}

export default async function Contact({ params }: LangParams) {
  const lang = await getLang(params);
  const d = t(lang);
  const c = d.contact;
  return (
    <Shell lang={lang} path="contact" crumbs={[{ name: d.nav.contact, path: "contact" }]}>
      <section className="hero">
        <div className="wrap">
          <h1>{c.h1}</h1>
          <p className="lead">{c.lead}</p>
        </div>
      </section>
      <section className="section">
        <div className="wrap prose">
          <p>{c.body}</p>
          <p>
            {c.emailLabel}: <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>
          </p>
          <ContactForm lang={lang} />
        </div>
      </section>
    </Shell>
  );
}
