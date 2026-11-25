import type {Metadata} from 'next';
import Link from 'next/link';
import './globals.css';
import {Providers} from '@/components/Providers';
import {Navigation} from '@/components/Navigation';
import {api, Me} from '@/lib/api';
import {locale, messages} from '@/lib/locale';
import {session} from '@/lib/auth';

export const metadata: Metadata = {
  title: 'QuincaiSoft · Retail operations',
  description: 'Retail, trade and distribution workspace',
  // The admin workspace is login-gated and must never be indexed.
  robots: {index: false, follow: false, nocache: true},
};

export default async function Layout({children}: {children: React.ReactNode}) {
  const lang = await locale();
  const t = messages[lang];
  let auth = null;
  let authorities: string[] = [];

  try {
    auth = await session();
    if (auth) {
      authorities = (await api<Me>('me')).authorities;
    }
  } catch {
    // Render the shell without navigation entries when the profile is unavailable.
  }

  return (
    <html lang={lang}>
      <body>
        <Providers>
          <a className="skip" href="#main">{t.skip}</a>
          <aside>
            <Link className="brand" href="/">
              Q
              <span>
                QuincaiSoft
                <small>Retail · Trade · Distribution</small>
              </span>
            </Link>
            <Navigation authorities={authorities} labels={t} />
            <div className="aside-foot">
              Opesware Technologies
              <br />
              <small>Retail operations</small>
            </div>
          </aside>
          <div className="workspace">
            <header>
              <span>{t.workspace}</span>
              <div>
                <form action="/locale" method="post">
                  <label className="sr-only" htmlFor="locale">Language / Langue</label>
                  <select id="locale" name="locale" defaultValue={lang}>
                    <option value="en">English</option>
                    <option value="fr">Français</option>
                  </select>
                  <button>OK</button>
                </form>
                {auth ? (
                  <>
                    <span>{auth.name}</span>
                    <form action="/auth/logout" method="post">
                      <button>{t.signout}</button>
                    </form>
                  </>
                ) : (
                  <a href="/auth/login">{t.signin}</a>
                )}
              </div>
            </header>
            <main id="main">{children}</main>
            <footer>QuincaiSoft · Opesware Technologies</footer>
          </div>
        </Providers>
      </body>
    </html>
  );
}
