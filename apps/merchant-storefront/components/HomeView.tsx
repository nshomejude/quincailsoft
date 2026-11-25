import {path, t, type Lang} from '@/lib/i18n';
import {Shell} from './Shell';

export function HomeView({lang}: {lang: Lang}) {
  const L = t(lang);
  return (
    <Shell lang={lang} altPath={path(lang === 'en' ? 'fr' : 'en', '')}>
      <main id="main">
        <h1>{L.homeTitle}</h1>
        <p>{L.homeText}</p>
        <div className="card"><p>{L.homeNote}</p></div>
      </main>
    </Shell>
  );
}
