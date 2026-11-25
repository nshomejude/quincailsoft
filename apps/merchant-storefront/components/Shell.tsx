import Link from 'next/link';
import {otherLang, path, t, type Lang} from '@/lib/i18n';

export function Shell({lang, altPath, children}: {lang: Lang; altPath: string; children: React.ReactNode}) {
  const L = t(lang);
  const o = otherLang(lang);
  return (
    <>
      <a className="skip" href="#main">{L.skip}</a>
      <header>
        <Link href={path(lang, '')} className="brand">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src="/logo.png" alt="" width={36} height={36} />
          <strong>{L.brand}</strong>
        </Link>
        <nav aria-label={L.langSwitch}>
          <Link href={path(lang, '')}>{L.home}</Link>
          <Link href={altPath} hrefLang={o} lang={o} aria-label={o === 'fr' ? 'Français' : 'English'}>{o.toUpperCase()}</Link>
        </nav>
      </header>
      {children}
      <footer><p>{L.powered} <strong>QuincaiSoft</strong></p></footer>
    </>
  );
}
