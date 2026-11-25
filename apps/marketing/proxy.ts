import { NextResponse, type NextRequest } from "next/server";

/** Redirects only "/" to /en or /fr using Accept-Language. All other pages are static. */
export function proxy(req: NextRequest) {
  const header = req.headers.get("accept-language") || "";
  const prefs = header
    .split(",")
    .map((part) => {
      const [tag, ...params] = part.trim().split(";");
      const q = params.map((p) => p.trim()).find((p) => p.startsWith("q="));
      return { tag: tag.toLowerCase(), q: q ? Number(q.slice(2)) : 1 };
    })
    .filter((p) => p.tag && !Number.isNaN(p.q) && p.q > 0)
    .sort((a, b) => b.q - a.q);
  const first = prefs.find((p) => p.tag === "fr" || p.tag.startsWith("fr-") || p.tag === "en" || p.tag.startsWith("en-"));
  const lang = first && first.tag.startsWith("fr") ? "fr" : "en";
  const url = req.nextUrl.clone();
  url.pathname = `/${lang}`;
  const res = NextResponse.redirect(url, 307);
  res.headers.set("Vary", "Accept-Language");
  return res;
}

export const config = { matcher: ["/"] };
