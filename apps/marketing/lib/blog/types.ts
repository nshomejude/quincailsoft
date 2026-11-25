import type { Lang } from "@/lib/site";

/** A rich-text run: plain string, or an object for emphasis/links. Keep inline markup minimal. */
export type Inline = string | { text: string; bold?: boolean; italic?: boolean; href?: string };

export type Block =
  | { type: "p"; text: Inline[] | string }
  | { type: "ul"; items: (Inline[] | string)[] }
  | { type: "ol"; items: (Inline[] | string)[] }
  | { type: "callout"; tone: "tip" | "warning" | "note"; title: string; text: Inline[] | string }
  | { type: "table"; caption: string; head: string[]; rows: string[][] }
  | { type: "quote"; text: string; cite?: string } // only for quotes that are genuinely real; never invent
  | { type: "h3"; text: string };

export type Section = { id: string; heading: string; blocks: Block[] };

export type PostLang = {
  title: string; // <= 60 chars ideal, includes primary keyword
  description: string; // meta description, 140-155 chars
  excerpt: string; // 1-2 sentences for cards
  primaryKeyword: string;
  keywords: string[]; // 6-10 secondary / long-tail keywords for this language
  /** Answer-first summary, 40-70 words: the direct answer to the title's question (for featured snippets / AI answers). */
  tldr: string;
  /** 3-6 key takeaways. */
  takeaways: string[];
  /** 5-9 H2 sections. */
  sections: Section[];
  /** 4-6 FAQ pairs, answer-first, 40-80 words each (rendered with FAQPage JSON-LD). */
  faq: { q: string; a: string }[];
  /** Optional reading list / authoritative references by NAME and publisher only (no fabricated URLs). */
  references?: { label: string; note?: string; href?: string }[];
};

export type Post = {
  slug: string; // same slug in both languages, kebab-case, keyword-led
  category: "buying-guide" | "operations" | "finance" | "payments" | "growth" | "compliance";
  publishedAt: string; // ISO date, use 2026-11-25
  updatedAt: string;
  readingMinutes: number; // honest estimate from the EN word count (~220 wpm)
  /** slugs of 2-3 related posts */
  related: string[];
  /** Internal product pages worth linking from this post, e.g. "features", "industries/hardware-shops". */
  linksTo: string[];
  en: PostLang;
  fr: PostLang;
};

export const CATEGORY_LABEL: Record<Post["category"], Record<Lang, string>> = {
  "buying-guide": { en: "Buying guides", fr: "Guides d'achat" },
  operations: { en: "Operations", fr: "Opérations" },
  finance: { en: "Finance & credit", fr: "Finance et crédit" },
  payments: { en: "Payments", fr: "Paiements" },
  growth: { en: "Growth", fr: "Croissance" },
  compliance: { en: "Tax & compliance", fr: "Fiscalité et conformité" },
};
