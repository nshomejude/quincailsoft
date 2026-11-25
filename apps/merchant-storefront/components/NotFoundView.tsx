import Link from 'next/link';
import {path, t, type Lang} from '@/lib/i18n';

export function NotFoundView({lang}: {lang: Lang}) {
  const L = t(lang);
  return (
    <main id="main">
      <h1>{L.notFoundTitle}</h1>
      <p>{L.notFoundText}</p>
      <Link className="button" href={path(lang, '')}>{L.home}</Link>
    </main>
  );
}
