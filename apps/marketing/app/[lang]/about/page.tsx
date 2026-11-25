import type { Metadata } from "next";
import { Shell } from "@/components/Shell";
import { t } from "@/lib/content";
import { getLang, type LangParams } from "@/lib/route";
import { buildMetadata } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const a = t(lang).about;
  return buildMetadata(lang, "about", a.title, a.description);
}

export default async function About({ params }: LangParams) {
  const lang = await getLang(params);
  const d = t(lang);
  const a = d.about;
  return (
    <Shell lang={lang} path="about" crumbs={[{ name: d.nav.about, path: "about" }]}>
      <section className="hero">
        <div className="wrap">
          <h1>{a.h1}</h1>
          <p className="lead">{a.lead}</p>
        </div>
      </section>
      <section className="section">
        <div className="wrap prose">
          {a.sections.map((s) => (
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
