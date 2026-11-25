import "@/app/product.css";
import type { Metadata } from "next";
import { Shell } from "@/components/Shell";
import { CtaSection, Hero, RelatedSection, webPageLd } from "@/components/product/ProductPage";
import { t } from "@/lib/content";
import { getData } from "@/lib/product-content";
import { getLang, type LangParams } from "@/lib/route";
import { buildMetadata, faqLd } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const f = getData(lang).faq;
  return buildMetadata(lang, "faq", f.metaTitle, f.description);
}

export default async function Faq({ params }: LangParams) {
  const lang = await getLang(params);
  const d = t(lang);
  const data = getData(lang);
  const f = data.faq;
  const ui = data.ui;
  const all = f.groups.flatMap((g) => g.items);
  return (
    <Shell
      lang={lang}
      path="faq"
      crumbs={[{ name: d.nav.faq, path: "faq" }]}
      schemas={[webPageLd(lang, "faq", f.metaTitle, f.description), faqLd(all)]}
    >
      <Hero icon="book" kicker={f.kicker} h1={f.h1} answer={f.answer} ui={ui} lang={lang} secondary={false} />
      <nav className="pr-toc" aria-label={ui.onThisPage}>
        <div className="wrap">
          <p className="pr-toc-label">{ui.onThisPage}</p>
          <ul>
            {f.groups.map((g) => (
              <li key={g.id}><a href={`#${g.id}`}>{g.title}</a></li>
            ))}
          </ul>
        </div>
      </nav>
      <section className="section">
        <div className="wrap">
          {f.groups.map((g) => (
            <div className="pr-faqgroup" id={g.id} key={g.id}>
              <h2>{g.title}</h2>
              <div className="pr-faq">
                {g.items.map((it) => (
                  <details key={it.q}>
                    <summary>{it.q}</summary>
                    <p>{it.a}</p>
                  </details>
                ))}
              </div>
            </div>
          ))}
        </div>
      </section>
      <RelatedSection lang={lang} paths={f.related} title={ui.related} />
      <CtaSection lang={lang} title={f.ctaTitle} body={f.ctaBody} ui={ui} />
    </Shell>
  );
}
