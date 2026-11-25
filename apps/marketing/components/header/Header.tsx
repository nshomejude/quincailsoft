"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { ChevronDown, HardHat, Languages, Menu, Package, Truck, X } from "lucide-react";
import type { IndustrySlug } from "@/lib/site";

const ICONS: Record<IndustrySlug, typeof Package> = {
  "hardware-shops": Package,
  "construction-materials": HardHat,
  "wholesale-distribution": Truck,
};
const ICON = { size: 20, strokeWidth: 1.75, "aria-hidden": true, focusable: false } as const;

export type HeaderProps = {
  path: string;
  homeHref: string;
  homeLabel: string;
  items: { key: string; href: string; label: string }[];
  industries: { slug: IndustrySlug; href: string; name: string; summary: string; current: boolean }[];
  industriesKey: string;
  industriesLabel: string;
  industriesIndex: { href: string; label: string; current: boolean };
  cta: { href: string; label: string };
  lang: { current: string; other: string; otherHref: string; otherName: string; group: string };
  labels: { menu: string; closeMenu: string; main: string; mobile: string };
};

function LangSwitch({ lang, className }: { lang: HeaderProps["lang"]; className: string }) {
  return (
    <div className={`langsw ${className}`} role="group" aria-label={lang.group}>
      <Languages {...ICON} />
      <span className="langsw-cur" aria-current="true">{lang.current.toUpperCase()}</span>
      <Link href={lang.otherHref} hrefLang={lang.other} lang={lang.other} aria-label={lang.otherName} title={lang.otherName}>
        {lang.other.toUpperCase()}
      </Link>
    </div>
  );
}

export function Header(p: HeaderProps) {
  const [ddOpen, setDd] = useState(false);
  const [menuOpen, setMenu] = useState(false);
  const ddRef = useRef<HTMLDetailsElement>(null);
  const menuRef = useRef<HTMLDetailsElement>(null);
  const closeAll = useCallback(() => { setDd(false); setMenu(false); }, []);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key !== "Escape") return;
      if (ddRef.current?.open) { setDd(false); ddRef.current.querySelector("summary")?.focus(); }
      else if (menuRef.current?.open) { setMenu(false); menuRef.current.querySelector("summary")?.focus(); }
    };
    const onDown = (e: PointerEvent) => {
      if (ddRef.current && !ddRef.current.contains(e.target as Node)) setDd(false);
    };
    const mq = window.matchMedia("(min-width: 1100px)");
    const onMq = () => { if (mq.matches) setMenu(false); };
    document.addEventListener("keydown", onKey);
    document.addEventListener("pointerdown", onDown);
    mq.addEventListener("change", onMq);
    return () => {
      document.removeEventListener("keydown", onKey);
      document.removeEventListener("pointerdown", onDown);
      mq.removeEventListener("change", onMq);
    };
  }, []);

  // Keep the page from scrolling underneath the open mobile menu.
  useEffect(() => {
    if (!menuOpen) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => { document.body.style.overflow = prev; };
  }, [menuOpen]);

  const industriesActive = p.path === "industries" || p.path.startsWith("industries/");
  const industryLinks = (cls: string) => (
    <ul className={cls}>
      {p.industries.map((i) => {
        const Icon = ICONS[i.slug];
        return (
          <li key={i.slug}>
            <Link href={i.href} aria-current={i.current ? "page" : undefined} onClick={closeAll}>
              <Icon {...ICON} />
              <span><strong>{i.name}</strong><small>{i.summary}</small></span>
            </Link>
          </li>
        );
      })}
      <li className="all">
        <Link href={p.industriesIndex.href} aria-current={p.industriesIndex.current ? "page" : undefined} onClick={closeAll}>
          {p.industriesIndex.label}
        </Link>
      </li>
    </ul>
  );

  return (
    <div className="wrap bar">
      <Link href={p.homeHref} className="brand" aria-label={p.homeLabel}>
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src="/logo.png" alt="QuincaiSoft" width={120} height={40} />
      </Link>

      <nav className="desktop-nav" aria-label={p.labels.main}>
        <ul>
          {p.items.map((it) =>
            it.key === p.industriesKey ? (
              <li key={it.key} className="has-dd">
                <details
                  ref={ddRef}
                  className="dd"
                  open={ddOpen}
                  onToggle={(e) => setDd(e.currentTarget.open)}
                  onBlur={(e) => { if (!e.currentTarget.contains(e.relatedTarget as Node | null)) setDd(false); }}
                >
                  <summary aria-expanded={ddOpen} className={industriesActive ? "is-active" : undefined}>
                    {p.industriesLabel}
                    <ChevronDown {...ICON} size={18} className="chev" />
                  </summary>
                  <div className="dd-panel">{industryLinks("dd-list")}</div>
                </details>
              </li>
            ) : (
              <li key={it.key}>
                <Link href={it.href} aria-current={p.path === it.key ? "page" : undefined}>{it.label}</Link>
              </li>
            ),
          )}
        </ul>
      </nav>

      <LangSwitch lang={p.lang} className="bar-lang" />
      <Link className="btn cta-desktop" href={p.cta.href}>{p.cta.label}</Link>

      <details
        ref={menuRef}
        className="menu"
        open={menuOpen}
        onToggle={(e) => setMenu(e.currentTarget.open)}
      >
        <summary aria-expanded={menuOpen}>
          <Menu {...ICON} className="i-open" />
          <X {...ICON} className="i-close" />
          <span>{menuOpen ? p.labels.closeMenu : p.labels.menu}</span>
        </summary>
        <div className="menu-panel">
          <nav aria-label={p.labels.mobile}>
            <ul className="m-list">
              {p.items.map((it) =>
                it.key === p.industriesKey ? (
                  <li key={it.key}>
                    <p className="m-head">{p.industriesLabel}</p>
                    {industryLinks("m-sub")}
                  </li>
                ) : (
                  <li key={it.key}>
                    <Link href={it.href} aria-current={p.path === it.key ? "page" : undefined} onClick={closeAll}>{it.label}</Link>
                  </li>
                ),
              )}
            </ul>
          </nav>
          <div className="m-foot">
            <Link className="btn" href={p.cta.href} onClick={closeAll}>{p.cta.label}</Link>
            <LangSwitch lang={p.lang} className="panel-lang" />
          </div>
        </div>
      </details>
    </div>
  );
}
