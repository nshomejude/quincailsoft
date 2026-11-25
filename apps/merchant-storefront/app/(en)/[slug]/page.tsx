import type {Metadata} from 'next';
import {StoreView} from '@/components/StoreView';
import {store} from '@/lib/api';
import {noindex, storeMetadata} from '@/lib/seo';

// The catalogue fetch is cached for 60s (data cache). Search params make this route render per request.
export const revalidate = 60;

export async function generateMetadata({params}: {params: Promise<{slug: string}>}): Promise<Metadata> {
  const {slug} = await params;
  const data = await store(slug);
  return data ? storeMetadata('en', slug, data) : {...noindex, title: 'Store not found'};
}

export default async function Store({params, searchParams}: {params: Promise<{slug: string}>; searchParams: Promise<{q?: string | string[]; unit?: string | string[]}>}) {
  const {slug} = await params;
  return <StoreView lang="en" slug={slug} searchParams={await searchParams} />;
}
