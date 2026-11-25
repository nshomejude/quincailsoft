import type { Metadata } from "next";
import { ContactForm } from "@/components/ContactForm";
import { Shell } from "@/components/Shell";
import { t } from "@/lib/content";
import { getLang, type LangParams } from "@/lib/route";
import { buildMetadata } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const p = t(lang).pricing;
  return buildMetadata(lang, "pricing", p.title, p.description);
}

export default async function Pricing({ params }: LangParams) {
  const lang = await getLang(params);
  const d = t(lang);
  const p = d.pricing;
  return (
    <Shell lang={lang} path="pricing" crumbs={[{ name: d.nav.pricing, path: "pricing" }]}>
      <section className="hero">
        <div className="wrap">
          <h1>{p.h1}</h1>
          <p className="lead">{p.lead}</p>
        </div>
      </section>
      <section className="section">
        <div className="wrap prose">
          <h2>{p.contactForPricing}</h2>
          {p.body.map((x) => (
            <p key={x}>{x}</p>
          ))}
          <h2>{p.formTitle}</h2>
          <ContactForm lang={lang} showBranches />
        </div>
      </section>
    </Shell>
  );
}
