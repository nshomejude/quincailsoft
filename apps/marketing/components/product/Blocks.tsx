import Link from "next/link";
import { ArrowRight, CircleAlert, CircleCheck, CircleDashed, CircleDot } from "lucide-react";
import type { ReactNode } from "react";
import { pagePath, type Lang } from "@/lib/site";
import type { Block, Item, ModuleData, ProductUi, Status } from "@/lib/product-types";
import { Icon } from "./Icon";

const SI = { available: CircleCheck, partial: CircleDot, pending: CircleAlert, planned: CircleDashed } as const;

export function StatusTag({ status, ui }: { status: Status; ui: ProductUi }) {
  const S = SI[status];
  return (
    <span className="pr-status" data-s={status}>
      <S size={14} strokeWidth={1.75} color="currentColor" fill="none" aria-hidden="true" focusable="false" />
      {ui.status[status]}
    </span>
  );
}

export function SectionHead({ id, title, intro }: { id?: string; title: string; intro?: string }) {
  return (
    <div className="pr-sec-head">
      <h2 id={id}>{title}</h2>
      {intro && <p>{intro}</p>}
    </div>
  );
}

function CardList({ items, ui }: { items: Item[]; ui: ProductUi }) {
  return (
    <ul className="pr-grid">
      {items.map((it) => (
        <li key={it.title} className="pr-card">
          <h3>
            {it.icon && <Icon name={it.icon} />}
            <span>{it.title}</span>
          </h3>
          {it.status && <StatusTag status={it.status} ui={ui} />}
          <p>{it.body}</p>
          {it.points && (
            <ul>
              {it.points.map((p) => (
                <li key={p}>{p}</li>
              ))}
            </ul>
          )}
        </li>
      ))}
    </ul>
  );
}

function Section({ id, alt, children }: { id?: string; alt?: boolean; children: ReactNode }) {
  return (
    <section className={`section pr-sec${alt ? " alt" : ""}`} id={id ? `${id}-section` : undefined}>
      <div className="wrap">{children}</div>
    </section>
  );
}

export function Blocks({ blocks, ui }: { blocks: Block[]; ui: ProductUi }) {
  return (
    <>
      {blocks.map((b, i) => {
        switch (b.kind) {
          case "cards":
            return (
              <Section key={i} id={b.id} alt={b.alt}>
                <SectionHead id={b.id} title={b.title} intro={b.intro} />
                <CardList items={b.items} ui={ui} />
              </Section>
            );
          case "steps":
            return (
              <Section key={i} id={b.id} alt={b.alt}>
                <SectionHead id={b.id} title={b.title} intro={b.intro} />
                <ol className="pr-steps">
                  {b.items.map((s) => (
                    <li key={s.title}>
                      <div>
                        <h3>{s.title}</h3>
                        <p>{s.body}</p>
                      </div>
                    </li>
                  ))}
                </ol>
              </Section>
            );
          case "table":
            return (
              <Section key={i} id={b.id} alt={b.alt}>
                <SectionHead id={b.id} title={b.title} intro={b.intro} />
                <div className="pr-tablewrap" role="region" aria-label={b.caption} tabIndex={0}>
                  <table className="pr-table">
                    <caption>{b.caption}</caption>
                    <thead>
                      <tr>
                        {b.head.map((h) => (
                          <th key={h} scope="col">{h}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {b.rows.map((r) => (
                        <tr key={r[0]}>
                          {r.map((c, j) => (j === 0 ? <th key={j} scope="row">{c}</th> : <td key={j}>{c}</td>))}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </Section>
            );
          case "limits":
            return (
              <Section key={i} id={b.id} alt={b.alt}>
                <div className="notice pr-notice" style={{ marginTop: 0 }}>
                  <h2 id={b.id}>
                    <Icon name="alert" />
                    {b.title}
                  </h2>
                  <ul>
                    {b.items.map((x) => (
                      <li key={x}>{x}</li>
                    ))}
                  </ul>
                </div>
              </Section>
            );
          case "text":
            return (
              <Section key={i} id={b.id} alt={b.alt}>
                <div className="pr-text">
                  <h2 id={b.id}>{b.title}</h2>
                  {b.paras.map((p) => (
                    <p key={p}>{p}</p>
                  ))}
                </div>
              </Section>
            );
          case "faq":
            return (
              <Section key={i} id={b.id} alt={b.alt}>
                <SectionHead id={b.id} title={b.title} />
                <div className="pr-faq" style={{ maxWidth: "52rem" }}>
                  {b.items.map((f) => (
                    <details key={f.q}>
                      <summary>{f.q}</summary>
                      <p>{f.a}</p>
                    </details>
                  ))}
                </div>
              </Section>
            );
        }
      })}
    </>
  );
}

export function ModuleCard({ m, ui }: { m: ModuleData; ui: ProductUi }) {
  return (
    <article className="pr-module" id={m.id} aria-labelledby={`${m.id}-h`}>
      <div className="pr-module-head">
        <h2 id={`${m.id}-h`}>
          <Icon name={m.icon} />
          <span>{m.title}</span>
        </h2>
        <StatusTag status={m.status} ui={ui} />
        <p>{m.what}</p>
      </div>
      <div className="pr-module-body">
        <div>
          <h3>{ui.moduleHow}</h3>
          <ul>
            {m.how.map((h) => (
              <li key={h}>{h}</li>
            ))}
          </ul>
        </div>
        <p className="pr-statusnote">{m.statusNote}</p>
      </div>
    </article>
  );
}

export function RelatedLinks({ lang, paths, titles }: { lang: Lang; paths: string[]; titles: Record<string, string> }) {
  return (
    <ul className="pr-related">
      {paths.map((p) => (
        <li key={p}>
          <Link href={pagePath(lang, p)}>
            <span>{titles[p] ?? p}</span>
            <ArrowRight size={18} strokeWidth={1.75} color="currentColor" fill="none" aria-hidden="true" focusable="false" />
          </Link>
        </li>
      ))}
    </ul>
  );
}
