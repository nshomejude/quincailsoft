import type { ProductPageData, ProductUi } from "../product-types";
import type { ProductSlug } from "../product-routes";

export const ui: ProductUi = {
  status: {
    available: "Available in migration slice",
    partial: "Partly available",
    planned: "Planned",
    pending: "Pending / disabled for now",
  },
  onThisPage: "On this page",
  related: "Related pages",
  contact: "Contact us",
  seeFeatures: "See the features",
  author: "Opesware Technologies",
  published: "Published 25 November 2026",
  readGuide: "Read the guide",
  limitsTitle: "Honest limits",
  moduleWhat: "What it does",
  moduleHow: "How it works",
  guidesTitle: "Practical guides",
  relatedGuides: "Related guides",
  resources: "Resources",
  minutes: "min read",
};

const CTA = {
  ctaTitle: "Want to see how this fits your business?",
  ctaBody: "Tell us about your shop or distribution business. We reply by email in English or French and are open about what is ready and what is still being built.",
};

export const products: Record<ProductSlug, ProductPageData> = {
  "mobile-app": {
    icon: "phone",
    kicker: "Mobile app",
    metaTitle: "Mobile app for owners and sales staff | QuincaiSoft",
    description: "The QuincaiSoft mobile app is included in every plan: secure sign-in, catalogue and sales views, and encrypted offline sale drafts. Offline economic posting is currently disabled.",
    h1: "A mobile app in every plan, designed for weak connectivity",
    answer: "QuincaiSoft includes a native mobile app, built with Expo and React Native, for owners, managers and sales staff in every plan. Today it signs in through your identity provider, searches the catalogue, shows sales, and keeps sale drafts in an encrypted local queue that is sent when the connection returns. Offline posting of completed sales and stock changes is currently disabled until signed stock quotas and native-device evidence exist.",
    glance: {
      title: "At a glance",
      items: [
        { label: "Secure sign-in", status: "available", note: "OpenID Connect with PKCE in the system browser." },
        { label: "Catalogue and sales views", status: "available", note: "Scoped to your company and branch." },
        { label: "Offline sale drafts", status: "available", note: "Drafts only. The app never claims stock or money was posted." },
        { label: "Offline completed sales", status: "pending", note: "Disabled until signed quotas and device evidence are complete." },
      ],
    },
    blocks: [
      {
        kind: "cards", id: "today", title: "What the app does today",
        items: [
          { icon: "key", status: "available", title: "Secure sign-in", body: "Sign-in uses the OpenID Connect Authorization Code flow with PKCE in the system browser. Tokens are kept in the operating system's secure store, never in browser storage, and no client secret is embedded in the app." },
          { icon: "search", status: "available", title: "Catalogue search and details", body: "Search and refresh the catalogue and open a product's details, read from the QuincaiSoft API within your company and branch scope." },
          { icon: "cart", status: "available", title: "Sales list and details", body: "See sales with their lines, customer, warehouse, date and amounts. Quantities are typed as exact values so nothing is rounded silently." },
          { icon: "walloff", status: "available", title: "Offline sale drafts", body: "Capture a sale draft with no connection. It waits in a durable local queue and is transmitted when you choose to send it. Only draft creation is implemented on mobile." },
          { icon: "database", status: "available", title: "Encrypted local database", body: "Each scope has its own SQLCipher-encrypted database with a random 256-bit key held in the secure store. If native encryption is missing the app refuses to store data in plain text." },
          { icon: "route", status: "available", title: "Safe retries", body: "A draft that may not have arrived is retried with its original operation ID, so a repeat cannot create a second sale." },
        ],
      },
      {
        kind: "table", id: "matrix", alt: true, title: "What works online and offline",
        intro: "This is the intended capability matrix and where each function stands today.",
        caption: "Mobile capabilities by connection state",
        head: ["Function", "Online", "Offline", "Status today"],
        rows: [
          ["Catalogue and price lookup", "Yes", "Cached read-only catalogue (stage 1)", "Online available; offline cache planned"],
          ["Sale draft", "Yes", "Yes, queued locally", "Available"],
          ["Stock count capture", "Yes", "Draft counts, approved after sync (stage 2)", "Planned on mobile"],
          ["Cash sale posting", "Yes", "Controlled, within a signed stock quota (stage 3)", "Offline disabled"],
          ["Customer credit sale", "Yes", "Restricted by a cached credit allowance", "Planned"],
          ["Mobile money verification", "Yes", "Never. It stays pending until the provider confirms online.", "By design"],
          ["Refunds and sensitive cash changes", "Yes, with step-up authentication", "Restricted", "Planned"],
          ["User roles and billing", "Yes, with MFA", "No", "By design"],
        ],
      },
      {
        kind: "steps", id: "stages", title: "How offline selling will be released",
        intro: "We release offline capability in stages, and only when each stage has evidence behind it.",
        items: [
          { title: "Stage 1: online, with a cached read-only catalogue", body: "Look up products and prices when the connection is weak." },
          { title: "Stage 2: offline stock counts", body: "Capture counts on the shop floor. A manager approves and posts them after synchronisation." },
          { title: "Stage 3: controlled offline cash sales", body: "Sell within a signed stock quota, an offline policy and limits set for the device. This stage stays disabled until the quota and native-device evidence are complete." },
        ],
      },
      {
        kind: "cards", id: "safety", alt: true, title: "How offline data is protected",
        items: [
          { icon: "fingerprint", status: "available", title: "Device enrolment", body: "A device proves possession of an RSA key of at least 2,048 bits and is approved by an owner. This does not establish hardware attestation, and approval does not grant offline selling." },
          { icon: "layers", status: "available", title: "Drafts and completed facts are kept apart", body: "Local records are classified as draft, completed-authorised or completed-exception. Completed evidence is retained for review and is never sent as an ordinary draft." },
          { icon: "shield", status: "available", title: "Recovery quarantine", body: "Owner-signed recovery keeps conflicting histories side by side for review instead of overwriting either one." },
        ],
      },
      {
        kind: "limits", id: "limits", title: "Honest limits",
        items: [
          "Offline posting of completed sales is disabled. Drafts are the only offline record the app creates.",
          "Mobile money payments cannot be verified offline. They stay pending until the provider confirms online.",
          "A passing build is not native-device certification. Native-device evidence is still to be produced.",
          "Camera barcode scanning is in the architecture but not in the delivered mobile slice.",
          "When disconnected, the app cannot guarantee live stock or balances.",
        ],
      },
      {
        kind: "faq", id: "faq", title: "Mobile app questions",
        items: [
          { q: "Is the mobile app included in the plan?", a: "Yes. Every package includes mobile access to the functions you subscribe to. Prices depend on scale and entitlements, not on a separate app fee." },
          { q: "Can I sell without internet today?", a: "You can record a sale draft offline, but it is not a completed sale. Completed offline sales are disabled until signed quotas and device evidence are in place." },
          { q: "Which phones does it run on?", a: "It is built with Expo and React Native for phones. Native-device testing and certification are not complete, so ask us about pilot devices before you plan a rollout." },
          { q: "What happens if a phone is lost?", a: "Local data is encrypted. An owner can change or deactivate a member's access, which raises an authorisation epoch so older tokens are rejected by the API." },
        ],
      },
    ],
    related: ["features", "payments", "security", "desktop", "resources/run-multiple-branches"],
    ...CTA,
  },

  payments: {
    icon: "card",
    kicker: "Payments",
    metaTitle: "Payments, credit and MTN Mobile Money | QuincaiSoft",
    description: "How QuincaiSoft handles cash, customer credit, deposits and MTN Mobile Money. Orange Money is not integrated and QuincaiSoft does not hold your customers' money.",
    h1: "Payments: cash, credit, deposits and MTN Mobile Money",
    answer: "QuincaiSoft records cash, customer credit, deposits and staged payments today, and includes an MTN Mobile Money adapter that still needs live credentials and MTN acceptance before it handles real money. Orange Money is not integrated. Neither QuincaiSoft nor Opesware Technologies holds your customers' money: payments belong to the merchant and its licensed payment provider.",
    glance: {
      title: "At a glance",
      items: [
        { label: "Cash and till sessions", status: "available", note: "Opening float, counted close and variance review." },
        { label: "Credit, deposits, receipts", status: "available", note: "Receivables, deposit balances and allocations in FCFA." },
        { label: "MTN Mobile Money", status: "partial", note: "Transport and reconciliation built. Live credentials and acceptance pending." },
        { label: "Orange Money", status: "planned", note: "Not integrated. Pending a contracted integration." },
      ],
    },
    blocks: [
      {
        kind: "cards", id: "tenders", title: "How each way of paying is recorded",
        items: [
          { icon: "banknote", status: "available", title: "Cash", body: "A cash session opens with a declared float. Expected cash is derived from the immutable cash records of the session, and the close compares it with the counted amount. A variance is reviewed, never edited away." },
          { icon: "wallet", status: "available", title: "Customer credit", body: "A credit sale creates a receivable. Cash receipts are applied against recognised customer debt, and a receipt larger than the debt is rejected rather than hidden." },
          { icon: "landmark", status: "available", title: "Deposits and staged payments", body: "A deposit is held as a customer liability, then allocated to a sale without a second cash receipt. A cash refund of an unused deposit needs its own permission and cannot exceed the remaining balance." },
          { icon: "receipt", status: "partial", title: "Bank transfer and manual references", body: "Manual references for a transfer or a mobile money payment are recorded as unverified. They are never labelled provider-confirmed because a number looks valid." },
        ],
      },
      {
        kind: "steps", id: "mtn", alt: true, title: "How an MTN Mobile Money collection works",
        intro: "The adapter is tested against simulated provider responses. Sandbox verification never posts FCFA to the books. Live use needs credentials and MTN acceptance.",
        items: [
          { title: "Prepare the intent", body: "The sale and exact amount are fixed. The intent is saved as queued before anything is sent to the provider." },
          { title: "Send the request to pay", body: "The state is saved as unknown before dispatch. Only approved HTTPS addresses are used and redirects are disabled." },
          { title: "Query the status", body: "If the answer is unclear, the same reference is queried. A new charge is never submitted to resolve doubt." },
          { title: "Verify", body: "A payment counts as verified only when the provider returns a financial transaction reference that matches the amount, currency and payer." },
          { title: "Reconcile", body: "Verified provider truth is saved first, then allocated to the receivable. If the period is closed or the allocation changed, the payment goes to review and the customer is not charged again." },
        ],
      },
      {
        kind: "cards", id: "providers", title: "Provider status",
        items: [
          { icon: "phone", status: "partial", title: "MTN Mobile Money", body: "Credential-token, request-to-pay and status transport exist, with durable intents and reconciliation. Live credentials and MTN acceptance are still required." },
          { icon: "phone", status: "planned", title: "Orange Money", body: "Not integrated. It is pending a contracted integration and is not marked as supported anywhere in the product." },
          { icon: "card", status: "planned", title: "Cards and other wallets", body: "Would need a provider contract, a security review and settlement reconciliation. Card data would not be stored by QuincaiSoft unless an approved PCI scope exists." },
        ],
      },
      {
        kind: "text", id: "custody", alt: true, title: "No custody of funds",
        paras: [
          "Your customers' payments belong to you and your licensed processor. Opesware Technologies, which develops QuincaiSoft, is not a custodian of merchant cash or settlements, and the software subscription is separate from your sales.",
          "The provider-intents report lists requests and their status. It does not add amounts together, because a request is not a collection and environments and currencies differ.",
        ],
      },
      {
        kind: "table", id: "reconcile", title: "Reconciliation: three views",
        caption: "Reconciliation views and their status",
        head: ["View", "What it holds", "Status"],
        rows: [
          ["Internal tender ledger", "Tenders confirmed inside QuincaiSoft: cash, receipts, deposits, verified provider payments.", "Available"],
          ["Provider records", "Provider transactions and settlement batches.", "Planned (settlement import)"],
          ["Bank statement", "The merchant's own bank statement, where available.", "Planned"],
        ],
      },
      {
        kind: "limits", id: "limits", title: "Honest limits",
        items: [
          "No live MTN collection is enabled until credentials and provider acceptance are in place.",
          "Public callback routing is not enabled. A callback hint can only flag a payment for reconciliation. It never settles money.",
          "Provider refunds are planned. A refund will not be marked refunded until the provider confirms it.",
          "No certification, fiscal approval or provider approval is claimed.",
        ],
      },
      {
        kind: "faq", id: "faq", alt: true, title: "Payment questions",
        items: [
          { q: "Can customers pay by MTN Mobile Money?", a: "Not yet in live use. The adapter exists and is tested against simulated responses, but live credentials and MTN acceptance are still required." },
          { q: "Does QuincaiSoft support Orange Money?", a: "No. Orange Money is not integrated and is pending a contracted integration." },
          { q: "Does Opesware hold my sales money?", a: "No. Merchant payments belong to the merchant and its licensed processor. Opesware Technologies is not a custodian of merchant cash or settlements." },
          { q: "Can I sell on credit and take deposits?", a: "Yes. Credit sales, receipts, deposits, allocations and approved deposit refunds are part of the delivered finance slice." },
        ],
      },
    ],
    related: ["features", "security", "resources/mobile-money-reconciliation-basics", "resources/manage-customer-credit-hardware-shop", "pricing"],
    ...CTA,
  },

  "online-store": {
    icon: "store",
    kicker: "Online store",
    metaTitle: "Online store: publish your catalogue | QuincaiSoft",
    description: "The optional QuincaiSoft online store publishes a chosen catalogue snapshot to public pages. Checkout, online payment and customer accounts are not available yet.",
    h1: "Online store: publish your catalogue, with checkout still to come",
    answer: "The QuincaiSoft online store is an optional module. A merchant can publish a chosen snapshot of the catalogue to a public store page and item pages that show only approved public fields. There is no checkout, online payment or customer account yet, and the store shows no live stock count.",
    glance: {
      title: "At a glance",
      items: [
        { label: "Public catalogue pages", status: "available", note: "A store page and item pages with approved public fields only." },
        { label: "Publication control", status: "available", note: "The owner publishes an explicit catalogue snapshot." },
        { label: "Cart, checkout, payment", status: "planned", note: "Specified in the architecture. Not built yet." },
        { label: "Custom domain", status: "planned", note: "Domain verification and certificates are planned." },
      ],
    },
    blocks: [
      {
        kind: "cards", id: "available", title: "What exists today",
        items: [
          { icon: "store", status: "available", title: "Public store and item pages", body: "A store is reachable by its name and lists the published catalogue. Each item has its own page. Missing stores and service failures show clear states." },
          { icon: "list", status: "available", title: "Owner-controlled publication", body: "Store settings are changed by an authorised owner, who publishes an explicit snapshot of the catalogue. Only approved public fields are exposed." },
          { icon: "lock", status: "available", title: "A separate, low-privilege application", body: "The storefront has no merchant sign-in token, no tenant header and no database connection. It reads only the public catalogue API." },
          { icon: "key", status: "available", title: "Optional by entitlement", body: "The store is a separate commerce entitlement provisioned through a privileged deployment process. Runtime requests cannot grant it or charge for it." },
        ],
      },
      {
        kind: "steps", id: "roadmap", alt: true, title: "Planned: from browsing to a fulfilled order",
        intro: "Everything in this list is specified in the architecture and is not built yet.",
        items: [
          { title: "Browse and cart", body: "Anonymous browsing, optional guest checkout according to merchant policy, and server-side totals so a price cannot be changed in the browser." },
          { title: "Stock reservation", body: "A reservation with an expiry holds stock during checkout. Unused reservations are released on schedule." },
          { title: "Payment through a licensed provider", body: "Provider callbacks are verified independently. Card data does not land on QuincaiSoft servers." },
          { title: "Fulfilment", body: "Pickup or delivery, delivery fees and customer notifications, with stock recognised on handover." },
          { title: "Returns and refunds", body: "Handled by the same returns and finance services as in-store sales." },
        ],
      },
      {
        kind: "cards", id: "control", title: "Designed for merchant control",
        items: [
          { icon: "check", status: "planned", title: "Preview and approve", body: "The merchant previews and approves public product visibility before anything is published." },
          { icon: "globe", status: "planned", title: "Search visibility in English and French", body: "Merchant SEO fields, product URL slugs, hreflang alternates, structured data and sitemaps are specified for the public pages." },
          { icon: "file", status: "planned", title: "Enquiry-only catalogue", body: "A merchant can show a catalogue and take enquiries or quote requests without accepting online payment." },
        ],
      },
      {
        kind: "limits", id: "limits", alt: true, title: "Honest limits",
        items: [
          "There is no checkout, online payment, customer account or order management yet.",
          "The store does not show live stock quantities.",
          "Custom domains, certificate automation and delivery regions are planned, not delivered.",
          "No payment is settled through Opesware Technologies. Online payments will use licensed providers.",
        ],
      },
      {
        kind: "faq", id: "faq", title: "Online store questions",
        items: [
          { q: "Can customers pay online today?", a: "No. The store publishes your catalogue. Checkout and online payment are planned and not available." },
          { q: "Does it show my stock levels to the public?", a: "No. The public pages show approved public fields only and no live stock count." },
          { q: "Is the online store included in every plan?", a: "No. It is an optional commerce entitlement. Contact us to discuss whether it fits your business." },
        ],
      },
    ],
    related: ["features", "payments", "security", "pricing"],
    ...CTA,
  },

  security: {
    icon: "shield",
    kicker: "Security",
    metaTitle: "Security, audit trail and data isolation | QuincaiSoft",
    description: "How QuincaiSoft separates tenants with PostgreSQL row-level security, keeps an audit trail, prevents duplicate postings and controls access. No certifications are claimed.",
    h1: "Security: tenant isolation, audit trail and controlled access",
    answer: "QuincaiSoft separates each business's data in PostgreSQL with row-level security, keeps an append-only audit trail, protects every command against duplicate posting with idempotency, enforces roles and permissions on the server, and signs people in through an OpenID Connect identity provider. We do not claim ISO 27001, SOC 2, PCI DSS or any other certification.",
    glance: {
      title: "At a glance",
      items: [
        { label: "Tenant isolation", status: "available", note: "Forced row-level security and a non-owner runtime database role." },
        { label: "Audit trail", status: "partial", note: "Append-only. Not yet wired into every command." },
        { label: "Idempotency", status: "available", note: "A repeated command returns the original result." },
        { label: "Certifications", status: "planned", note: "None claimed today." },
      ],
    },
    blocks: [
      {
        kind: "cards", id: "controls", title: "Controls in the product today",
        items: [
          { icon: "lock", status: "available", title: "Tenant isolation with row-level security", body: "Every tenant-owned table carries a tenant ID and forced row-level security. The runtime database role is not the table owner and cannot bypass the policies, and the migration role is separate. A request without a verified tenant context fails closed." },
          { icon: "scroll", status: "partial", title: "Append-only audit trail", body: "Audit entries cannot be edited or deleted. Posted facts are corrected by reversal linked to the original, never overwritten. Audit currently covers identity changes and projected events, and is not yet instrumented on every command." },
          { icon: "fingerprint", status: "available", title: "Idempotent commands", body: "Each command carries a stable operation ID and a request hash. Repeating it returns the original result instead of posting twice, and reusing an ID with different content is rejected." },
          { icon: "users", status: "available", title: "Roles, permissions and approvals", body: "Permissions are enforced again on the server even if a button is hidden. Sensitive changes need an independent approver. Changing a role raises an authorisation epoch so older tokens stop working." },
          { icon: "key", status: "available", title: "OpenID Connect sign-in", body: "Authorization Code with PKCE. Web tokens stay on the server, and Redis holds temporary sessions, not balances. Password recovery, MFA and global sign-out belong to the identity provider you configure. There is no demo-password bypass in production." },
          { icon: "database", status: "available", title: "Business data stays in PostgreSQL", body: "Ledgers, stock, documents and audit live in PostgreSQL 18. Reports read through security-invoker views, so row-level security still applies to them." },
          { icon: "phone", status: "available", title: "Encrypted storage on the phone", body: "Mobile data is held in an encrypted local database with its key in the operating system's secure store, and it fails closed if encryption is unavailable." },
          { icon: "building", status: "available", title: "Platform operations kept apart", body: "Platform administration is a separate application with its own sign-in, issuer, audience and cookie. Operators are enrolled through a privileged deployment process." },
        ],
      },
      {
        kind: "steps", id: "flow", alt: true, title: "How a request is protected",
        items: [
          { title: "Authenticate", body: "The token is validated for signature, issuer and audience." },
          { title: "Resolve membership", body: "An active database membership decides the tenant, company and branch. A header from the browser is never trusted for this." },
          { title: "Check the permission", body: "The service checks the permission itself, regardless of what the screen showed." },
          { title: "Open a scoped transaction", body: "The verified tenant is set inside the transaction. Row-level security then limits every query to that tenant." },
          { title: "Post once, atomically", body: "The command's stock, money and document effects commit together with its idempotency record and audit or outbox evidence." },
        ],
      },
      {
        kind: "text", id: "data", title: "Where your data lives",
        paras: [
          "Business data is stored in PostgreSQL. Hosting region, backups and recovery commitments depend on the deployment mode and the contract, and are not published here as service levels. In a customer-hosted deployment the data stays on infrastructure the customer controls.",
          "You can export reports page by page as CSV today. A full machine-readable export of your records is planned.",
        ],
      },
      {
        kind: "limits", id: "limits", alt: true, title: "What we do not claim",
        items: [
          "No ISO 27001, SOC 2, PCI DSS, OHADA or DGI approval is claimed.",
          "No independent penetration test report is published.",
          "Passing integration tests against PostgreSQL 18 is evidence of behaviour. It is not a security audit.",
          "Hardware-backed attestation of mobile device keys is not established.",
          "MFA depends on the identity provider you configure.",
        ],
      },
      {
        kind: "faq", id: "faq", title: "Security questions",
        items: [
          { q: "Can one business see another business's data?", a: "It is designed not to. Row-level security is forced on tenant tables, the runtime role cannot bypass it, and a missing tenant context fails closed. Isolation is tested against a real PostgreSQL 18 database." },
          { q: "Can a posted sale be deleted?", a: "No. Posted facts are corrected by reversal or adjustment linked to the original." },
          { q: "Who manages passwords and MFA?", a: "Your identity provider. QuincaiSoft does not store passwords and has no demo-password bypass in production." },
          { q: "Is QuincaiSoft certified?", a: "No certification is claimed. We describe controls and the evidence behind them instead." },
        ],
      },
    ],
    related: ["features", "mobile-app", "payments", "desktop", "faq"],
    ...CTA,
  },

  desktop: {
    icon: "monitor",
    kicker: "Windows desktop",
    metaTitle: "Windows desktop and the optional sync adapter | QuincaiSoft",
    description: "Your existing Windows software keeps working on its own. QuincaiSoft includes an optional sync adapter whose integration into the original Windows application is still pending.",
    h1: "Windows desktop: your existing software stays independent",
    answer: "If you already run the existing Windows application, it keeps working on its own with a local SQLite database and no cloud dependency. QuincaiSoft includes an optional .NET 10 sync adapter that could connect a Windows site to the cloud after explicit consent, but it is not yet integrated into the original Windows application, and no cloud connection is offered today.",
    glance: {
      title: "At a glance",
      items: [
        { label: "Standalone Windows", status: "available", note: "Unchanged. No cloud account and no startup network." },
        { label: "Sync adapter library", status: "partial", note: "Built and tested. Integration into the original host is pending." },
        { label: "Connected mode", status: "planned", note: "Only after explicit onboarding and cutover." },
        { label: "Phone companion for standalone", status: "planned", note: "Encrypted snapshots and drafts, no live posting." },
      ],
    },
    blocks: [
      {
        kind: "cards", id: "modes", title: "Two supported modes",
        items: [
          { icon: "monitor", status: "available", title: "Standalone", body: "The Windows application and its SQLite database remain the sole business authority and work fully offline. No cloud account is needed and nothing syncs automatically. Existing standalone customers can choose never to connect." },
          { icon: "cloud", status: "planned", title: "Connected", body: "After explicit onboarding and cutover, a desktop becomes a registered site with its own offline allocation under a cloud tenant. The cloud receives acknowledged operations and shows consolidated views. The same sale is represented once globally." },
        ],
      },
      {
        kind: "cards", id: "adapter", alt: true, title: "What the adapter does today",
        items: [
          { icon: "database", status: "available", title: "Crash-safe local outbox", body: "The outbox shares the local posting transaction, so a crash between the local commit and the network send loses nothing." },
          { icon: "key", status: "available", title: "Protected device keys", body: "Device keys are protected by Windows DPAPI, and signatures match the cloud's device proof." },
          { icon: "lock", status: "available", title: "Consent gates and no startup network", body: "Connection needs explicit consent. In standalone mode the adapter makes no network call at startup." },
          { icon: "route", status: "available", title: "Recovery with identical IDs", body: "Uncertain outcomes are retried with the same identifiers. Matched review receipts stop automatic resending while the original facts are kept." },
        ],
      },
      {
        kind: "steps", id: "cutover", title: "Planned cutover protocol",
        intro: "A summary of the architecture's protocol for connecting a Windows site. It is planned and not yet offered.",
        items: [
          { title: "Consent and proof of ownership", body: "The owner selects the tenant and branch, proves ownership and accepts the data terms." },
          { title: "Native gates and a verified backup", body: "The desktop passes its own acceptance gates and a restore-tested encrypted backup is made." },
          { title: "Dry-run export", body: "Customers, suppliers, products and units, stock, opening balances and document references are exported and checked for collisions." },
          { title: "ID map and reconciliation", body: "Every local ID is mapped, and opening stock, credit, deposits, payables and cash are reconciled before anything is accepted." },
          { title: "One opening method", body: "Choose historical import or current-balance cutover, never both, so stock and revenue are not counted twice." },
          { title: "Activation with a rollback path", body: "A device quota is issued, a synthetic sale is synchronised and checked, and at least one closing cycle is monitored before unassisted rollout." },
        ],
      },
      {
        kind: "limits", id: "limits", alt: true, title: "Honest limits",
        items: [
          "The adapter has not been injected into the original Windows application's posting path.",
          "The recovered Windows baseline has not been natively compiled and certified here, and it has open dependency-audit findings.",
          "The adapter's automated checks were run on Linux. That is not native Windows certification.",
          "No cloud synchronisation for Windows is offered today.",
          "Mobile money entries in the existing Windows software are manually confirmed references, not provider verification.",
        ],
      },
      {
        kind: "faq", id: "faq", title: "Windows questions",
        items: [
          { q: "Do I have to move my Windows shop to the cloud?", a: "No. Standalone remains fully supported and does not need a cloud account. Connecting is optional and reversible." },
          { q: "Will my Windows software stop if the internet is down?", a: "No. It works on its own with a local database, independent of cloud availability." },
          { q: "Can I try the sync today?", a: "No. The adapter exists as a tested library, but integration into the original Windows application is pending." },
        ],
      },
    ],
    related: ["migration-and-onboarding", "mobile-app", "security", "pricing"],
    ...CTA,
  },

  "migration-and-onboarding": {
    icon: "route",
    kicker: "Migration and onboarding",
    metaTitle: "Migration and onboarding: how a shop moves in | QuincaiSoft",
    description: "The planned path for moving a shop to QuincaiSoft: set-up, opening-data import with a dry run, reconciliation, training, a pilot and go-live.",
    h1: "Migration and onboarding: how a shop moves in",
    answer: "Moving a shop to QuincaiSoft is planned as a supervised, reversible process: set up the business, bring in opening data with a dry run, reconcile stock and balances, train staff, run a pilot and then go live. The steps below follow the architecture. Automated provisioning and import tooling are not complete, so onboarding today is arranged directly with Opesware Technologies.",
    glance: {
      title: "At a glance",
      items: [
        { label: "Guided onboarding", status: "planned", note: "Set-up, opening data and checks, step by step." },
        { label: "Import with a dry run", status: "planned", note: "Totals are shown before anything is posted." },
        { label: "Pilot of at least 10 trading days", status: "planned", note: "Before wider rollout." },
        { label: "Today", status: "partial", note: "Arranged with our team. No automated import yet." },
      ],
    },
    blocks: [
      {
        kind: "steps", id: "path", title: "The onboarding path",
        intro: "All steps are planned. Each ends with a check, and a failed step resumes rather than leaving a half-built account.",
        items: [
          { title: "Verify the owner and company", body: "Confirm the owner's contact and collect the company profile and legal identity." },
          { title: "Create the working set-up", body: "Default branch, stock location, price list, document numbering, roles and plan entitlements. Fiscal settings stay provisional until your accountant confirms them." },
          { title: "Choose how to bring in data", body: "Start with no data, import a CSV, or use a supervised migration." },
          { title: "Dry run", body: "See the totals and the business date before anything is posted." },
          { title: "Post opening balances", body: "Opening stock, receivables and payables are posted with audited opening-balance references. Historical stock never creates duplicate sales." },
          { title: "Configure the counter", body: "FCFA, receipt language, shift policy, and a printer test where available." },
          { title: "Confirm", body: "Onboarding is confirmed only after tenant isolation, permissions, document numbering and data checks pass." },
        ],
      },
      {
        kind: "cards", id: "data", alt: true, title: "What can be brought across",
        items: [
          { icon: "package", status: "planned", title: "Products, units and categories", body: "Your catalogue with its units of measure, so stock keeps the same meaning." },
          { icon: "boxes", status: "planned", title: "Stock snapshot", body: "Opening quantities per warehouse, reconciled against a count." },
          { icon: "users", status: "planned", title: "Customers and suppliers", body: "Master data, with collisions shown before import." },
          { icon: "wallet", status: "planned", title: "Opening credit, deposits and payables", body: "Balances owed to you and by you, reconciled before they are accepted." },
          { icon: "file", status: "planned", title: "Existing document references", body: "Original document numbers are kept as the original number and never rewritten." },
        ],
      },
      {
        kind: "steps", id: "pilot", title: "Pilot and go-live",
        items: [
          { title: "Train the team", body: "Documented user training is a condition for finishing a pilot." },
          { title: "Pilot for at least 10 trading days", body: "Capture baseline and closing stock and balances, daily cashier shifts, sales by payment type, returns, deposits and purchasing." },
          { title: "Exit criteria", body: "No unresolved critical finance or isolation defect, no unexplained reconciliation difference, a successful backup restore and stable support." },
        ],
      },
      {
        kind: "limits", id: "limits", alt: true, title: "Honest limits",
        items: [
          "Operational import and export tooling and the full provisioning lifecycle are not complete.",
          "We do not publish timelines or fees here. Contact us for both.",
          "A machine-readable export of your data when you leave is planned and not yet available.",
          "Connecting an existing Windows site follows its own protocol and is not yet offered. See the Windows desktop page.",
        ],
      },
      {
        kind: "faq", id: "faq", title: "Onboarding questions",
        items: [
          { q: "Can I import my products and customers?", a: "An import with a dry run is planned. Today, onboarding is arranged with our team and automated import is not yet available." },
          { q: "Will my old document numbers be kept?", a: "Yes, that is the design. Original numbers are kept as the original number and are not rewritten." },
          { q: "How long does a move take?", a: "We do not publish a duration. It depends on your data and branches. A pilot lasts at least 10 trading days." },
        ],
      },
    ],
    related: ["desktop", "features", "pricing", "resources/buy-and-receive-stock-from-suppliers"],
    ...CTA,
  },

  compare: {
    icon: "scale",
    kicker: "Buying guide",
    metaTitle: "Choosing retail software in CEMAC: a neutral checklist",
    description: "A vendor-neutral checklist for choosing retail, stock and point-of-sale software in Cameroon and the CEMAC region, with how QuincaiSoft answers each point today.",
    h1: "Choosing retail software in CEMAC: a neutral checklist",
    answer: "Choose retail and stock software by testing it on your own daily work: units and conversions, credit, cash control, honest mobile money handling, behaviour without internet, data ownership and support. Use this checklist with any vendor, including us, and ask for evidence rather than claims. We name no competitors and show where QuincaiSoft stands today, including its gaps.",
    blocks: [
      {
        kind: "cards", id: "checklist", title: "The checklist",
        intro: "Ask each vendor to show these on your own products, not on a demonstration catalogue.",
        items: [
          { icon: "ruler", title: "Units and conversions", body: "Can you sell the same product by bag, piece, metre or tonne with consistent stock?", points: ["Does it reject conversions that do not divide exactly?", "Can you cut or repack stock and keep the balance correct?"] },
          { icon: "wallet", title: "Credit and deposits", body: "Can you see what each customer owes and has paid in advance?", points: ["Are deposits kept separate from sales?", "Can a receipt be applied twice by mistake?"] },
          { icon: "banknote", title: "Cash control", body: "Can you reconcile the till every day without editing history?", points: ["Is expected cash derived, not typed?", "Are corrections made by new entries?"] },
          { icon: "card", title: "Mobile money honesty", body: "Does it say whether a payment is verified or only a typed reference?", points: ["Which providers are truly integrated?", "Who holds the money?"] },
          { icon: "walloff", title: "Weak connectivity", body: "What happens at the counter when the internet drops?", points: ["What can be sold offline, and under what limits?", "How are duplicates prevented on reconnect?"] },
          { icon: "shield", title: "Data and security", body: "Is your data separated from other customers and exportable?", points: ["Who can see which branch?", "Is there an audit trail?"] },
          { icon: "landmark", title: "Fiscal and legal", body: "What does the vendor claim about tax and fiscal approval?", points: ["Is any approval claimed with a reference?", "Who confirms the rules with your accountant?"] },
          { icon: "handshake", title: "Support and total cost", body: "Who answers, in which language, and what is not included?", points: ["Hardware, hosting, training, upgrades?", "What happens to your data if you leave?"] },
        ],
      },
      {
        kind: "table", id: "ours", alt: true, title: "How QuincaiSoft answers today",
        intro: "Status follows the project's own migration records.",
        caption: "QuincaiSoft against the checklist",
        head: ["Checklist point", "QuincaiSoft today", "Status"],
        rows: [
          ["Units and conversions", "One base unit per product, exact quantities, zero-loss cutting and repacking jobs. Alternate pack units with exact conversion are planned.", "Partly available"],
          ["Credit and deposits", "Receivables, receipts, deposits, allocation and approved deposit refunds.", "Available"],
          ["Cash control", "Derived expected cash, counted close, reviewed variances, independent approvals.", "Available"],
          ["Mobile money honesty", "MTN adapter built, live use pending. Orange Money not integrated. Manual references are never called verified.", "Partly available"],
          ["Weak connectivity", "Encrypted offline sale drafts. Offline completed sales are disabled for now.", "Partly available"],
          ["Data and security", "Row-level tenant isolation, append-only audit, idempotency. No certification claimed.", "Partly available"],
          ["Fiscal and legal", "Configurable. No DGI approval or certification is claimed.", "Not claimed"],
          ["Support and cost", "Contact us for pricing. Onboarding is arranged with our team.", "Planned"],
        ],
      },
      {
        kind: "steps", id: "trial", title: "Run a fair trial in one week",
        items: [
          { title: "Pick your hardest products", body: "Choose items sold in several units, cut to length, or repacked." },
          { title: "Replay a real day", body: "Enter a day of sales, credit, a deposit, a return and a supplier delivery." },
          { title: "Close the till", body: "Check that the expected cash and variance are derived, not typed." },
          { title: "Test the bad day", body: "Cut the internet and repeat a submission. Look for duplicates and honest labels." },
          { title: "Ask for the limits in writing", body: "Ask what is not built yet, and what is not certified." },
        ],
      },
      {
        kind: "faq", id: "faq", alt: true, title: "Buying questions",
        items: [
          { q: "Should I choose the cheapest option?", a: "Compare total cost: licences or subscription, hardware, hosting, training, upgrades, support and the cost of leaving. A low price can hide those items." },
          { q: "How do I check a vendor's claims?", a: "Ask to see each claim working on your own data and ask for the limits in writing. Be cautious about unsupported certification or fiscal approval claims." },
        ],
      },
    ],
    related: ["features", "pricing", "faq", "resources"],
    ctaTitle: "Want us to walk through this checklist with you?",
    ctaBody: "We will answer each point for your business, including what is not built yet.",
  },
};

/** Hub page for /resources. */
export const resourcesHub = {
  metaTitle: "Resources and practical guides for hardware and trade shops | QuincaiSoft",
  description: "Practical, vendor-neutral guides for shops and distributors in Cameroon and CEMAC: bulk materials in FCFA, customer credit, supplier receiving, quotations, branches and mobile money.",
  kicker: "Resources",
  h1: "Practical guides for hardware, building-material and trade businesses",
  answer: "These guides explain how to run the daily work of a shop or distributor in Cameroon and the CEMAC region: tracking bulk materials in FCFA, managing credit, receiving supplier stock, quoting contractors, running several branches and reconciling mobile money. They are written to be useful with any system, and they say where QuincaiSoft helps.",
  listTitle: "Six guides",
  related: ["features", "industries", "compare", "faq"],
};

export const titles: Record<string, string> = {
  "mobile-app": "Mobile app",
  payments: "Payments and mobile money",
  "online-store": "Online store",
  security: "Security and data",
  desktop: "Windows desktop",
  "migration-and-onboarding": "Migration and onboarding",
  compare: "Choosing retail software for CEMAC",
  resources: "Resources and guides",
  "resources/track-cement-and-bulk-materials-fcfa": "How to track cement and bulk materials in FCFA",
  "resources/manage-customer-credit-hardware-shop": "Managing customer credit in a hardware shop",
  "resources/buy-and-receive-stock-from-suppliers": "Buying and receiving stock from suppliers",
  "resources/quotations-for-contractors": "Quotations for contractors",
  "resources/run-multiple-branches": "Running multiple branches",
  "resources/mobile-money-reconciliation-basics": "Mobile money reconciliation basics",
};
