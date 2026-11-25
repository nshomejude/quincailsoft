import type { Metadata } from "next";
import { Shell } from "@/components/Shell";
import { t } from "@/lib/content";
import { getLang, type LangParams } from "@/lib/route";
import { buildMetadata } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const p = t(lang).terms;
  return { ...buildMetadata(lang, "terms", p.title, p.description), robots: { index: false, follow: true } };
}

export default async function Terms({ params }: LangParams) {
  const lang = await getLang(params);
  const d = t(lang);
  const p = d.terms;
  return (
    <Shell lang={lang} path="terms" crumbs={[{ name: d.nav.terms, path: "terms" }]}>
      <section className="hero">
        <div className="wrap">
          <h1>{p.h1}</h1>
          <p className="notice" role="note"><strong>{d.ui.draft}.</strong> {p.draftNotice}</p>
        </div>
      </section>
      <section className="section">
        <div className="wrap prose">
          {p.sections.map((s) => (
            <section key={s.title}>
              <h2>{s.title}</h2>
              <p>{s.body}</p>
            </section>
          ))}
        </div>
      </section>
    </Shell>
  );
}
