import type { Metadata } from "next";
import { Shell } from "@/components/Shell";
import { t } from "@/lib/content";
import { getLang, type LangParams } from "@/lib/route";
import { buildMetadata } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const p = t(lang).privacy;
  return { ...buildMetadata(lang, "privacy", p.title, p.description), robots: { index: false, follow: true } };
}

export default async function Privacy({ params }: LangParams) {
  const lang = await getLang(params);
  const d = t(lang);
  const p = d.privacy;
  return (
    <Shell lang={lang} path="privacy" crumbs={[{ name: d.nav.privacy, path: "privacy" }]}>
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
