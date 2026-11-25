import Link from "next/link";
import { Languages, Mail } from "lucide-react";
import { t } from "@/lib/content";
import { CONTACT_EMAIL, INDUSTRY_SLUGS, pagePath, type Lang } from "@/lib/site";

const ICON = { size: 18, strokeWidth: 1.75, "aria-hidden": true, focusable: false } as const;

export function Footer({ lang, path }: { lang: Lang; path: string }) {
  const d = t(lang);
  const s = d.shell;
  const link = (p: string, label: string) => (
    <li key={p}>
      <Link href={pagePath(lang, p)} aria-current={path === p ? "page" : undefined}>{label}</Link>
    </li>
  );
  const langs: { code: Lang; name: string }[] = [
    { code: "en", name: "English" },
    { code: "fr", name: "Français" },
  ];

  return (
    <footer className="site-footer">
      <div className="wrap">
        <div className="f-grid">
          <div className="f-brand">
            <Link href={pagePath(lang, "")} className="f-logo" aria-label={s.navHome}>QuincaiSoft</Link>
            <p>{d.ui.footerTagline}</p>
            <p className="small">{s.status}</p>
            <p className="f-mail">
              <Mail {...ICON} />
              <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>
            </p>
          </div>
          <nav className="f-nav" aria-label={s.footerNav}>
            <div className="f-col">
              <h2>{s.colProduct}</h2>
              <ul>{(["features", "pricing", "faq"] as const).map((p) => link(p, d.nav[p]))}</ul>
            </div>
            <div className="f-col">
              <h2>{s.colIndustries}</h2>
              <ul>
                {INDUSTRY_SLUGS.map((slug) => link(`industries/${slug}`, d.industries.items[slug].name))}
                {link("industries", s.allIndustries)}
              </ul>
            </div>
            <div className="f-col">
              <h2>{s.colCompany}</h2>
              <ul>{(["about", "contact"] as const).map((p) => link(p, d.nav[p]))}</ul>
            </div>
            <div className="f-col">
              <h2>{s.colLegal}</h2>
              <ul>{(["privacy", "terms"] as const).map((p) => link(p, d.nav[p]))}</ul>
            </div>
          </nav>
        </div>
        <div className="f-bottom">
          <div className="f-notes">
            <p className="small">{d.ui.footerLegal} {s.notes}</p>
            <p className="small">© {new Date().getFullYear()} Opesware Technologies. {s.rights}</p>
          </div>
          <div className="f-langs" role="group" aria-label={s.langsLabel}>
            <Languages {...ICON} />
            {langs.map((l) =>
              l.code === lang ? (
                <span key={l.code} aria-current="true">{l.name}</span>
              ) : (
                <Link key={l.code} href={pagePath(l.code, path)} hrefLang={l.code} lang={l.code}>{l.name}</Link>
              ),
            )}
          </div>
        </div>
      </div>
    </footer>
  );
}
