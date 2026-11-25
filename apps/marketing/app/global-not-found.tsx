import "./globals.css";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "404 | QuincaiSoft",
  robots: { index: false, follow: false },
};

export default function GlobalNotFound() {
  return (
    <html lang="en">
      <body>
        <main className="wrap section" id="main">
          <h1>404 - Page not found / Page introuvable</h1>
          <p>The page you are looking for does not exist or has moved.</p>
          <p>La page que vous cherchez n&apos;existe pas ou a été déplacée.</p>
          <p>
            <a href="/en/contact">Contact</a> · <a href="/en/faq">FAQ</a> · <a href="/en/support">Support</a> ·{" "}
            <a href="/fr/contact">Contact (FR)</a> · <a href="/fr/faq">FAQ (FR)</a> · <a href="/fr/support">Assistance</a>
          </p>
          <p className="actions">
            <a className="btn" href="/en">English</a>
            <a className="btn secondary" href="/fr">Français</a>
          </p>
        </main>
      </body>
    </html>
  );
}
