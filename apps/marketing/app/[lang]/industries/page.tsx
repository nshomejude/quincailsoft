import Link from "next/link";
import type { Metadata } from "next";
import { ArrowRight } from "lucide-react";
import { ProductPage } from "@/components/product/ProductPage";
import { SectionHead } from "@/components/product/Blocks";
import { t } from "@/lib/content";
import { getData } from "@/lib/product-content";
import { getLang, type LangParams } from "@/lib/route";
import { INDUSTRY_SLUGS, buildMetadata, pagePath } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const i = getData(lang).industries.index;
  return buildMetadata(lang, "industries", i.metaTitle, i.description);
}

export default async function Industries({ params }: LangParams) {
  const lang = await getLang(params);
  const d = t(lang);
  const idx = getData(lang).industries.index;
  return (
    <ProductPage
      lang={lang}
      path="industries"
      data={idx}
      crumbs={[{ name: d.nav.industries, path: "industries" }]}
      afterHero={
        <section className="section pr-sec" id="sectors">
          <div className="wrap">
            <SectionHead id="sectors-title" title={idx.sectorsTitle} />
            <ul className="pr-guide-list">
              {INDUSTRY_SLUGS.map((s) => (
                <li key={s}>
                  <Link className="pr-card" href={pagePath(lang, `industries/${s}`)}>
                    <h3>{d.industries.items[s].name}</h3>
                    <p>{idx.sectors[s].summary}</p>
                    <span className="pr-readmore">
                      {idx.sectorLink}
                      <ArrowRight size={18} strokeWidth={1.75} color="currentColor" fill="none" aria-hidden="true" focusable="false" />
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        </section>
      }
    />
  );
}
