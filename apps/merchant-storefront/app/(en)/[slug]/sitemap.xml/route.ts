import {store} from '@/lib/api';
import {path} from '@/lib/i18n';
import {abs} from '@/lib/seo';

export const revalidate = 60;

const esc = (s: string) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&apos;');

function entry(rel: string) {
  const en = abs(path('en', rel));
  const fr = abs(path('fr', rel));
  return (
    `<url><loc>${esc(en)}</loc>` +
    `<xhtml:link rel="alternate" hreflang="en" href="${esc(en)}"/>` +
    `<xhtml:link rel="alternate" hreflang="fr" href="${esc(fr)}"/>` +
    `<xhtml:link rel="alternate" hreflang="x-default" href="${esc(en)}"/></url>` +
    `<url><loc>${esc(fr)}</loc>` +
    `<xhtml:link rel="alternate" hreflang="en" href="${esc(en)}"/>` +
    `<xhtml:link rel="alternate" hreflang="fr" href="${esc(fr)}"/>` +
    `<xhtml:link rel="alternate" hreflang="x-default" href="${esc(en)}"/></url>`
  );
}

export async function GET(_req: Request, {params}: {params: Promise<{slug: string}>}) {
  const {slug} = await params;
  const data = await store(slug);
  if (!data) return new Response('Not found', {status: 404, headers: {'X-Robots-Tag': 'noindex'}});
  const urls = [entry('/' + slug), ...data.items.map((p) => entry(`/${slug}/${encodeURIComponent(p.id)}`))].join('');
  const xml = `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">${urls}</urlset>`;
  return new Response(xml, {headers: {'Content-Type': 'application/xml; charset=utf-8', 'Cache-Control': 'public, s-maxage=60, stale-while-revalidate=300'}});
}
