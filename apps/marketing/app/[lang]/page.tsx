import type { Metadata } from "next";
import { Shell } from "@/components/Shell";
import { HomePage, homeSchemas } from "@/components/home/HomePage";
import { t } from "@/lib/content";
import { homeContent } from "@/lib/home-content";
import { getLang, type LangParams } from "@/lib/route";
import { buildMetadata, softwareLd } from "@/lib/site";

export async function generateMetadata({ params }: LangParams): Promise<Metadata> {
  const lang = await getLang(params);
  const m = homeContent(lang).meta;
  return buildMetadata(lang, "", m.title, m.description);
}

export default async function Home({ params }: LangParams) {
  const lang = await getLang(params);
  const d = t(lang);
  return (
    <Shell lang={lang} path="" schemas={[softwareLd(lang, d.softwareDescription), ...homeSchemas(lang)]}>
      <HomePage lang={lang} />
    </Shell>
  );
}
