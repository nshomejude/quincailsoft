import Link from 'next/link';
import {notFound} from 'next/navigation';
import {money, store} from '@/lib/api';
import {path, t, type Lang} from '@/lib/i18n';
import {storeJsonLd} from '@/lib/seo';
import {JsonLd} from './JsonLd';
import {Shell} from './Shell';

type Sp = {q?: string | string[]; unit?: string | string[]};
const first = (v?: string | string[]) => (Array.isArray(v) ? v[0] : v) ?? '';

export async function StoreView({lang, slug, searchParams}: {lang: Lang; slug: string; searchParams: Sp}) {
  const L = t(lang);
  const data = await store(slug);
  if (!data) notFound();
  const q = first(searchParams.q).trim().slice(0, 100);
  const unit = first(searchParams.unit).slice(0, 40);
  const needle = q.toLowerCase();
  const units = [...new Set(data.items.map((p) => p.unit))].sort();
  const items = data.items.filter(
    (p) => (!unit || p.unit === unit) && (!needle || p.title.toLowerCase().includes(needle) || p.description.toLowerCase().includes(needle)),
  );
  const base = path(lang, '/' + slug);
  const filtered = Boolean(q || unit);

  return (
    <Shell lang={lang} altPath={path(lang === 'en' ? 'fr' : 'en', '/' + slug)}>
      <main id="main">
        <JsonLd data={storeJsonLd(lang, slug, data)} />
        <p className="muted">{L.catalogue}</p>
        <h1>{data.displayName}</h1>
        <p className="notice">{data.availability}. {L.checkoutNote}</p>

        {data.items.length > 0 && (
          <form method="get" action={base} role="search" aria-label={L.search} className="filters">
            <label>
              {L.search}
              <input type="search" name="q" defaultValue={q} maxLength={100} />
            </label>
            {units.length > 1 && (
              <label>
                {L.unit}
                <select name="unit" defaultValue={unit}>
                  <option value="">{L.allUnits}</option>
                  {units.map((u) => <option key={u} value={u}>{u}</option>)}
                </select>
              </label>
            )}
            <div className="actions">
              <button type="submit">{L.searchBtn}</button>
              {filtered && <Link href={base}>{L.clear}</Link>}
            </div>
          </form>
        )}

        {data.items.length > 0 && <p className="muted" role="status">{L.results(items.length, data.items.length)}</p>}

        <section className="grid" aria-label={L.catalogue}>
          {items.map((p) => (
            <article className="card" key={p.id}>
              <h2><Link href={`${base}/${encodeURIComponent(p.id)}`}>{p.title}</Link></h2>
              <p>{p.description}</p>
              <p className="price">{money(p.priceXaf)}</p>
              <span className="muted">{L.per} {p.unit}</span>
            </article>
          ))}
        </section>

        {!data.items.length && <div className="card empty"><p>{L.noProducts}</p></div>}
        {data.items.length > 0 && !items.length && (
          <div className="card empty"><p>{L.noMatch}</p><Link href={base}>{L.clear}</Link></div>
        )}
      </main>
    </Shell>
  );
}
