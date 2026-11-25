import type { Metadata } from "next";
import { ContactForm } from "@/components/ContactForm";
import { ProductPage } from "@/components/product/ProductPage";
import { t } from "@/lib/content";
import { getData } from "@/lib/product-content";
import { getLang, type LangParams } from "@/lib/route";
import { buildMetadata } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const p = getData(lang).pricing;
  return buildMetadata(lang, "pricing", p.metaTitle, p.description);
}

export default async function Pricing({ params }: LangParams) {
  const lang = await getLang(params);
  const d = t(lang);
  const p = getData(lang).pricing;
  return (
    <ProductPage
      lang={lang}
      path="pricing"
      data={p}
      crumbs={[{ name: d.nav.pricing, path: "pricing" }]}
      afterBlocks={
        <section className="section pr-sec" id="quote">
          <div className="wrap">
            <div className="pr-sec-head">
              <h2>{p.formTitle}</h2>
            </div>
            <ContactForm lang={lang} showBranches />
          </div>
        </section>
      }
    />
  );
}
