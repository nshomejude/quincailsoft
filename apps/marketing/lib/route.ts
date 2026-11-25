import { notFound } from "next/navigation";
import { isLang, type Lang } from "./site";

export type LangParams = { params: Promise<{ lang: string }> };

export async function getLang(p: LangParams["params"]): Promise<Lang> {
  const { lang } = await p;
  if (!isLang(lang)) notFound();
  return lang;
}
