# QuincaiSoft brand requirements — 18 November 2026

User instruction: preserve the original QuincaiSoft logo styling and its glass sky-blue identity. Applies to cloud and local agents. Backend Java Spring Boot migration remains the priority; mobile and native desktop work remain paused.

## Visual authority

Use the existing assets/logo.png wordmark: deep navy Quincai, sky-blue Soft and underline, white background. Preserve its proportions and original artwork. ARCHITECTURE.md brand section proposes navy #0A2354, sky #1595E6, white #FFFFFF, surface #F7FAFD, text #162238, muted #667085, border #E3E8F0. These are documented design tokens, not a claim of exact pixel extraction from the logo. Validate against the original asset before final visual acceptance.

The user's glass sky-blue description must retain the original brand appearance. Do not infer permission for a new frosted-glass dashboard redesign: the architecture requires readable operational surfaces without glow or decorative glassmorphism. Use accessible foreground/background pairings; sky blue alone does not guarantee readable white button text.

## Required integration by Desktop Codex

Read this requirement and incorporate it into root AGENTS.md so future agents check branding before UI changes. Centralize brand tokens and apply them consistently to login, app shell, navigation, actions, forms, detail pages, reports, and printable branding. Preserve semantic warning/error/success colors.

At reviewed commit bc8591d76f0e60e6bd8846258010fed3cc5cde59, apps/web-admin/app/globals.css still uses the older ink #142934, steel #20566e and sand #d0a16a palette. Correct this drift while preserving existing layout and behavior. Do not replace the QuincaiSoft identity with the standalone Windows product palette.

Verify the actual local login, dashboard, one detail page and one report/document against the original logo at desktop and narrow widths. Save genuine screenshots and identify anything not yet running. Do not claim styling fixed from this requirements document alone.

## Coordination review

Desktop pushed f858c9f and bc8591d after handover, covering gate/work allocation updates, inventory/master-data migration selection and catalog forms. Published docs/WORK_ALLOCATION.md reports three gate tests and 19 web Node tests passed, with Next build/authenticated browser and local OIDC/seed startup pending. This is Desktop's published evidence, not an independently rerun verification. The latest diff also appears to corrupt existing French accents in apps/web-admin/lib/i18n.ts (for example Référence becomes RÃ©fÃ©rence); inspect and restore UTF-8 text before visual acceptance.

This branch intentionally changes documentation only to avoid conflicting with Desktop's active Java and web edits. Desktop owns final integration and the live PC preview.
