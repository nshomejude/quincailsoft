import { NextResponse } from "next/server";
import { mailConfig, sendMail } from "@/lib/mailer";
import { SITE_URL } from "@/lib/site";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

// Contract used by the contact and demo forms:
// POST application/json
// { form: "contact"|"demo", lang: "en"|"fr", name, email, business?, role?, country?, branches?, topic?, phone?, message?,
//   consent: true, website: "" }   <- "website" is a honeypot and must be empty.
// 200 {ok:true} | 400 {ok:false,error:"invalid",fields:[...]} | 403 | 429 | 503 {ok:false,error:"unavailable"} | 502

const FIELDS = ["name", "email", "business", "role", "country", "branches", "topic", "phone", "message"] as const;
const MAX = { name: 120, email: 200, business: 160, role: 120, country: 80, branches: 20, topic: 80, phone: 40, message: 4000 };
const hits = new Map<string, number[]>();
const WINDOW_MS = 10 * 60_000;
const LIMIT = 5;

const clean = (v: unknown, max: number) =>
  typeof v === "string" ? v.replace(/[\u0000-\u0008\u000b\u000c\u000e-\u001f]/g, "").trim().slice(0, max) : "";
const oneLine = (v: string) => v.replace(/[\r\n]+/g, " ");
const esc = (v: string) =>
  v.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");

function limited(ip: string) {
  const now = Date.now();
  const recent = (hits.get(ip) || []).filter((t) => now - t < WINDOW_MS);
  recent.push(now);
  hits.set(ip, recent);
  if (hits.size > 5000) for (const [k, v] of hits) if (v.every((t) => now - t >= WINDOW_MS)) hits.delete(k);
  return recent.length > LIMIT;
}

export async function POST(request: Request) {
  const origin = request.headers.get("origin");
  const allowed = new Set([SITE_URL, SITE_URL.replace("://", "://www.")]);
  if (!origin || !allowed.has(origin)) return NextResponse.json({ ok: false, error: "origin" }, { status: 403 });

  const ip = (request.headers.get("x-forwarded-for") || "unknown").split(",")[0].trim();
  if (limited(ip)) return NextResponse.json({ ok: false, error: "rate" }, { status: 429 });

  let body: Record<string, unknown>;
  try {
    const text = await request.text();
    if (text.length > 20_000) return NextResponse.json({ ok: false, error: "invalid" }, { status: 400 });
    body = JSON.parse(text);
  } catch {
    return NextResponse.json({ ok: false, error: "invalid" }, { status: 400 });
  }

  // Honeypot: pretend success so bots learn nothing.
  if (clean(body.website, 50)) return NextResponse.json({ ok: true });

  const form = body.form === "demo" ? "demo" : "contact";
  const lang = body.lang === "fr" ? "fr" : "en";
  const v = Object.fromEntries(FIELDS.map((f) => [f, clean(body[f], MAX[f])])) as Record<(typeof FIELDS)[number], string>;
  const bad: string[] = [];
  if (!v.name) bad.push("name");
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(v.email)) bad.push("email");
  if (form === "contact" && !v.message) bad.push("message");
  if (body.consent !== true) bad.push("consent");
  if (bad.length) return NextResponse.json({ ok: false, error: "invalid", fields: bad }, { status: 400 });

  const cfg = mailConfig();
  if (!cfg) {
    console.error("contact: SMTP_HOST / CONTACT_FROM / CONTACT_TO not configured; message not sent");
    return NextResponse.json({ ok: false, error: "unavailable" }, { status: 503 });
  }

  const label = form === "demo" ? "Demo request" : "Contact message";
  const rows = [
    ["Form", label],
    ["Language", lang],
    ["Name", v.name],
    ["Email", v.email],
    ["Phone", v.phone],
    ["Business", v.business],
    ["Role", v.role],
    ["Country", v.country],
    ["Branches", v.branches],
    ["Topic", v.topic],
  ].filter(([, val]) => val);
  const text = rows.map(([k, val]) => `${k}: ${val}`).join("\n") + `\n\n${v.message || "(no message)"}\n\n-- Sent from ${SITE_URL}`;
  const html =
    `<table cellpadding="6" style="border-collapse:collapse;font:14px system-ui,sans-serif">` +
    rows.map(([k, val]) => `<tr><td><strong>${esc(k)}</strong></td><td>${esc(val)}</td></tr>`).join("") +
    `</table><p style="font:14px system-ui,sans-serif;white-space:pre-wrap">${esc(v.message || "(no message)")}</p>` +
    `<p style="color:#667;font:12px system-ui,sans-serif">Sent from ${esc(SITE_URL)}</p>`;

  try {
    await sendMail(cfg, {
      subject: oneLine(`[QuincaiSoft ${label}] ${v.name}${v.business ? " - " + v.business : ""}`).slice(0, 200),
      text,
      html,
      replyTo: v.email,
    });
    return NextResponse.json({ ok: true });
  } catch (error) {
    console.error("contact: send failed:", error instanceof Error ? error.message : "unknown");
    return NextResponse.json({ ok: false, error: "send" }, { status: 502 });
  }
}
