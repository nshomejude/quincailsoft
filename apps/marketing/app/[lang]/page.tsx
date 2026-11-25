import Link from "next/link";
import type { Metadata } from "next";
import { Cards } from "@/components/Cards";
import { Shell } from "@/components/Shell";
import { t } from "@/lib/content";
import { getLang, type LangParams } from "@/lib/route";
import { INDUSTRY_SLUGS, buildMetadata, pagePath, softwareLd } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const d = t(lang);
  return buildMetadata(lang, "", d.home.title, d.home.description);
}

export default async function Home({ params }: LangParams) {
  const lang = await getLang(params);
  const d = t(lang);
  const h = d.home;
  return (
    <Shell lang={lang} path="" schemas={[softwareLd(lang, d.softwareDescription)]}>
      <section className="hero">
        <div className="wrap">
          <h1>{h.h1}</h1>
          <p className="lead">{h.lead}</p>
          <div className="actions">
            <Link className="btn" href={pagePath(lang, "contact")}>{d.ui.cta}</Link>
            <Link className="btn secondary" href={pagePath(lang, "features")}>{d.ui.ctaSecondary}</Link>
          </div>
        </div>
      </section>
      <section className="section">
        <div className="wrap">
          <h2>{h.valueTitle}</h2>
          <Cards items={h.value} />
        </div>
      </section>
      <section className="section alt">
        <div className="wrap">
          <h2>{h.industriesTitle}</h2>
          <ul className="cards">
            {INDUSTRY_SLUGS.map((s) => (
              <li key={s}>
                <h3><Link href={pagePath(lang, `industries/${s}`)}>{d.industries.items[s].name}</Link></h3>
                <p>{d.industries.items[s].summary}</p>
              </li>
            ))}
          </ul>
        </div>
      </section>
      <section className="section">
        <div className="wrap">
          <div className="notice">
            <h2>{h.honestyTitle}</h2>
            <ul>
              {h.honesty.map((x) => (
                <li key={x}>{x}</li>
              ))}
            </ul>
          </div>
        </div>
      </section>
      <section className="section alt">
        <div className="wrap">
          <h2>{h.ctaTitle}</h2>
          <p className="lead">{h.ctaBody}</p>
          <div className="actions">
            <Link className="btn" href={pagePath(lang, "contact")}>{d.ui.cta}</Link>
            <Link className="btn secondary" href={pagePath(lang, "pricing")}>{d.pricing.contactForPricing}</Link>
          </div>
        </div>
      </section>
    </Shell>
  );
}
