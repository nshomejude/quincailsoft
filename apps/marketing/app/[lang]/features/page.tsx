import "@/app/product.css";
import type { Metadata } from "next";
import { Shell } from "@/components/Shell";
import { CtaSection, Hero, RelatedSection, webPageLd } from "@/components/product/ProductPage";
import { Blocks, ModuleCard, SectionHead, StatusTag } from "@/components/product/Blocks";
import { t } from "@/lib/content";
import { getData } from "@/lib/product-content";
import { getLang, type LangParams } from "@/lib/route";
import { buildMetadata, softwareLd } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const f = getData(lang).features;
  return buildMetadata(lang, "features", f.metaTitle, f.description);
}

export default async function Features({ params }: LangParams) {
  const lang = await getLang(params);
  const d = t(lang);
  const data = getData(lang);
  const f = data.features;
  const ui = data.ui;
  return (
    <Shell
      lang={lang}
      path="features"
      crumbs={[{ name: d.nav.features, path: "features" }]}
      schemas={[softwareLd(lang, d.softwareDescription), webPageLd(lang, "features", f.metaTitle, f.description)]}
    >
      <Hero icon="list" kicker={f.kicker} h1={f.h1} answer={f.answer} ui={ui} lang={lang} secondary={false} />
      <nav className="pr-toc" aria-label={ui.onThisPage}>
        <div className="wrap">
          <p className="pr-toc-label">{ui.onThisPage}</p>
          <ul>
            {f.modules.map((m) => (
              <li key={m.id}><a href={`#${m.id}`}>{m.title}</a></li>
            ))}
          </ul>
        </div>
      </nav>
      <section className="section pr-sec" id="overview">
        <div className="wrap">
          <SectionHead title={f.summaryTitle} />
          <div className="pr-tablewrap" role="region" aria-label={f.summaryCaption} tabIndex={0}>
            <table className="pr-table">
              <caption>{f.summaryCaption}</caption>
              <thead>
                <tr>
                  {f.summaryHead.map((h) => (
                    <th key={h} scope="col">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {f.modules.map((m) => (
                  <tr key={m.id}>
                    <th scope="row"><a href={`#${m.id}`}>{m.title}</a></th>
                    <td><StatusTag status={m.status} ui={ui} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </section>
      <section className="section alt pr-sec" id="status">
        <div className="wrap">
          <SectionHead title={f.statusLegendTitle} />
          <ul className="pr-grid">
            {f.statusLegend.map((s) => (
              <li key={s.status} className="pr-card">
                <StatusTag status={s.status} ui={ui} />
                <p>{s.body}</p>
              </li>
            ))}
          </ul>
        </div>
      </section>
      <section className="section pr-sec" id="modules">
        <div className="wrap pr-modules">
          {f.modules.map((m) => (
            <ModuleCard key={m.id} m={m} ui={ui} />
          ))}
        </div>
      </section>
      <Blocks
        ui={ui}
        blocks={[
          { kind: "cards", id: "more", alt: true, title: f.moreTitle, items: f.more },
          { kind: "limits", id: "limits", title: ui.limitsTitle, items: f.limits },
        ]}
      />
      <RelatedSection lang={lang} paths={f.related} title={ui.related} />
      <CtaSection lang={lang} title={f.ctaTitle} body={f.ctaBody} ui={ui} />
    </Shell>
  );
}
