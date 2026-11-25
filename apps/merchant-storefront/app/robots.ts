import type {MetadataRoute} from 'next';
import {siteUrl} from '@/lib/seo';

// Per-store sitemaps live at /{slug}/sitemap.xml (store slugs cannot be enumerated here).
// Point crawlers at them via Search Console or a link from the merchant's own site.
export default function robots(): MetadataRoute.Robots {
  return {rules: [{userAgent: '*', allow: '/'}], host: siteUrl};
}
