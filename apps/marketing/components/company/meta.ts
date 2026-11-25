import type { Metadata } from "next";
import { buildMetadata, type Lang } from "@/lib/site";

export function companyMeta(lang: Lang, path: string, title: string, description: string, noindex = false): Metadata {
  const m = buildMetadata(lang, path, title, description);
  return noindex ? { ...m, robots: { index: false, follow: true } } : m;
}
