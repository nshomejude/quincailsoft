'use client';
import {t, type Lang} from '@/lib/i18n';

export function ErrorView({lang, reset}: {lang: Lang; reset: () => void}) {
  const L = t(lang);
  return (
    <main id="main" role="alert">
      <h1>{L.errorTitle}</h1>
      <p>{L.errorText}</p>
      <button onClick={reset}>{L.retry}</button>
    </main>
  );
}
