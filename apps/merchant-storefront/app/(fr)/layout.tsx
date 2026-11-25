import '@/app/globals.css';
import type {Metadata} from 'next';
import {siteUrl} from '@/lib/seo';

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title: {default: 'QuincaiSoft Boutique marchande', template: '%s | QuincaiSoft'},
  description: 'QuincaiSoft retail and trade',
  icons: {icon: '/logo.png'},
};

export default function Layout({children}: {children: React.ReactNode}) {
  return (
    <html lang="fr">
      <body>{children}</body>
    </html>
  );
}
