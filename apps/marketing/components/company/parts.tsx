import Link from "next/link";
import type { ReactNode } from "react";
import {
  Boxes, BookOpen, CalendarCheck, CircleHelp, ClipboardList, Clock, Code, Cookie, Eye, Handshake, Headset, Keyboard,
  Languages, LifeBuoy, Mail, Newspaper, Plug, Rocket, Scale, Search, ShieldCheck, Store, Target, TriangleAlert, Users,
  Wrench, Building2, FileText, Layers, Globe, type LucideIcon,
} from "lucide-react";
import "@/app/company.css";

const ICONS: Record<string, LucideIcon> = {
  boxes: Boxes, book: BookOpen, calendar: CalendarCheck, help: CircleHelp, list: ClipboardList, clock: Clock, code: Code,
  cookie: Cookie, eye: Eye, handshake: Handshake, headset: Headset, keyboard: Keyboard, languages: Languages, lifebuoy: LifeBuoy,
  mail: Mail, news: Newspaper, plug: Plug, rocket: Rocket, scale: Scale, search: Search, shield: ShieldCheck, store: Store,
  target: Target, alert: TriangleAlert, users: Users, wrench: Wrench, building: Building2, file: FileText, layers: Layers, globe: Globe,
};

export function Icon({ name }: { name: string }) {
  const I = ICONS[name] ?? FileText;
  return <I className="co-icon" size={24} strokeWidth={1.75} aria-hidden="true" focusable="false" />;
}

export type CardItem = { icon: string; title: string; body: string; href?: string; cta?: string };

export function Hero({ h1, lead, children }: { h1: string; lead: string; children?: ReactNode }) {
  return (
    <section className="hero co-hero">
      <div className="wrap">
        <h1>{h1}</h1>
        <p className="lead">{lead}</p>
        {children}
      </div>
    </section>
  );
}

export function Section({ title, intro, alt, id, children }: { title?: string; intro?: string; alt?: boolean; id?: string; children: ReactNode }) {
  return (
    <section className={`section co-section${alt ? " alt" : ""}`} id={id} aria-labelledby={id && title ? `${id}-h` : undefined}>
      <div className="wrap">
        {title && <h2 id={id ? `${id}-h` : undefined}>{title}</h2>}
        {intro && <p className="co-intro">{intro}</p>}
        {children}
      </div>
    </section>
  );
}

export function CardGrid({ items, lang }: { items: CardItem[]; lang: string }) {
  return (
    <ul className="co-grid">
      {items.map((c) => (
        <li className="co-card" key={c.title}>
          <Icon name={c.icon} />
          <h3>{c.title}</h3>
          <p>{c.body}</p>
          {c.href &&
            (c.href.startsWith("http") ? (
              <a className="co-link" href={c.href} rel="noopener">{c.cta}</a>
            ) : c.href.startsWith("mailto:") ? (
              <a className="co-link" href={c.href}>{c.cta}</a>
            ) : (
              <Link className="co-link" href={`/${lang}/${c.href}`}>{c.cta}</Link>
            ))}
        </li>
      ))}
    </ul>
  );
}

export function Steps({ items }: { items: { title: string; body: string }[] }) {
  return (
    <ol className="co-steps">
      {items.map((s) => (
        <li key={s.title}>
          <h3>{s.title}</h3>
          <p>{s.body}</p>
        </li>
      ))}
    </ol>
  );
}

export function Checklist({ items }: { items: string[] }) {
  return (
    <ul className="co-list">
      {items.map((i) => <li key={i}>{i}</li>)}
    </ul>
  );
}

export function Faq({ items }: { items: { q: string; a: string }[] }) {
  return (
    <div className="faq co-faq">
      {items.map((i) => (
        <details key={i.q}>
          <summary>{i.q}</summary>
          <p>{i.a}</p>
        </details>
      ))}
    </div>
  );
}

export function DraftNote({ label, text }: { label: string; text: string }) {
  return <p className="notice co-draft" role="note"><strong>{label}.</strong> {text}</p>;
}

export function Prose({ sections }: { sections: { title: string; body: string[] }[] }) {
  return (
    <div className="co-prose">
      {sections.map((s) => (
        <section key={s.title}>
          <h2>{s.title}</h2>
          {s.body.map((p) => <p key={p}>{p}</p>)}
        </section>
      ))}
    </div>
  );
}
