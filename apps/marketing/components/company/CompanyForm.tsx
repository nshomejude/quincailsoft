"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import Link from "next/link";
import { CONTACT_EMAIL, type Lang } from "@/lib/site";
import { FORM, TOPICS, type TopicKey } from "@/lib/company-form-content";

type Payload = {
  name: string; email: string; business: string; role: string; country: string;
  branches: string; topic: TopicKey; message: string; consent: boolean; lang: Lang; source: string;
};
type Errors = Partial<Record<"name" | "email" | "business" | "message" | "consent", string>>;

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

type Result = { ok: true } | { ok: false; kind: "invalid" | "rate" | "origin" | "unavailable"; fields?: string[] };

/** Posts to /api/contact (see app/api/contact/route.ts for the contract). */
async function deliver(p: Payload): Promise<Result> {
  try {
    const res = await fetch("/api/contact", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        form: p.source === "demo" ? "demo" : "contact", lang: p.lang, name: p.name, email: p.email, business: p.business, role: p.role,
        country: p.country, branches: p.branches, topic: TOPICS[p.lang][p.topic], message: p.message, consent: p.consent, website: "",
      }),
    });
    if (res.ok) return { ok: true };
    const data = (await res.json().catch(() => ({}))) as { fields?: string[] };
    if (res.status === 400) return { ok: false, kind: "invalid", fields: data.fields };
    if (res.status === 429) return { ok: false, kind: "rate" };
    if (res.status === 403) return { ok: false, kind: "origin" };
    return { ok: false, kind: "unavailable" };
  } catch {
    return { ok: false, kind: "unavailable" };
  }
}

function mailtoFor(p: Payload, subject: string): string {
  const f = FORM[p.lang];
  const t = TOPICS[p.lang];
  const lines = [
    `${f.name}: ${p.name}`, `${f.email}: ${p.email}`, `${f.business}: ${p.business}`, `${f.role}: ${p.role}`,
    `${f.country}: ${p.country}`, `${f.branches}: ${p.branches}`, `${f.topic}: ${t[p.topic]}`, "", p.message,
  ];
  return `mailto:${CONTACT_EMAIL}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(lines.join("\n"))}`;
}

export function CompanyForm({ lang, variant }: { lang: Lang; variant: "contact" | "demo" }) {
  const f = FORM[lang];
  const topics = TOPICS[lang];
  const [js, setJs] = useState(false);
  const [errors, setErrors] = useState<Errors>({});
  const [sent, setSent] = useState<string | null>(null);
  const [fail, setFail] = useState<{ text: string; mailto: string } | null>(null);
  const [busy, setBusy] = useState(false);
  const formRef = useRef<HTMLFormElement>(null);
  useEffect(() => setJs(true), []);
  const needsMessage = variant === "contact";

  const defaultTopic: TopicKey = variant === "demo" ? "demo" : "sales";
  const topicKeys = (Object.keys(topics) as TopicKey[]).filter((k) => (variant === "demo" ? k === "demo" : true));
  const subjectFor = (topic: TopicKey) => `QuincaiSoft - ${topics[topic]}`;

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    const get = (k: string) => String(fd.get(k) ?? "").trim();
    if (get("website")) return; // honeypot: silently drop
    const p: Payload = {
      name: get("name"), email: get("email"), business: get("business"), role: get("role"), country: get("country"),
      branches: get("branches"), topic: (get("topic") as TopicKey) || defaultTopic, message: get("message"),
      consent: fd.get("consent") === "on", lang, source: variant,
    };
    const next: Errors = {};
    if (!p.name) next.name = f.errRequired;
    if (!p.email) next.email = f.errRequired;
    else if (!EMAIL_RE.test(p.email)) next.email = f.errEmail;
    if (!p.business) next.business = f.errRequired;
    if (needsMessage && !p.message) next.message = f.errRequired;
    if (!p.consent) next.consent = f.errConsent;
    setErrors(next);
    setSent(null);
    const keys = Object.keys(next);
    if (keys.length) {
      requestAnimationFrame(() => formRef.current?.querySelector<HTMLElement>(`[name="${keys[0]}"]`)?.focus());
      return;
    }
    setFail(null);
    setBusy(true);
    const form = e.currentTarget;
    const r = await deliver(p);
    setBusy(false);
    if (r.ok) {
      setSent(p.email);
      form.reset();
      return;
    }
    if (r.kind === "invalid") {
      const n: Errors = {};
      for (const k of r.fields ?? []) if (k === "name" || k === "email" || k === "message" || k === "consent") n[k] = k === "email" ? f.errEmail : k === "consent" ? f.errConsent : f.errRequired;
      setErrors(n);
      return;
    }
    const text = r.kind === "rate" ? f.rate : r.kind === "origin" ? f.origin : f.failed;
    setFail({ text, mailto: mailtoFor(p, subjectFor(p.topic)) });
  }

  const err = (k: keyof Errors) => errors[k];
  const labels: Record<keyof Errors, string> = { name: f.name, email: f.email, business: f.business, message: f.message, consent: f.consentShort };
  const errKeys = Object.keys(errors) as (keyof Errors)[];
  const field = (k: keyof Errors) => ({ "aria-invalid": !!err(k), "aria-describedby": err(k) ? `e-${k}` : undefined });

  return (
    <form
      ref={formRef}
      className="co-form"
      onSubmit={onSubmit}
      noValidate={js}
    >
      <div className="co-status" aria-live="polite">
        {errKeys.length > 0 && (
          <div className="co-summary">
            <p><strong>{f.errSummary}</strong></p>
            <ul>{errKeys.map((k) => <li key={k}><a href={`#f-${k}`}>{labels[k]}: {errors[k]}</a></li>)}</ul>
          </div>
        )}
        {busy && <p>{f.sending}</p>}
        {sent && (
          <div className="co-sent">
            <p>{f.sent} <strong className="co-mail">{sent}</strong>.</p>
          </div>
        )}
        {fail && (
          <div className="co-summary">
            <p>{fail.text} <a className="co-mail" href={fail.mailto}>{CONTACT_EMAIL}</a></p>
            <p><a href={fail.mailto}>{f.fallbackLead}</a></p>
          </div>
        )}
      </div>

      <div className="co-row">
        <div>
          <label htmlFor="f-name">{f.name}</label>
          <input id="f-name" name="name" autoComplete="name" required aria-required="true" {...field("name")} />
          {err("name") && <p className="co-err" id="e-name">{err("name")}</p>}
        </div>
        <div>
          <label htmlFor="f-email">{f.email}</label>
          <input id="f-email" name="email" type="email" autoComplete="email" required aria-required="true" {...field("email")} />
          {err("email") && <p className="co-err" id="e-email">{err("email")}</p>}
        </div>
      </div>
      <div className="co-row">
        <div>
          <label htmlFor="f-business">{f.business}</label>
          <input id="f-business" name="business" autoComplete="organization" required aria-required="true" {...field("business")} />
          {err("business") && <p className="co-err" id="e-business">{err("business")}</p>}
        </div>
        <div>
          <label htmlFor="f-role">{f.role} <span className="co-opt">({f.optional})</span></label>
          <input id="f-role" name="role" autoComplete="organization-title" />
        </div>
      </div>
      <div className="co-row">
        <div>
          <label htmlFor="f-country">{f.country} <span className="co-opt">({f.optional})</span></label>
          <input id="f-country" name="country" autoComplete="country-name" />
        </div>
        <div>
          <label htmlFor="f-branches">{f.branches} <span className="co-opt">({f.optional})</span></label>
          <select id="f-branches" name="branches" defaultValue="">
            <option value="">{f.choose}</option>
            {f.branchOptions.map((o) => <option key={o} value={o}>{o}</option>)}
          </select>
        </div>
      </div>
      <div>
        <label htmlFor="f-topic">{f.topic}</label>
        <select id="f-topic" name="topic" defaultValue={defaultTopic}>
          {topicKeys.map((k) => <option key={k} value={k}>{topics[k]}</option>)}
        </select>
      </div>
      <div>
        <label htmlFor="f-message">{f.message}</label>
        <textarea
          id="f-message" name="message" rows={6} required={needsMessage} aria-required={needsMessage}
          aria-invalid={!!err("message")} aria-describedby={err("message") ? "e-message h-message" : "h-message"}
        />
        <p className="co-hint" id="h-message">{variant === "demo" ? f.hintDemo : f.hintContact}</p>
        {err("message") && <p className="co-err" id="e-message">{err("message")}</p>}
      </div>
      <div className="co-hp" aria-hidden="true">
        <label htmlFor="f-website">{f.honeypot}</label>
        <input id="f-website" name="website" tabIndex={-1} autoComplete="off" />
      </div>
      <div>
        <div className="co-consent">
          <input id="f-consent" name="consent" type="checkbox" required aria-required="true" {...field("consent")} />
          <label htmlFor="f-consent">{f.consent} <Link href={`/${lang}/privacy`}>{f.privacy}</Link>.</label>
        </div>
        {err("consent") && <p className="co-err" id="e-consent">{err("consent")}</p>}
      </div>
      <div>
        <button type="submit" className="btn" disabled={busy}>{variant === "demo" ? f.submitDemo : f.submit}</button>
      </div>
      <p className="small">{f.note}</p>
      <noscript><p className="small">{f.noscript} <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>.</p></noscript>
    </form>
  );
}
