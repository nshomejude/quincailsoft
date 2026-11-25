# QuincaiSoft marketing site

Public, bilingual (EN/FR) marketing site for QuincaiSoft by Opesware Technologies. Next.js 16, React 19, TypeScript, fully statically generated, `output: "standalone"`.

## Run

```
pnpm install
pnpm --filter @quincaisoft/marketing dev        # http://localhost:3300
pnpm --filter @quincaisoft/marketing build
pnpm --filter @quincaisoft/marketing start      # next start --port 3300
pnpm --filter @quincaisoft/marketing typecheck
```

## Configuration

- `NEXT_PUBLIC_SITE_URL` - public origin for canonical URLs, hreflang, sitemap, robots and JSON-LD (default `https://quincaisoft.com`). Set it at build time.
- `NEXT_PUBLIC_CONTACT_EMAIL` - receiving inbox (default `contact@quincaisoft.com`, unconfirmed).

## Structure

- `app/[lang]/...` - pages for `en` and `fr` (home, features, industries, pricing, about, contact, faq, privacy, terms). `proxy.ts` redirects `/` by `Accept-Language`.
- `lib/content.ts` - all copy in both languages. `lib/site.ts` - URLs, metadata and JSON-LD builders.
- `app/robots.ts`, `app/sitemap.ts` (with hreflang alternates), `app/manifest.ts`, `app/icon.svg`.
- `public/llms.txt`, `public/llms-full.txt` - factual summaries for AI crawlers. Update them with the content. They contain `quincaisoft.com` URLs; adjust if the domain differs.

## Content rules

No invented prices, customers, testimonials, statistics or certifications. Do not claim DGI fiscal approval; MTN Mobile Money is not live-verified; Orange Money is not integrated; offline posting is disabled (see `ARCHITECTURE.md` and the root `README.md`).

## Open TODOs

- Contact and pricing forms use a `mailto:` placeholder. Replace with a server-side handler and confirm the inbox.
- Privacy and terms pages are drafts needing legal review; they are `noindex` and excluded from the sitemap until approved.
- `public/logo.png` is a copy of `assets/logo.png` (a JPEG with a `.png` extension); supply a proper transparent PNG/SVG and OG image when available.
- Content-Security-Policy and a hosting/deployment pipeline are not set up.
