import Link from "next/link";
import type { ReactNode } from "react";
import { t } from "@/lib/content";
import { INDUSTRY_SLUGS, breadcrumbLd, organizationLd, pagePath, websiteLd, type Lang } from "@/lib/site";
import { JsonLd } from "./JsonLd";

export function Shell({
  lang,
  path,
  crumbs = [],
  schemas = [],
  children,
}: {
  lang: Lang;
  /** Language-less path of this page, used by the language switcher. */
  path: string;
  /** Crumbs after Home, each with a language-less path ("features"). */
  crumbs?: { name: string; path: string }[];
  schemas?: unknown[];
  children: ReactNode;
}) {
  const d = t(lang);
  const other: Lang = lang === "en" ? "fr" : "en";
  const nav: [string, string][] = [
    ["features", d.nav.features],
    ["industries", d.nav.industries],
    ["pricing", d.nav.pricing],
    ["about", d.nav.about],
    ["faq", d.nav.faq],
    ["contact", d.nav.contact],
  ];
  const trail = [{ name: d.ui.home, path: pagePath(lang, "") }, ...crumbs.map((c) => ({ name: c.name, path: pagePath(lang, c.path) }))];
  const links = (
    <ul>
      {nav.map(([p, label]) => (
        <li key={p}><Link href={pagePath(lang, p)}>{label}</Link></li>
      ))}
    </ul>
  );

  return (
    <>
      <a className="skip" href="#main">{d.ui.skip}</a>
      <header className="site-header">
        <div className="wrap bar">
          <Link href={pagePath(lang, "")} className="brand" aria-label="QuincaiSoft">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src="/logo.png" alt="QuincaiSoft" width={120} height={40} />
          </Link>
          <nav className="desktop-nav" aria-label="Main">{links}</nav>
          <Link className="lang" href={pagePath(other, path)} hrefLang={other} lang={other} aria-label={`${d.ui.language}: ${d.ui.switchTo}`}>
            {d.ui.switchTo}
          </Link>
          <details className="menu">
            <summary>{d.ui.menu}</summary>
            <nav aria-label="Main">{links}</nav>
          </details>
        </div>
      </header>
      {crumbs.length > 0 && (
        <nav className="wrap crumbs" aria-label={d.ui.breadcrumb}>
          <ol>
            {trail.map((c, i) => (
              <li key={c.path}>
                {i < trail.length - 1 ? <Link href={c.path}>{c.name}</Link> : <span aria-current="page">{c.name}</span>}
              </li>
            ))}
          </ol>
        </nav>
      )}
      <main id="main" tabIndex={-1}>{children}</main>
      <footer className="site-footer">
        <div className="wrap">
          <p>{d.ui.footerTagline}</p>
          <ul>
            {(["features", "pricing", "about", "faq", "contact", "privacy", "terms"] as const).map((p) => (
              <li key={p}><Link href={pagePath(lang, p)}>{d.nav[p]}</Link></li>
            ))}
            {INDUSTRY_SLUGS.map((s) => (
              <li key={s}><Link href={pagePath(lang, `industries/${s}`)}>{d.industries.items[s].name}</Link></li>
            ))}
          </ul>
          <p className="small">{d.ui.footerLegal}</p>
        </div>
      </footer>
      <JsonLd data={organizationLd()} />
      <JsonLd data={websiteLd(lang)} />
      {crumbs.length > 0 && <JsonLd data={breadcrumbLd(trail)} />}
      {schemas.map((s, i) => (
        <JsonLd key={i} data={s} />
      ))}
    </>
  );
}
