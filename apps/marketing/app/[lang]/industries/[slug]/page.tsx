import Link from "next/link";
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Cards } from "@/components/Cards";
import { Shell } from "@/components/Shell";
import { t } from "@/lib/content";
import { INDUSTRY_SLUGS, LANGS, buildMetadata, pagePath, type IndustrySlug } from "@/lib/site";
import { getLang } from "@/lib/route";

type P = { params: Promise<{ lang: string; slug: string }> };

export const dynamicParams = false;

export function generateStaticParams() {
  return LANGS.flatMap((lang) => INDUSTRY_SLUGS.map((slug) => ({ lang, slug })));
}

async function resolve(params: P["params"]) {
  const p = await params;
  const lang = await getLang(Promise.resolve(p));
  if (!(INDUSTRY_SLUGS as readonly string[]).includes(p.slug)) notFound();
  return { lang, slug: p.slug as IndustrySlug };
}

export async function generateMetadata({ params }: P): Promise<Metadata> {
  const { lang, slug } = await resolve(params);
  const it = t(lang).industries.items[slug];
  return buildMetadata(lang, `industries/${slug}`, `${it.name} | QuincaiSoft`, it.metaDescription);
}

export default async function Industry({ params }: P) {
  const { lang, slug } = await resolve(params);
  const d = t(lang);
  const it = d.industries.items[slug];
  return (
    <Shell
      lang={lang}
      path={`industries/${slug}`}
      crumbs={[
        { name: d.nav.industries, path: "industries" },
        { name: it.name, path: `industries/${slug}` },
      ]}
    >
      <section className="hero">
        <div className="wrap">
          <h1>{it.name}</h1>
          <p className="lead">{it.intro}</p>
          <div className="actions">
            <Link className="btn" href={pagePath(lang, "contact")}>{d.ui.cta}</Link>
            <Link className="btn secondary" href={pagePath(lang, "features")}>{d.ui.ctaSecondary}</Link>
          </div>
        </div>
      </section>
      <section className="section">
        <div className="wrap">
          <h2>{d.industries.needsTitle}</h2>
          <Cards items={it.needs} />
        </div>
      </section>
      <section className="section alt">
        <div className="wrap">
          <h2>{d.industries.h1}</h2>
          <p>{d.industries.lead}</p>
          <ul>
            {INDUSTRY_SLUGS.filter((s) => s !== slug).map((s) => (
              <li key={s}><Link href={pagePath(lang, `industries/${s}`)}>{d.industries.items[s].name}</Link></li>
            ))}
          </ul>
        </div>
      </section>
    </Shell>
  );
}
