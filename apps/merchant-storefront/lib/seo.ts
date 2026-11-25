import type {Metadata} from 'next';
import {money, type PublicItem, type PublicStore} from './api';
import {path, t, type Lang} from './i18n';

export const siteUrl = (process.env.NEXT_PUBLIC_SITE_URL || process.env.SITE_URL || 'http://localhost:3300').replace(/\/+$/, '');
export const abs = (p: string) => siteUrl + p;
const ogLocale = (l: Lang) => (l === 'fr' ? 'fr_CM' : 'en_CM');

export const noindex: Metadata = {robots: {index: false, follow: false}};

function alternates(rel: string, lang: Lang): Metadata['alternates'] {
  return {
    canonical: abs(path(lang, rel)),
    languages: {en: abs(path('en', rel)), fr: abs(path('fr', rel)), 'x-default': abs(path('en', rel))},
  };
}

function build(lang: Lang, rel: string, title: string, description: string): Metadata {
  const url = abs(path(lang, rel));
  return {
    title: {absolute: title},
    description,
    alternates: alternates(rel, lang),
    openGraph: {title, description, url, siteName: t(lang).brand, locale: ogLocale(lang), type: 'website', images: [{url: abs('/logo.png'), alt: 'QuincaiSoft'}]},
    twitter: {card: 'summary', title, description, images: [abs('/logo.png')]},
  };
}

export const storeMetadata = (lang: Lang, slug: string, d: PublicStore) =>
  build(lang, '/' + slug, t(lang).storeTitle(d.displayName), t(lang).storeDesc(d.displayName, d.items.length));

export const productMetadata = (lang: Lang, slug: string, d: PublicStore, p: PublicItem) =>
  build(lang, `/${slug}/${p.id}`, `${p.title} – ${d.displayName}`, t(lang).productDesc(p.title, d.displayName, money(p.priceXaf), p.unit));

export const homeMetadata = (lang: Lang) => build(lang, '', t(lang).homeTitle + ' | QuincaiSoft', t(lang).homeText);

function availabilityUrl(text: string) {
  if (/out of stock|unavailable|closed|rupture|indisponible|ferm/i.test(text)) return 'https://schema.org/OutOfStock';
  return 'https://schema.org/InStock';
}

const storeId = (lang: Lang, slug: string) => abs(path(lang, '/' + slug)) + '#store';

const crumbs = (lang: Lang, d: PublicStore, slug: string, p?: PublicItem) => ({
  '@context': 'https://schema.org',
  '@type': 'BreadcrumbList',
  itemListElement: [
    {'@type': 'ListItem', position: 1, name: t(lang).home, item: abs(path(lang, ''))},
    {'@type': 'ListItem', position: 2, name: d.displayName, item: abs(path(lang, '/' + slug))},
    ...(p ? [{'@type': 'ListItem', position: 3, name: p.title, item: abs(path(lang, `/${slug}/${p.id}`))}] : []),
  ],
});

const seller = (lang: Lang, d: PublicStore, slug: string) => ({
  '@context': 'https://schema.org',
  '@type': 'Store',
  '@id': storeId(lang, slug),
  name: d.displayName,
  url: abs(path(lang, '/' + slug)),
  currenciesAccepted: 'XAF',
  areaServed: 'CM',
});

export const storeJsonLd = (lang: Lang, slug: string, d: PublicStore) => [seller(lang, d, slug), crumbs(lang, d, slug)];

export const productJsonLd = (lang: Lang, slug: string, d: PublicStore, p: PublicItem) => [
  seller(lang, d, slug),
  {
    '@context': 'https://schema.org',
    '@type': 'Product',
    name: p.title,
    description: p.description || undefined,
    sku: p.id,
    url: abs(path(lang, `/${slug}/${p.id}`)),
    brand: {'@type': 'Organization', name: d.displayName},
    offers: {
      '@type': 'Offer',
      url: abs(path(lang, `/${slug}/${p.id}`)),
      priceCurrency: 'XAF',
      price: p.priceXaf,
      availability: availabilityUrl(d.availability),
      seller: {'@id': storeId(lang, slug)},
      priceSpecification: {'@type': 'UnitPriceSpecification', price: p.priceXaf, priceCurrency: 'XAF', unitText: p.unit},
    },
  },
  crumbs(lang, d, slug, p),
];
