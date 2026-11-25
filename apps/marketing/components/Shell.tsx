import Link from "next/link";
import type { ReactNode } from "react";
import { t } from "@/lib/content";
import { INDUSTRY_SLUGS, breadcrumbLd, organizationLd, pagePath, websiteLd, type Lang } from "@/lib/site";
import { Footer } from "./footer/Footer";
import { Header } from "./header/Header";
import { navigationLd } from "./header/navLd";
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
  const navItems = [
    { key: "features", label: d.nav.features },
    { key: "industries", label: d.nav.industries },
    { key: "pricing", label: d.nav.pricing },
    { key: "faq", label: d.nav.faq },
    { key: "about", label: d.nav.about },
    { key: "contact", label: d.nav.contact },
  ].map((n) => ({ ...n, href: pagePath(lang, n.key) }));
  const trail = [{ name: d.ui.home, path: pagePath(lang, "") }, ...crumbs.map((c) => ({ name: c.name, path: pagePath(lang, c.path) }))];
  return (
    <>
      <a className="skip" href="#main">{d.ui.skip}</a>
      <header className="site-header">
        <Header
          path={path}
          homeHref={pagePath(lang, "")}
          homeLabel={d.shell.navHome}
          items={navItems}
          industriesKey="industries"
          industriesLabel={d.nav.industries}
          industries={INDUSTRY_SLUGS.map((slug) => ({
            slug,
            href: pagePath(lang, `industries/${slug}`),
            name: d.industries.items[slug].name,
            summary: d.industries.items[slug].summary,
            current: path === `industries/${slug}`,
          }))}
          industriesIndex={{ href: pagePath(lang, "industries"), label: d.shell.allIndustries, current: path === "industries" }}
          cta={{ href: pagePath(lang, "contact"), label: d.shell.requestDemo }}
          lang={{ current: lang, other, otherHref: pagePath(other, path), otherName: d.ui.switchTo, group: d.shell.langGroup }}
          labels={{ menu: d.ui.menu, closeMenu: d.shell.closeMenu, main: d.shell.mainNav, mobile: d.shell.mobileNav }}
        />
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
      <Footer lang={lang} path={path} />
      <JsonLd data={organizationLd()} />
      <JsonLd data={websiteLd(lang)} />
      <JsonLd data={navigationLd(navItems.map((n) => ({ name: n.label, path: n.href })))} />
      {crumbs.length > 0 && <JsonLd data={breadcrumbLd(trail)} />}
      {schemas.map((s, i) => (
        <JsonLd key={i} data={s} />
      ))}
    </>
  );
}
