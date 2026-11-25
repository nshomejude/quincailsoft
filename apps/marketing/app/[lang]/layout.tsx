import "../globals.css";
import type { Metadata, Viewport } from "next";
import { notFound } from "next/navigation";
import type { ReactNode } from "react";
import { LANGS, SITE_URL, isLang } from "@/lib/site";

export const dynamicParams = false;

export function generateStaticParams() {
  return LANGS.map((lang) => ({ lang }));
}

export const viewport: Viewport = { width: "device-width", initialScale: 1, themeColor: "#0b4f8a" };

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  applicationName: "QuincaiSoft",
  robots: { index: true, follow: true },
};

export default async function LangLayout({ children, params }: { children: ReactNode; params: Promise<{ lang: string }> }) {
  const { lang } = await params;
  if (!isLang(lang)) notFound();
  return (
    <html lang={lang}>
      <body>{children}</body>
    </html>
  );
}
