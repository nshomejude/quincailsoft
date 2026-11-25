import type { Metadata } from "next";
import { Cards } from "@/components/Cards";
import { Shell } from "@/components/Shell";
import { t } from "@/lib/content";
import { getLang, type LangParams } from "@/lib/route";
import { buildMetadata, softwareLd } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const f = t(lang).features;
  return buildMetadata(lang, "features", f.title, f.description);
}

export default async function Features({ params }: LangParams) {
  const lang = await getLang(params);
  const d = t(lang);
  const f = d.features;
  return (
    <Shell lang={lang} path="features" crumbs={[{ name: d.nav.features, path: "features" }]} schemas={[softwareLd(lang, d.softwareDescription)]}>
      <section className="hero">
        <div className="wrap">
          <h1>{f.h1}</h1>
          <p className="lead">{f.lead}</p>
        </div>
      </section>
      <section className="section">
        <div className="wrap"><Cards items={f.groups} headingLevel={2} /></div>
      </section>
      <section className="section alt" id="status">
        <div className="wrap">
          <h2>{f.statusTitle}</h2>
          <Cards items={f.status} />
        </div>
      </section>
    </Shell>
  );
}
