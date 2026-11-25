import nodemailer from "nodemailer";

export type MailConfig = {
  host: string;
  port: number;
  secure: boolean;
  user?: string;
  pass?: string;
  from: string;
  to: string[];
};

// Returns null when mail is not configured, so the API can answer 503 and the page can fall back to mailto.
export function mailConfig(): MailConfig | null {
  const host = process.env.SMTP_HOST;
  const from = process.env.CONTACT_FROM;
  const to = (process.env.CONTACT_TO || "")
    .split(/[,\s]+/)
    .map((s) => s.trim())
    .filter(Boolean);
  if (!host || !from || to.length === 0) return null;
  const port = Number(process.env.SMTP_PORT || 587);
  return {
    host,
    port,
    secure: process.env.SMTP_SECURE ? process.env.SMTP_SECURE === "true" : port === 465,
    user: process.env.SMTP_USER || undefined,
    pass: process.env.SMTP_PASSWORD || undefined,
    from,
    to,
  };
}

export async function sendMail(
  cfg: MailConfig,
  msg: { subject: string; text: string; html: string; replyTo?: string },
) {
  const transport = nodemailer.createTransport({
    host: cfg.host,
    port: cfg.port,
    secure: cfg.secure,
    requireTLS: !cfg.secure && process.env.SMTP_REQUIRE_TLS !== "false",
    auth: cfg.user ? { user: cfg.user, pass: cfg.pass } : undefined,
    connectionTimeout: 10_000,
    socketTimeout: 15_000,
  });
  await transport.sendMail({
    from: cfg.from,
    to: cfg.to,
    replyTo: msg.replyTo,
    subject: msg.subject,
    text: msg.text,
    html: msg.html,
  });
}
