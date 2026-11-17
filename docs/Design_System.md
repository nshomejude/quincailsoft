# QuincaiSoft — Sky Clarity design system
Version 1.0 · 17 November 2026

## Colour and branding
Preserve the supplied logo without stretching, cropping, redrawing or effects. Use object-fit: contain and clear space equal to at least one quarter of its height. Colours below are working selections from the logo, not verified vector values.

| Token | Hex | Purpose |
|---|---|---|
| Sky blue | #1290E5 | Brand accents and decorative strokes |
| Navy | #051A53 | Headings and brand anchors |
| Action blue | #086CB4 | Primary buttons with white labels |
| Canvas | #F3F8FC | Page background |
| White | #FFFFFF | Opaque working surfaces |
| Text | #14283F | Body |
| Muted | #52657A | Secondary text |
| Border | #D6E2ED | Decorative dividers |
| Selection | #E6F4FF | Active navigation background |
| Success | #166534 | Labelled success states |
| Warning | #854D0E | Labelled warnings |
| Error | #B42318 | Labelled errors |

No glow, blur, translucent panels behind text, rainbow charts, colour-only status, or competing gradients. Sky blue is not suitable for small white button labels: use action blue. Charts use navy/blue sequential colours plus patterns or labels; semantic red/green remains reserved for meaning.

## Typography and icons
Inter if locally packaged, otherwise Segoe UI/system sans-serif. Body 15–16px, labels 13–14px, headings 28–32px; line height 1.5. Use tabular numerals and right alignment for quantities and money. Production icon family: Lucide, 20px, 1.75 stroke, 24px view box. Icons accompany text; icon-only controls require accessible names. Complete Lucide assets are not packaged in this dependency-free preview.

## Layout and spacing
8px rhythm with 4px fine adjustments; cards use 12px radius and controls 8px. Desktop: 264px sidebar, 76px context bar, 32px content gutters, maximum content width 1600px. Tablet: 224px sidebar, 24px gutters. Below 720px: collapsible navigation, single-column cards and 16px gutters. Minimum touch target 44px. Quick actions precede metrics. Horizontal overflow stays inside tables; no compressed labels or overlapping panels.

## Layout families
Records: toolbar, filters, table, detail drawer.
POS: product search/grid and sale basket, with tender dialog and receipt outcome.
Approval: request, evidence, history and distinct decision panel.
Reconciliation: expected versus observed amounts, discrepancy explanation and resolution.
Fulfillment: allocation, picking, partial delivery and custody transitions.
Mobile: task-first list and single-column form with visible offline state.
Commerce: catalogue, cart, checkout and order tracking.
Reports: scope, period, freshness, table/chart and export state.
Documents: clean paper surface with no application navigation in print.

The current generic renderer preserves each canonical screen contract. Specialized production layouts and state machines must still be implemented individually.

## Accessibility and interaction
Visible keyboard focus, semantic headings, explicit labels and native dialogs. Status combines colour with text. Maintain contrast for normal text at 4.5:1 and controls at 3:1 where applicable; validate all final states. Loading, empty, error, denied, stale/offline and success states require distinct messages and recovery actions. Preserve input on errors. Never display a queued transaction as server-confirmed.

## Bilingual rules
Production strings must use en/fr translation keys. Use en-CM/fr-CM formatting, FCFA, Africa/Douala business dates, expandable labels and explicit locale-aware parsing. Do not concatenate translated sentences. Current preview is English; French parity is pending.

## Printing
A4 for commercial documents, landscape for wide reports, dedicated 58mm/80mm thermal layouts. Repeat table headers, avoid split rows, keep totals/signatures together. Each issued output requires legal merchant identity, currency, number, date, source snapshot, template version and original/duplicate marker. This preview uses synthetic data and three representative document structures; it is not certified fiscal output.

## Handoff
Reuse contracts/design-tokens.json in web, Expo and WPF resources. Backend permissions, financial posting, numbering, idempotency and synchronization remain mandatory. Every screen action must be mapped to its actual command and passing evidence before being marked implemented.
