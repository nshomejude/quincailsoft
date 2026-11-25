import type {Metadata} from 'next';
import {ProductView} from '@/components/ProductView';
import {findItem} from '@/lib/api';
import {noindex, productMetadata} from '@/lib/seo';

export const revalidate = 60;
// No pages are prebuilt (slugs are unknown at build); ISR caches each product page on first request.
export const generateStaticParams = async () => [];

type P = {params: Promise<{slug: string; id: string}>};

export async function generateMetadata({params}: P): Promise<Metadata> {
  const {slug, id} = await params;
  const {data, item} = await findItem(slug, id);
  return data && item ? productMetadata('en', slug, data, item) : {...noindex, title: 'Product not found'};
}

export default async function Product({params}: P) {
  const {slug, id} = await params;
  return <ProductView lang="en" slug={slug} id={id} />;
}
