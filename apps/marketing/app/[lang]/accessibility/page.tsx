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
  const m = CC[lang].accessibility.meta;
  return companyMeta(lang, "accessibility", m.title, m.description, true);
}

export default async function Accessibility({ params }: LangParams) {
  const lang = await getLang(params);
  const c = CC[lang];
  const p = c.accessibility;
  return (
    <Shell lang={lang} path="accessibility" crumbs={[{ name: c.ui.crumbs.accessibility, path: "accessibility" }]}>
      <Hero h1={p.meta.h1} lead={p.meta.lead}>
        <DraftNote label={c.ui.draft} text={c.ui.draftNotice} />
      </Hero>
      <Section>
        <Prose sections={p.sections} />
        <p><Link href={pagePath(lang, "contact")}>{c.ui.send}</Link></p>
      </Section>
    </Shell>
  );
}
