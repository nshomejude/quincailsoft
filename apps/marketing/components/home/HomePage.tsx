import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { t } from "@/lib/content";
import { homeContent } from "@/lib/home-content";
import { INDUSTRY_SLUGS, abs, pagePath, type Lang } from "@/lib/site";
import { HomeIcon } from "./HomeIcon";
import { ProductScreen } from "./ProductScreen";
import "@/app/home.css";

export function homeSchemas(lang: Lang) {
  const c = homeContent(lang);
  return [
    {
      "@context": "https://schema.org",
      "@type": "FAQPage",
      "@id": `${abs(pagePath(lang, ""))}#home-faq`,
      mainEntity: c.faq.items.map((it) => ({
        "@type": "Question",
        name: it.q,
        acceptedAnswer: { "@type": "Answer", text: it.a },
      })),
    },
    {
      "@context": "https://schema.org",
      "@type": "ItemList",
      "@id": `${abs(pagePath(lang, ""))}#features`,
      name: c.itemListName,
      inLanguage: lang,
      itemListElement: c.features.items.map((f, i) => ({
        "@type": "ListItem",
        position: i + 1,
        name: f.title,
        description: f.body,
        url: abs(pagePath(lang, "features")),
      })),
    },
  ];
}

function Head({ id, title, intro }: { id: string; title: string; intro?: string }) {
  return (
    <header className="home-head">
      <h2 id={id}>{title}</h2>
      {intro && <p className="home-intro">{intro}</p>}
    </header>
  );
}

function More({ href, children }: { href: string; children: string }) {
  return (
    <p className="home-more">
      <Link href={href}>{children} <ArrowRight size={18} strokeWidth={1.75} aria-hidden="true" /></Link>
    </p>
  );
}

export function HomePage({ lang }: { lang: Lang }) {
  const c = homeContent(lang);
  const d = t(lang);
  const p = (path: string) => pagePath(lang, path);

  return (
    <div className="home">
      <section className="home-hero" aria-labelledby="home-h1">
        <div className="home-wrap home-hero-grid">
          <div className="home-hero-copy">
            <p className="home-eyebrow">{c.hero.eyebrow}</p>
            <h1 id="home-h1">{c.hero.h1}</h1>
            <p className="home-definition">{c.hero.definition}</p>
            <p className="home-lead">{c.hero.lead}</p>
            <div className="home-actions">
              <Link className="home-btn" href={p("contact")}>{c.hero.ctaDemo}</Link>
              <Link className="home-btn is-outline" href={p("features")}>{c.hero.ctaFeatures}</Link>
            </div>
            <p className="home-status">{c.hero.status}</p>
          </div>
          <ProductScreen s={c.hero.screen} />
        </div>
      </section>

      <section className="home-section is-tight" aria-labelledby="home-facts">
        <div className="home-wrap">
          <h2 id="home-facts" className="home-kicker">{c.facts.title}</h2>
          <ul className="home-facts">
            {c.facts.items.map((f) => (
              <li key={f.title}>
                <HomeIcon name={f.icon} />
                <div>
                  <h3>{f.title}</h3>
                  <p>{f.body}</p>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section className="home-section is-alt" aria-labelledby="home-problems">
        <div className="home-wrap">
          <Head id="home-problems" title={c.problems.title} intro={c.problems.intro} />
          <ul className="home-grid home-problems">
            {c.problems.items.map((x) => (
              <li key={x.pain} className="home-card">
                <HomeIcon name={x.icon} />
                <h3>{x.pain}</h3>
                <p>{x.fix}</p>
                <p className="home-tag"><span>{c.problems.fixLabel}:</span> {x.feature}</p>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section className="home-section" aria-labelledby="home-features">
        <div className="home-wrap">
          <Head id="home-features" title={c.features.title} intro={c.features.intro} />
          <ul className="home-grid home-features">
            {c.features.items.map((x) => (
              <li key={x.title} className="home-card">
                <HomeIcon name={x.icon} />
                <h3>{x.title}</h3>
                <p>{x.body}</p>
              </li>
            ))}
          </ul>
          <More href={p("features")}>{c.features.more}</More>
        </div>
      </section>

      <section className="home-section is-alt" aria-labelledby="home-how">
        <div className="home-wrap">
          <Head id="home-how" title={c.how.title} intro={c.how.intro} />
          <ol className="home-steps">
            {c.how.steps.map((s, i) => (
              <li key={s.title}>
                <span className="home-step-n" aria-hidden="true">{i + 1}</span>
                <h3>{s.title}</h3>
                <p>{s.body}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      <section className="home-section" aria-labelledby="home-industries">
        <div className="home-wrap">
          <Head id="home-industries" title={c.industries.title} intro={c.industries.intro} />
          <ul className="home-grid home-industries">
            {INDUSTRY_SLUGS.map((s) => (
              <li key={s} className="home-card">
                <HomeIcon name={c.industries.icons[s]} />
                <h3><Link href={p(`industries/${s}`)}>{d.industries.items[s].name}</Link></h3>
                <p>{d.industries.items[s].summary}</p>
                <More href={p(`industries/${s}`)}>{`${c.industries.link}: ${d.industries.items[s].name}`}</More>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section className="home-section is-alt" aria-labelledby="home-mobile">
        <div className="home-wrap home-split">
          <div>
            <Head id="home-mobile" title={c.mobile.title} intro={c.mobile.intro} />
            <h3 className="home-sub">{c.mobile.worksTitle}</h3>
            <ul className="home-list">{c.mobile.works.map((x) => <li key={x}>{x}</li>)}</ul>
            <h3 className="home-sub">{c.mobile.stagesTitle}</h3>
            <ul className="home-list">{c.mobile.stages.map((x) => <li key={x}>{x}</li>)}</ul>
          </div>
          <div className="home-side">
            <div className="home-notice" role="note">
              <h3>{c.mobile.limitsTitle}</h3>
              <ul className="home-list">{c.mobile.limits.map((x) => <li key={x}>{x}</li>)}</ul>
            </div>
            <figure className="home-phone">
              <div className="home-phone-frame" aria-hidden="true">
                <div className="home-screen-bar">
                  <span className="home-screen-title">{c.mobile.phone.title}</span>
                  <span className="home-screen-tag">{c.hero.screen.label}</span>
                </div>
                <dl>
                  {c.mobile.phone.rows.map(([k, v]) => (
                    <div key={k}><dt>{k}</dt><dd>{v}</dd></div>
                  ))}
                </dl>
              </div>
              <figcaption className="home-caption">{c.mobile.phone.note}</figcaption>
            </figure>
          </div>
        </div>
      </section>

      <section className="home-section" aria-labelledby="home-payments">
        <div className="home-wrap">
          <Head id="home-payments" title={c.payments.title} intro={c.payments.intro} />
          <ul className="home-grid home-payments">
            {c.payments.items.map((x) => (
              <li key={x.name} className="home-card">
                <h3>{x.name}</h3>
                <p className="home-badge">{x.status}</p>
                <p>{x.body}</p>
              </li>
            ))}
          </ul>
          <More href={p("features")}>{c.payments.more}</More>
        </div>
      </section>

      <section className="home-section is-alt" aria-labelledby="home-security">
        <div className="home-wrap">
          <Head id="home-security" title={c.security.title} intro={c.security.intro} />
          <ul className="home-grid home-security">
            {c.security.items.map((x) => (
              <li key={x.title} className="home-card">
                <HomeIcon name={x.icon} />
                <h3>{x.title}</h3>
                <p>{x.body}</p>
              </li>
            ))}
          </ul>
          <p className="home-fine">{c.security.disclaimer}</p>
        </div>
      </section>

      <section className="home-section" aria-labelledby="home-faq">
        <div className="home-wrap home-faq-wrap">
          <Head id="home-faq" title={c.faq.title} intro={c.faq.intro} />
          <div className="home-faq">
            {c.faq.items.map((f) => (
              <details key={f.q}>
                <summary>{f.q}</summary>
                <p>{f.a}</p>
              </details>
            ))}
          </div>
          <More href={p("faq")}>{c.faq.all}</More>
        </div>
      </section>

      <section className="home-cta" aria-labelledby="home-cta">
        <div className="home-wrap home-cta-inner">
          <div>
            <h2 id="home-cta">{c.cta.title}</h2>
            <p>{c.cta.body}</p>
          </div>
          <div className="home-actions">
            <Link className="home-btn is-invert" href={p("contact")}>{c.cta.primary}</Link>
            <Link className="home-btn is-ghost" href={p("pricing")}>{c.cta.secondary}</Link>
          </div>
        </div>
      </section>
    </div>
  );
}
