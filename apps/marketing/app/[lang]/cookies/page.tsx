import type { Metadata } from "next";
import Link from "next/link";
import { Shell } from "@/components/Shell";
import { DraftNote, Hero, Prose, Section } from "@/components/company/parts";
import { companyMeta } from "@/components/company/meta";
import { CC } from "@/lib/company-content";
import { getLang, type LangParams } from "@/lib/route";
import { pagePath } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const m = CC[lang].cookies.meta;
  return companyMeta(lang, "cookies", m.title, m.description, true);
}

export default async function Cookies({ params }: LangParams) {
  const lang = await getLang(params);
  const c = CC[lang];
  const p = c.cookies;
  return (
    <Shell lang={lang} path="cookies" crumbs={[{ name: c.ui.crumbs.cookies, path: "cookies" }]}>
      <Hero h1={p.meta.h1} lead={p.meta.lead}>
        <DraftNote label={c.ui.draft} text={c.ui.draftNotice} />
      </Hero>
      <Section>
        <Prose sections={p.sections} />
        <p><Link href={pagePath(lang, "privacy")}>{lang === "en" ? "Privacy notice" : "Notice de confidentialité"}</Link></p>
      </Section>
    </Shell>
  );
}
