import Link from 'next/link';
import {locale, messages} from '@/lib/locale';

export default async function NotFound() {
  const t = messages[await locale()];

  return (
    <section className="panel">
      <p className="eyebrow">404</p>
      <h1>{t.notFoundTitle}</h1>
      <p>{t.notFoundBody}</p>
      <Link className="button" href="/">{t.backHome}</Link>
    </section>
  );
}
