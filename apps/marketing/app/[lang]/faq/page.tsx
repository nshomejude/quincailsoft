import type { Metadata } from "next";
import { Shell } from "@/components/Shell";
import { t } from "@/lib/content";
import { getLang, type LangParams } from "@/lib/route";
import { buildMetadata, faqLd } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const f = t(lang).faq;
  return buildMetadata(lang, "faq", f.title, f.description);
}

export default async function Faq({ params }: LangParams) {
  const lang = await getLang(params);
  const d = t(lang);
  const f = d.faq;
  return (
    <Shell lang={lang} path="faq" crumbs={[{ name: d.nav.faq, path: "faq" }]} schemas={[faqLd(f.items)]}>
      <section className="hero">
        <div className="wrap">
          <h1>{f.h1}</h1>
          <p className="lead">{f.lead}</p>
        </div>
      </section>
      <section className="section">
        <div className="wrap prose faq">
          {f.items.map((it) => (
            <details key={it.q}>
              <summary><h2 style={{ display: "inline", fontSize: "inherit", margin: 0 }}>{it.q}</h2></summary>
              <p>{it.a}</p>
            </details>
          ))}
        </div>
      </section>
    </Shell>
  );
}
