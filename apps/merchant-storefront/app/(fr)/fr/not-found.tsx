import type {Metadata} from 'next';
import {NotFoundView} from '@/components/NotFoundView';
import {Shell} from '@/components/Shell';
import {noindex} from '@/lib/seo';

export const metadata: Metadata = {...noindex, title: 'Page introuvable'};
export default function NotFound() {
  return (
    <Shell lang="fr" altPath="/">
      <NotFoundView lang="fr" />
    </Shell>
  );
}
