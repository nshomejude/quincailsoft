import Link from "next/link";
import type { Metadata } from "next";
import { Shell } from "@/components/Shell";
import { t } from "@/lib/content";
import { getLang, type LangParams } from "@/lib/route";
import { INDUSTRY_SLUGS, buildMetadata, pagePath } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const i = t(lang).industries;
  return buildMetadata(lang, "industries", i.title, i.description);
}

export default async function Industries({ params }: LangParams) {
  const lang = await getLang(params);
  const d = t(lang);
  const i = d.industries;
  return (
    <Shell lang={lang} path="industries" crumbs={[{ name: d.nav.industries, path: "industries" }]}>
      <section className="hero">
        <div className="wrap">
          <h1>{i.h1}</h1>
          <p className="lead">{i.lead}</p>
        </div>
      </section>
      <section className="section">
        <div className="wrap">
          <ul className="cards">
            {INDUSTRY_SLUGS.map((s) => (
              <li key={s}>
                <h2><Link href={pagePath(lang, `industries/${s}`)}>{i.items[s].name}</Link></h2>
                <p>{i.items[s].summary}</p>
              </li>
            ))}
          </ul>
        </div>
      </section>
    </Shell>
  );
}
