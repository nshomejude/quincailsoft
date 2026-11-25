import 'server-only';
import {cache} from 'react';

export type PublicItem = {id: string; title: string; description: string; unit: string; priceXaf: string};
export type PublicStore = {slug: string; displayName: string; items: PublicItem[]; availability: string; checkoutStatus: string};

export const REVALIDATE_SECONDS = 60;
const SLUG = /^[a-z0-9][a-z0-9-]{2,62}$/;

export const isValidSlug = (slug: string) => SLUG.test(slug);

/**
 * Fetches a public store. Cached by the Next data cache for REVALIDATE_SECONDS
 * (tagged `store:<slug>` for on-demand revalidation) and de-duplicated within a
 * single render pass with React cache(), so page + generateMetadata share one request.
 */
export const store = cache(async (slug: string): Promise<PublicStore | null> => {
  if (!isValidSlug(slug)) return null;
  const base = process.env.PUBLIC_CATALOG_API_URL;
  if (!base) throw new Error('Public catalogue API is not configured.');
  const response = await fetch(new URL('/api/public/stores/' + encodeURIComponent(slug), base), {
    next: {revalidate: REVALIDATE_SECONDS, tags: ['store:' + slug]},
  });
  if (response.status === 404) return null;
  if (!response.ok) throw new Error('Store catalogue is temporarily unavailable.');
  return (await response.json()) as PublicStore;
});

export const findItem = cache(async (slug: string, id: string) => {
  const data = await store(slug);
  const item = data?.items.find((p) => p.id === id) ?? null;
  return {data, item};
});

export function money(value: string) {
  if (!/^\d+$/.test(value)) throw new Error('Invalid price');
  return new Intl.NumberFormat('fr-CM').format(BigInt(value)) + ' FCFA';
}
