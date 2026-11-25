import Link from 'next/link';
import {notFound} from 'next/navigation';
import {findItem, money} from '@/lib/api';
import {path, t, type Lang} from '@/lib/i18n';
import {productJsonLd} from '@/lib/seo';
import {JsonLd} from './JsonLd';
import {Shell} from './Shell';

export async function ProductView({lang, slug, id}: {lang: Lang; slug: string; id: string}) {
  const L = t(lang);
  const {data, item: p} = await findItem(slug, id);
  if (!data || !p) notFound();
  const other = lang === 'en' ? 'fr' : 'en';

  return (
    <Shell lang={lang} altPath={path(other, `/${slug}/${encodeURIComponent(id)}`)}>
      <main id="main">
        <JsonLd data={productJsonLd(lang, slug, data, p)} />
        <nav aria-label={L.breadcrumb} className="crumbs">
          <ol>
            <li><Link href={path(lang, '')}>{L.home}</Link></li>
            <li><Link href={path(lang, '/' + slug)}>{data.displayName}</Link></li>
            <li aria-current="page">{p.title}</li>
          </ol>
        </nav>
        <article className="card">
          <h1>{p.title}</h1>
          <p>{p.description}</p>
          <p className="price">{money(p.priceXaf)} / {p.unit}</p>
          <p className="notice">{L.productNotice}</p>
        </article>
      </main>
    </Shell>
  );
}
