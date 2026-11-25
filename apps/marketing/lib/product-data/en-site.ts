import type { FaqData, FeaturesData, IndustriesData, PricingData } from "../product-types";

const CTA = {
  ctaTitle: "Want to see how this fits your business?",
  ctaBody: "Tell us about your shop or distribution business. We reply by email in English or French and are open about what is ready and what is still being built.",
};

export const features: FeaturesData = {
  metaTitle: "Features: module by module | QuincaiSoft ERP and POS",
  description: "Every QuincaiSoft module explained: sales and POS, stock and units, purchasing, customer credit and deposits, quotations, returns, cash, documents, reports, branches and audit, each with an honest status.",
  kicker: "Features",
  h1: "Features, module by module, with an honest status",
  answer: "QuincaiSoft is an ERP and point of sale for hardware shops, building-material retailers and wholesalers in Cameroon and CEMAC. This page explains what each module does and how it works, and tags every module as available in the current migration slice, partly available or planned. We build in stages and do not claim a complete or certified product.",
  statusLegendTitle: "How to read the status tags",
  statusLegend: [
    { status: "available", body: "Implemented and tested in the current migration slice. It is not a claim of full coverage of the module or of production certification." },
    { status: "partial", body: "Some workflows exist and others are still to come. The note under each module says which." },
    { status: "planned", body: "Specified in the architecture and not built yet." },
    { status: "pending", body: "Built but disabled or waiting for an external step, such as provider acceptance or device evidence." },
  ],
  summaryTitle: "Modules at a glance",
  summaryCaption: "QuincaiSoft modules and their current status",
  summaryHead: ["Module", "Status"],
  modules: [
    {
      id: "sales-pos", icon: "cart", title: "Sales and point of sale", status: "available",
      what: "Sell at the counter or by assisted order. Each sale is priced from the catalogue, linked to a customer and warehouse, and posted as one transaction with its stock and accounting effects and its numbered invoice.",
      how: [
        "Lines are priced by the server from the catalogue, not by the browser, and quantities are exact.",
        "Posting needs a reason and a version check, so two people cannot post over each other.",
        "The invoice is issued in the same transaction as the stock and journal entries. If the invoice settings are missing, nothing is posted.",
        "Sending the same command twice returns the first result instead of creating a second sale.",
      ],
      statusNote: "Available for immediate zero-tax sales. Tax-bearing sales, split tenders at the counter and fiscal invoicing are planned.",
    },
    {
      id: "stock-units", icon: "boxes", title: "Stock, units and transformations", status: "available",
      what: "Know what you hold in each warehouse, move it between branches, count it, and turn one product into another by cutting or repacking.",
      how: [
        "Each product has one base unit that cannot be changed after creation. Quantities are exact, never rounded silently.",
        "Transfers go through draft, independent approval, dispatch into transit and partial or final receipt. Destination stock changes only on receipt.",
        "Physical counts take a snapshot, capture what was observed, need independent approval, and post a gain or loss with a journal entry.",
        "Cutting and repacking are approved jobs. Cutting keeps the length exact in millimetres, and the full carrying value moves to the output product.",
      ],
      statusNote: "Available, with zero-loss cutting and repacking only. Alternate pack units such as bag and tonne with exact conversion, kerf and offcut accounting, lots and serials are planned.",
    },
    {
      id: "purchasing", icon: "truck", title: "Purchasing and suppliers", status: "available",
      what: "Order from suppliers, receive goods against the order and record the supplier invoice, with stock and payables kept in step.",
      how: [
        "A purchase draft takes original supplier, cost and quantity snapshots. Ordering does not change stock.",
        "An independent approver approves the order. Goods receipts can be partial and cannot exceed the quantity ordered.",
        "Receiving increases stock and records goods received not yet invoiced. The supplier invoice for a receipt then creates the payable.",
        "Returning uninvoiced goods to a supplier is limited to what was actually received.",
      ],
      statusNote: "Available for orders, receipts, supplier invoices and uninvoiced returns. Requisitions, landed costs, supplier payments and ageing are planned.",
    },
    {
      id: "customers-credit", icon: "wallet", title: "Customers, credit and deposits", status: "available",
      what: "Sell on account, take advance deposits, collect payments and always see what each customer owes or has paid in advance.",
      how: [
        "A credit sale creates a receivable. A cash receipt is applied against recognised debt, and an over-payment is rejected.",
        "A deposit is held as a customer liability and allocated to a sale without recording a second cash receipt.",
        "A cash refund of an unused deposit needs its own permission and cannot exceed the remaining balance.",
        "Every one of these commands is idempotent and writes its journal entry in the same transaction.",
      ],
      statusNote: "Available. Customer credit limits, ageing reports and statements are planned.",
    },
    {
      id: "quotations", icon: "file", title: "Quotations", status: "available",
      what: "Prepare a quotation for a contractor or trade customer, issue it as a numbered document and convert the accepted one into a sale.",
      how: [
        "A draft is priced from the catalogue and carries an expiry date.",
        "Issuing freezes a numbered snapshot that later edits cannot change.",
        "Conversion creates one linked sale draft. It is rejected if the price changed, the quotation expired or tax applies, rather than altering the quoted amount.",
      ],
      statusNote: "Available. Partial acceptance into several orders, project budgets and credit sub-limits are planned.",
    },
    {
      id: "returns", icon: "undo", title: "Returns", status: "available",
      what: "Take back goods against an original sale, inspect them, and reverse revenue and cost correctly with a credit note.",
      how: [
        "A return draft refers to the original sale and cannot exceed the quantity previously sold less earlier returns.",
        "Inspection evidence is recorded, and a different person approves before posting.",
        "Posting returns resaleable stock, reverses revenue and cost, splits the value between receivable and customer credit, and issues a credit note linked to the original sale.",
      ],
      statusNote: "Available for sellable customer returns. A return does not by itself pay cash or a provider refund. Exchanges and warranty handling are planned.",
    },
    {
      id: "cash-expenses", icon: "banknote", title: "Cash and expenses", status: "available",
      what: "Open and close tills, record expenses with approval, and close accounting periods with controls.",
      how: [
        "A cash session opens with a declared float. Expected cash comes from the session's immutable cash records, and variance is the counted amount minus expected.",
        "Expenses go through draft, submission and independent approval or rejection, then are paid from an open cash session.",
        "Periods are closed or reopened by proposal and independent approval. Open tills and unapproved items block a close.",
      ],
      statusNote: "Available. Bank payouts and other payment channels for expenses are not built.",
    },
    {
      id: "documents", icon: "filecheck", title: "Documents and printing", status: "partial",
      what: "Issue numbered business documents that stay the same forever, and print them in English or French.",
      how: [
        "Each document is a snapshot with the issuer's identity, lines, amounts and a SHA-256 hash. Database rules prevent edits and deletes.",
        "Numbers are allocated per tenant, company, branch, kind, year and series, one at a time.",
        "Reprints are marked COPY and never post stock or money again. Credit notes point to the original sale.",
      ],
      statusNote: "Partly available: A4 print views in English and French and issuer settings exist. Thermal receipt printers and other document types are planned, and printer or weighing-scale compatibility needs real device tests.",
    },
    {
      id: "reports", icon: "chart", title: "Reports", status: "partial",
      what: "Read sales, stock, receivables, cash and mobile money requests for a period, with totals and CSV export.",
      how: [
        "Five reports exist: posted sales, stock, receivables, cash and provider payment requests.",
        "Date reports take an inclusive range of up to 367 days, and totals cover the whole filter, not only the visible page.",
        "Amounts are exact values. CSV export covers the page you request, and spreadsheet-formula-looking cells are neutralised.",
      ],
      statusNote: "Partly available: five reports. The remaining reports in the plan, ageing and consolidated views are planned.",
    },
    {
      id: "multi-branch", icon: "building", title: "Multi-branch and permissions", status: "partial",
      what: "Work across companies and branches, with each person seeing and doing only what their role allows.",
      how: [
        "Every record carries tenant, company and branch scope. Scope comes from a verified membership, never from a browser header.",
        "Role and activation changes need a request from one owner and approval by another. Applying one raises an authorisation epoch so older tokens stop working.",
        "Permissions are checked again on the server regardless of what the screen shows.",
      ],
      statusNote: "Partly available: organisation reads, identity requests and approvals. Self-service creation of companies and branches and full tenant provisioning are planned.",
    },
    {
      id: "audit", icon: "scroll", title: "Audit trail", status: "partial",
      what: "See who changed what, and rely on posted facts staying as they were.",
      how: [
        "The audit register is append-only. Database triggers prevent edits.",
        "Posted facts are corrected by reversal or adjustment that links to the original, never by overwriting.",
        "A background worker projects events into the audit register without ever posting business effects again.",
      ],
      statusNote: "Partly available: identity changes and projected events are audited. Instrumenting every command is planned.",
    },
  ],
  moreTitle: "Beyond the core modules",
  more: [
    { icon: "phone", status: "partial", title: "Mobile app", body: "Included in every plan. Sign-in, catalogue, sales and offline sale drafts work today. Offline posting is disabled." },
    { icon: "card", status: "partial", title: "Payments", body: "Cash, credit and deposits are available. The MTN adapter awaits live credentials. Orange Money is not integrated." },
    { icon: "store", status: "partial", title: "Online store", body: "An optional public catalogue. Checkout is not available yet." },
    { icon: "monitor", status: "pending", title: "Windows desktop", body: "Existing Windows software stays standalone. The optional sync adapter awaits integration." },
  ],
  limits: [
    "QuincaiSoft is in active development. Source presence is not acceptance of a workflow, and no production certification is claimed.",
    "No DGI fiscal approval, OHADA conformity or other certification is claimed.",
    "Offline posting of completed sales on mobile is disabled until signed quotas and device evidence exist.",
  ],
  related: ["industries", "pricing", "mobile-app", "payments", "security", "faq"],
  ...CTA,
};

export const industries: IndustriesData = {
  index: {
    icon: "hardhat",
    kicker: "Industries",
    metaTitle: "Industries: hardware, building materials and wholesale | QuincaiSoft",
    description: "Real workflows for hardware shops, construction-material retailers and wholesalers in Cameroon and CEMAC: units, cutting and repacking, credit, quotations, purchasing, with an honest status for each.",
    h1: "Industries: workflows for hardware, building materials and wholesale",
    answer: "QuincaiSoft is designed for businesses that sell many products in many units, serve walk-in and trade customers, and often run more than one site. The pages below describe real workflows per sector, such as cutting a bar to length, selling on credit or quoting a contractor, and mark each as available or planned.",
    blocks: [
      {
        kind: "table", id: "matrix", title: "Workflows by sector",
        caption: "Workflows, their status and where they matter most",
        head: ["Workflow", "Status", "Matters most for"],
        rows: [
          ["Counter sales and numbered invoices", "Available", "All sectors"],
          ["Quotations converted to sales", "Available", "Construction materials"],
          ["Customer credit and deposits", "Available", "Construction materials, wholesale"],
          ["Stock counts and transfers with transit", "Available", "All sectors, especially multi-branch"],
          ["Purchasing, receiving, supplier invoices", "Available", "Wholesale, hardware"],
          ["Cutting to length (zero-loss)", "Partly available", "Hardware, construction materials"],
          ["Repacking bulk into packs (zero-loss)", "Partly available", "Hardware, wholesale"],
          ["Alternate pack units such as bag and tonne", "Planned", "Construction materials"],
          ["Price lists by customer group", "Planned", "Wholesale"],
          ["Delivery handover and project credit limits", "Planned", "Construction materials"],
        ],
      },
      {
        kind: "text", id: "how", alt: true, title: "How we describe workflows",
        paras: [
          "We describe what exists in the current migration slice and label everything else as planned. Where a workflow has a current limit, such as zero-loss cutting only, we say so on the sector page.",
        ],
      },
    ],
    related: ["features", "compare", "resources", "pricing"],
    sectorsTitle: "Choose your sector",
    sectorLink: "See the workflows",
    sectors: {
      "hardware-shops": { summary: "Large catalogues in small units, quick counter sales, cutting and repacking, daily cash control." },
      "construction-materials": { summary: "Quotations, deposits and credit for contractors, bulk materials and several yards." },
      "wholesale-distribution": { summary: "Purchasing discipline, supplier invoices, transfers between warehouses and trade customers on account." },
    },
    ...CTA,
  },
  items: {
    "hardware-shops": {
      icon: "hammer",
      kicker: "Hardware shops",
      metaTitle: "Hardware shop software for Cameroon | QuincaiSoft",
      description: "QuincaiSoft for hardware shops in Cameroon: units, cutting to length, repacking, fast counter sales, stock counts, credit regulars and daily cash control in FCFA.",
      h1: "Hardware shop software: fast counter, honest stock, clean cash",
      answer: "A hardware shop sells thousands of small items in different units, cuts pipe and bars to length, repacks screws and nails, and serves regulars on credit. QuincaiSoft supports counter sales, exact stock quantities, cutting and repacking jobs, stock counts, credit and daily cash control in FCFA, and marks planned items such as pack-unit conversion as planned.",
      glance: {
        title: "In a hardware shop",
        items: [
          { label: "Counter sale and receipt", status: "available", note: "Numbered invoice, COPY reprints." },
          { label: "Cut to length and repack", status: "partial", note: "Zero-loss jobs with approval." },
          { label: "Stock counts", status: "available", note: "Snapshot, count, approve, post." },
          { label: "Barcode scanning", status: "planned", note: "Specified, not delivered." },
        ],
      },
      blocks: [
        {
          kind: "cards", id: "workflows", title: "Workflows that matter in a hardware shop",
          items: [
            { icon: "package", status: "available", title: "Catalogue and pricing", body: "Create products with SKU, name, one base unit and a selling price in whole FCFA.", points: ["Choose the smallest unit you stock as the base unit. It cannot be changed later.", "Alternate pack units such as box or carton with exact conversion are planned."] },
            { icon: "cart", status: "available", title: "Counter sale", body: "Priced by the server, posted with its stock effect and a numbered invoice in one step.", points: ["Reprint a copy marked COPY without posting again.", "Repeating a submission cannot create a second sale."] },
            { icon: "scissors", status: "partial", title: "Cutting to length", body: "Turn one piece into whole pieces of another product, with the length kept exact in millimetres.", points: ["An independent approver approves the job.", "Kerf, waste and offcut accounting are planned."] },
            { icon: "package", status: "partial", title: "Repacking", body: "Turn bulk stock into pack products using an approved recipe with exact quantities.", points: ["For example, nails received in bulk and sold in packs.", "Measured loss during repacking is planned."] },
            { icon: "clipboard", status: "available", title: "Stock counts", body: "Count a product in a warehouse and find where differences arise.", points: ["A change since the snapshot blocks posting.", "A shortage cannot silently use up reserved stock."] },
            { icon: "wallet", status: "available", title: "Regulars on credit", body: "Sell on account and collect later with receipts, always seeing the balance.", points: ["An over-payment is rejected."] },
            { icon: "banknote", status: "available", title: "End-of-day cash", body: "Open the till with a float and close with a counted amount against derived expected cash." },
          ],
        },
        {
          kind: "steps", id: "example", alt: true, title: "Worked example: cutting a bar",
          intro: "An illustration with made-up quantities, showing how a cutting job behaves today.",
          items: [
            { title: "Receive 6,000 mm bars", body: "Bars are stocked as one product, for example a 6 m bar, counted as pieces." },
            { title: "Create a cutting job", body: "Source: one 6 m bar. Output: a separate 2 m product, three whole pieces. The job checks that 3 x 2,000 mm equals 6,000 mm exactly." },
            { title: "Approve it", body: "A different person approves the recipe and evidence reference." },
            { title: "Post", body: "Stock moves from the source to the output and the full carrying value moves with it. Kerf is not accounted for yet, so the job is zero-loss." },
          ],
        },
        {
          kind: "limits", id: "limits", title: "Honest limits",
          items: [
            "Cutting and repacking are zero-loss. Kerf, waste and offcut tracking are planned.",
            "Barcode scanning is not in the delivered slice.",
            "Printer, scanner and scale compatibility needs real device tests and is not claimed.",
          ],
        },
        {
          kind: "faq", id: "faq", alt: true, title: "Hardware shop questions",
          items: [
            { q: "Can I cut pipe or iron bars to length?", a: "Yes, as an approved zero-loss cutting job: one source piece becomes whole pieces of another product with exact millimetre length. Kerf and waste accounting are planned." },
            { q: "Can I sell nails by the kilo and in packs?", a: "You can repack bulk stock into pack products with an approved recipe. Zero-loss only for now." },
            { q: "Does it support barcode scanners?", a: "Barcodes are in the architecture but scanning is not in the delivered slice." },
          ],
        },
      ],
      related: ["industries/construction-materials", "features", "resources/track-cement-and-bulk-materials-fcfa", "resources/manage-customer-credit-hardware-shop"],
      ...CTA,
    },
    "construction-materials": {
      icon: "hardhat",
      kicker: "Construction materials",
      metaTitle: "Software for building-material retailers in Cameroon | QuincaiSoft",
      description: "QuincaiSoft for construction-material retailers in Cameroon: contractor quotations, deposits, credit accounts, bulk units such as cement bags and tonnes, and stock across yards.",
      h1: "Building-material software: quotations, deposits and bulk units",
      answer: "Cement, iron bars, roofing and tiles are often sold against a quotation, a deposit or an account, and in units such as bags, bars, metres and tonnes. QuincaiSoft supports quotations that convert into sales, deposits, credit accounts and stock across several yards today, and marks bag-to-tonne conversion, delivery handover and project credit limits as planned.",
      glance: {
        title: "In a building-material business",
        items: [
          { label: "Quotation to sale", status: "available", note: "Numbered snapshot, linked conversion." },
          { label: "Deposits and credit", status: "available", note: "Balances and allocations in FCFA." },
          { label: "Several yards", status: "available", note: "Warehouse stock and transfers." },
          { label: "Bag and tonne conversion", status: "planned", note: "Exact conversion is specified." },
        ],
      },
      blocks: [
        {
          kind: "cards", id: "workflows", title: "Workflows that matter in building materials",
          items: [
            { icon: "file", status: "available", title: "Contractor quotations", body: "Draft with catalogue prices and an expiry date, issue a numbered snapshot and convert the accepted quotation into a linked sale.", points: ["Conversion is rejected if the price changed or the quotation expired."] },
            { icon: "landmark", status: "available", title: "Deposits and staged payments", body: "Take a deposit, keep it as a liability and allocate it to the sale without another cash receipt.", points: ["A cash refund of an unused deposit needs a separate permission."] },
            { icon: "wallet", status: "available", title: "Customer accounts", body: "Sell on credit and apply receipts to recognised debt, with the balance always visible." },
            { icon: "ruler", status: "planned", title: "Bag, tonne and other units", body: "Sell the same material by bag or tonne with exact conversion.", points: ["Today each product has one base unit.", "Non-exact conversions are specified to be rejected, not rounded."] },
            { icon: "warehouse", status: "available", title: "Several yards", body: "Stock per warehouse, with transfers that go through approval, transit and receipt." },
            { icon: "undo", status: "available", title: "Returns with inspection", body: "Take back unused material against the original sale with a credit note." },
            { icon: "truck", status: "planned", title: "Delivery handover and project limits", body: "Delivery evidence, project budgets and credit sub-limits are specified and not built." },
          ],
        },
        {
          kind: "text", id: "units", alt: true, title: "Worked example: cement in bags and tonnes",
          paras: [
            "If a bag weighs 50 kg, a tonne is 20 bags. Today, choose the bag as the base unit, receive and sell in bags, and keep a separate bulk product if you also stock loose material, moving it into bags with an approved repack recipe.",
            "Automatic conversion on purchase and sale lines, so that a delivery in tonnes becomes bags exactly, is planned. The specification rejects any conversion that does not divide exactly instead of rounding stock.",
          ],
        },
        {
          kind: "steps", id: "quotation", title: "Worked example: from quotation to paid sale",
          items: [
            { title: "Quote", body: "Draft the quotation with catalogue prices and an expiry date, then issue it as a numbered snapshot." },
            { title: "Take a deposit", body: "When the contractor accepts, record the deposit. It is held as a customer liability." },
            { title: "Convert", body: "Convert the accepted quotation into one linked sale draft. Changed prices or an expired quotation are rejected." },
            { title: "Post and allocate", body: "Post the sale and allocate the deposit to it without recording a second cash receipt." },
            { title: "Collect the balance", body: "Apply later receipts against the remaining receivable until it is cleared." },
          ],
        },
        {
          kind: "limits", id: "limits", alt: true, title: "Honest limits",
          items: [
            "Quotation conversion rejects tax-bearing quotations for now.",
            "Partial acceptance into several orders and project credit limits are planned.",
            "Delivery handover evidence is planned.",
          ],
        },
        {
          kind: "faq", id: "faq", title: "Building-material questions",
          items: [
            { q: "Can I sell cement by the bag and by the tonne?", a: "Today each product has one base unit, so choose the bag and sell in bags. Automatic bag-to-tonne conversion is planned." },
            { q: "Can a contractor pay a deposit first?", a: "Yes. A deposit is held as a customer liability and allocated to the sale later without a second cash receipt." },
          ],
        },
      ],
      related: ["industries/hardware-shops", "industries/wholesale-distribution", "resources/quotations-for-contractors", "resources/track-cement-and-bulk-materials-fcfa"],
      ...CTA,
    },
    "wholesale-distribution": {
      icon: "warehouse",
      kicker: "Wholesale and distribution",
      metaTitle: "Wholesale and distribution software for CEMAC | QuincaiSoft",
      description: "QuincaiSoft for wholesalers and distributors in Cameroon and CEMAC: purchasing with approval, partial receipts, supplier invoices, transfers between warehouses and trade customers on account.",
      h1: "Wholesale and distribution software: purchasing, stock and trade accounts",
      answer: "A wholesaler lives on purchasing discipline, stock visibility and trade credit. QuincaiSoft supports approved purchase orders, partial goods receipts, supplier invoices, transfers through transit, customer credit and reports today, and marks price lists by customer group and supplier payments as planned.",
      glance: {
        title: "In a wholesale business",
        items: [
          { label: "Purchasing cycle", status: "available", note: "Order, approve, receive, invoice." },
          { label: "Transfers between warehouses", status: "available", note: "Approval, transit and receipt." },
          { label: "Trade customers on account", status: "available", note: "Receivables and receipts." },
          { label: "Price lists by customer group", status: "planned", note: "Specified, not delivered." },
        ],
      },
      blocks: [
        {
          kind: "cards", id: "workflows", title: "Workflows that matter in wholesale",
          items: [
            { icon: "truck", status: "available", title: "Purchasing cycle", body: "Create a purchase draft, submit it, approve it independently and receive goods in one or several deliveries.", points: ["Ordering does not change stock. Receiving does.", "A receipt cannot exceed the quantity ordered."] },
            { icon: "receipt", status: "available", title: "Supplier invoices", body: "Record the supplier invoice against a receipt, which creates the payable.", points: ["One invoice per receipt, with a unique supplier reference.", "Supplier payments and ageing are planned."] },
            { icon: "transfer", status: "available", title: "Transfers between warehouses", body: "Move stock with approval, dispatch into transit and partial or final receipt.", points: ["Stock is not available at both ends at once."] },
            { icon: "wallet", status: "available", title: "Trade customers on account", body: "Sell on credit, apply receipts and see outstanding receivables per sale." },
            { icon: "chart", status: "partial", title: "Reports", body: "Posted sales with margin, stock value, receivables and cash, with totals across the whole filter." },
            { icon: "layers", status: "planned", title: "Price lists by customer group", body: "Different prices for retail, trade and wholesale customers.", points: ["Today the catalogue holds one selling price per product."] },
          ],
        },
        {
          kind: "steps", id: "example", alt: true, title: "Worked example: from purchase order to payable",
          items: [
            { title: "Draft and submit", body: "A purchasing officer drafts the order with supplier, quantities and unit costs and submits it." },
            { title: "Approve", body: "A different person approves. The initiator cannot approve their own order." },
            { title: "Receive in parts", body: "Each delivery is a receipt. Stock increases and goods received not yet invoiced are recorded." },
            { title: "Record the invoice", body: "When the supplier invoices a receipt, the payable is created from it." },
            { title: "Return what is wrong", body: "Uninvoiced goods can be returned up to what was received, and the stock is reduced at current weighted value." },
          ],
        },
        {
          kind: "limits", id: "limits", title: "Honest limits",
          items: [
            "Supplier payment allocation, ageing, landed costs and requisitions are planned.",
            "Price lists by customer group are planned.",
            "Cross-company trade between your own legal entities is planned.",
          ],
        },
        {
          kind: "faq", id: "faq", alt: true, title: "Wholesale questions",
          items: [
            { q: "Can I receive a delivery in parts?", a: "Yes. Receipts can be partial and the order closes only when everything ordered has been received." },
            { q: "Can I give trade customers special prices?", a: "Price lists by customer group are planned. Today the catalogue holds one selling price per product." },
          ],
        },
      ],
      related: ["industries/construction-materials", "resources/buy-and-receive-stock-from-suppliers", "resources/run-multiple-branches", "features"],
      ...CTA,
    },
  },
};

export const pricing: PricingData = {
  icon: "calculator",
  kicker: "Pricing",
  metaTitle: "Pricing and plan structure | QuincaiSoft",
  description: "QuincaiSoft plan structure: Starter, Growth, Enterprise and an optional online store, in several deployment modes, with the mobile app included. Contact us for pricing.",
  h1: "Pricing: plan structure, with no invented prices",
  answer: "We have not published a price list. Contact us for pricing. This page shows how QuincaiSoft is structured: Starter, Growth and Enterprise packages, an optional online store, several deployment modes, and the mobile app included in every package. Prices depend on your branches, users and modules and on how you deploy, and no price, trial or discount is implied here.",
  glance: {
    title: "At a glance",
    items: [
      { label: "Price", note: "Contact us for pricing." },
      { label: "Mobile app", note: "Included in every package." },
      { label: "Online store", status: "planned", note: "Optional add-on." },
      { label: "Deployment", note: "Cloud, dedicated, customer-hosted or Windows." },
    ],
  },
  blocks: [
    {
      kind: "cards", id: "plans", title: "Plan structure",
      intro: "The packages below describe the intended structure. Limits and contents may change as the product matures, and nothing here is an offer.",
      items: [
        { icon: "store", status: "planned", title: "Starter", body: "For one or a few branches.", points: ["Inventory and point of sale", "Customer records and reports", "Mobile app", "Baseline support"] },
        { icon: "building", status: "planned", title: "Growth", body: "For several branches.", points: ["Multi-branch operation", "Purchasing and payables", "Credit, deposits and role approvals", "Controlled offline mobile", "Advanced reports and integrations"] },
        { icon: "server", status: "planned", title: "Enterprise / Dedicated", body: "For larger or more demanding deployments.", points: ["Isolated database and runtime", "Advanced API scope", "Contractual service level", "Dedicated onboarding and support", "Custom industry entitlements"] },
        { icon: "cart", status: "planned", title: "Online store (optional)", body: "A tenant storefront, added where contracted.", points: ["Separate from the ERP core", "Merchant payment-provider charges are separate"] },
      ],
    },
    {
      kind: "table", id: "compare", alt: true, title: "Compare the packages",
      caption: "What each package is intended to include",
      head: ["Capability", "Starter", "Growth", "Enterprise / Dedicated"],
      rows: [
        ["Branches", "One or a few", "Several", "As contracted"],
        ["Inventory and point of sale", "Included", "Included", "Included"],
        ["Customer records and reports", "Included", "Included", "Included"],
        ["Mobile app", "Included", "Included", "Included"],
        ["Purchasing and payables", "Not included", "Included", "Per contract"],
        ["Credit, deposits and role approvals", "Not included", "Included", "Per contract"],
        ["Controlled offline mobile", "Not included", "Included (when released)", "Per contract"],
        ["Advanced reports and integrations", "Not included", "Included", "Per contract"],
        ["Isolated database and runtime", "Not included", "Not included", "Included"],
        ["Contractual service level", "Not included", "Not included", "Included"],
        ["Online store", "Optional", "Optional", "Optional"],
      ],
    },
    {
      kind: "table", id: "deployment", title: "Deployment modes",
      intro: "A package is combined with a deployment mode. Not every mode is available today.",
      caption: "Deployment modes and how they are licensed",
      head: ["Mode", "Where it runs", "Commercial form", "Status"],
      rows: [
        ["Standard cloud", "Shared, tenant-isolated cloud service", "Subscription", "In development"],
        ["Dedicated cloud", "Isolated deployment with its own database", "Enterprise subscription", "Planned"],
        ["Customer-hosted", "Your private cloud or on-premises server", "Software licence plus maintenance", "Planned"],
        ["Standalone Windows", "Your PC, local database, no cloud needed", "Perpetual desktop licence", "Existing software"],
        ["Cloud-connected Windows", "Windows site coordinated with the cloud", "Desktop plus connector entitlement", "Planned"],
      ],
    },
    {
      kind: "cards", id: "included", alt: true, title: "What is the same in every package",
      items: [
        { icon: "phone", title: "Mobile app", body: "Every package includes mobile access to the functions you subscribe to. Price follows scale and entitlements, not a separate app." },
        { icon: "layers", title: "One product", body: "A package is a set of entitlements. There is no separate codebase per plan." },
        { icon: "scroll", title: "Records are never altered by billing", body: "A plan change is an audited event. Historical financial records are not changed because of billing." },
        { icon: "globe", title: "English, French and FCFA", body: "Interface and documents in English and French, with FCFA as the working currency." },
      ],
    },
    {
      kind: "cards", id: "factors", title: "What affects the price",
      items: [
        { icon: "users", title: "Scale", body: "Number of branches, warehouses and staff seats." },
        { icon: "list", title: "Modules", body: "Which modules and integrations you need." },
        { icon: "cloud", title: "Deployment", body: "Shared cloud, dedicated cloud, customer-hosted or Windows." },
        { icon: "handshake", title: "Support and onboarding", body: "Level of onboarding, training and support." },
        { icon: "server", title: "Costs that are separate", body: "Hosting, hardware, payment-provider charges for online sales and implementation travel are itemised separately." },
      ],
    },
    {
      kind: "faq", id: "faq", alt: true, title: "Pricing questions",
      items: [
        { q: "How much does QuincaiSoft cost?", a: "We have not published prices. Contact us for pricing. It depends on branches, users, modules and deployment." },
        { q: "Is there a free trial or a discount?", a: "We do not publish trials or discounts that are not defined. Ask us about a pilot." },
        { q: "Is the mobile app extra?", a: "No. Every package includes mobile access to the functions you subscribe to." },
        { q: "Is the online store included?", a: "It is optional and added where contracted. Checkout is not available yet." },
        { q: "Do I need the cloud?", a: "Not necessarily. Standalone Windows works without a cloud account, and customer-hosted and dedicated modes are planned." },
        { q: "Can I change plan later?", a: "That is the design. A plan change is an audited, dated entitlement change, and historical records are not altered." },
      ],
    },
  ],
  related: ["features", "compare", "faq", "migration-and-onboarding"],
  ctaTitle: "Contact us for pricing",
  ctaBody: "Tell us about your branches, users and the modules you need, and we reply with a proposal.",
  formTitle: "Request a quote",
};

const G = {
  about: "About QuincaiSoft",
  stock: "Stock, units and purchasing",
  sales: "Sales, credit and documents",
  payments: "Payments and mobile money",
  mobile: "Mobile and offline",
  security: "Security, data and compliance",
  deployment: "Deployment and Windows",
  pricing: "Pricing and availability",
};

export const faq: FaqData = {
  metaTitle: "FAQ: answers about QuincaiSoft | QuincaiSoft",
  description: "Answers about QuincaiSoft: stock and units, cement bags and tonnes, credit and deposits, MTN and Orange Money, offline use, security, Windows, pricing and availability.",
  kicker: "FAQ",
  h1: "Frequently asked questions",
  answer: "Short, direct answers about QuincaiSoft, grouped by topic. Each answer says plainly what is available today and what is planned.",
  groups: [
    {
      id: "about", title: G.about,
      items: [
        { q: "What is QuincaiSoft?", a: "QuincaiSoft is an ERP and point of sale for retail, trade and distribution businesses, developed by Opesware Technologies. It covers catalogue, stock, quotations, purchasing, sales, cash and business documents." },
        { q: "Who is QuincaiSoft for?", a: "It is designed for hardware shops, construction-material retailers and wholesalers or distributors in Cameroon and the CEMAC region, including businesses with several branches." },
        { q: "Who develops QuincaiSoft?", a: "Opesware Technologies develops QuincaiSoft for retail, trade and distribution businesses in Cameroon and the CEMAC region." },
        { q: "Is QuincaiSoft ready for production use?", a: "It is in active development. Several modules exist, but not every workflow is complete and no production release certification is claimed. Contact us to discuss a pilot." },
        { q: "Where can I get a demonstration?", a: "Use the contact page to request a demonstration or a pilot discussion. We reply by email in English or French." },
      ],
    },
    {
      id: "stock", title: G.stock,
      items: [
        { q: "Can it manage several branches and warehouses?", a: "Yes. Stock, sales and cash are organised per branch and warehouse, with consolidated reporting for authorised users." },
        { q: "Can I sell cement by the bag and by the tonne?", a: "Today each product has one base unit, so you would sell cement in bags. Automatic conversion between bags and tonnes, with exact arithmetic, is planned." },
        { q: "Can I cut or repack products?", a: "Yes, as approved jobs. Cutting turns one piece into whole pieces of another product with exact millimetre length, and repacking turns bulk into packs. Both are zero-loss for now, and kerf, waste and measured loss are planned." },
        { q: "Can I count stock and move it between branches?", a: "Yes. Counts take a snapshot, need independent approval and post gains or losses. Transfers go through approval, transit and receipt." },
        { q: "Does it handle purchasing and suppliers?", a: "Yes. Supplier orders, goods receipts and supplier balances are connected to stock." },
      ],
    },
    {
      id: "sales", title: G.sales,
      items: [
        { q: "Does it work in FCFA?", a: "Yes. FCFA (XAF) is the working currency, and amounts and documents are presented accordingly." },
        { q: "Is it available in French and English?", a: "Yes. The interface and business documents are designed to be bilingual, English and French." },
        { q: "Can I prepare quotations?", a: "Yes. Quotations are part of the product, including follow-up and conversion to sales, together with customer accounts, deposits and returns." },
        { q: "Can I sell on credit and take deposits?", a: "Yes. Credit sales, receipts, deposits, allocations to sales and approved deposit refunds are available. Credit limits and ageing are planned." },
        { q: "Can I print receipts and invoices?", a: "Numbered documents have an A4 print view in English and French, and reprints are marked COPY. Thermal receipt printers need real device tests and are not claimed yet." },
        { q: "Can I take returns?", a: "Yes. A return refers to the original sale, is inspected and independently approved, and issues a credit note. It does not by itself pay cash or a provider refund." },
      ],
    },
    {
      id: "payments", title: G.payments,
      items: [
        { q: "Does it support MTN Mobile Money?", a: "Partly. An MTN payment intent and reconciliation adapter exists, but live provider credentials and acceptance are still required. A payment is not shown as verified until the provider confirms it." },
        { q: "Does it support Orange Money?", a: "Not yet. Orange Money is pending a contracted integration and is not marked as integrated. References can be recorded manually, but they are never labelled as verified." },
        { q: "Does QuincaiSoft hold my customers' money?", a: "No. Payments belong to the merchant and its licensed payment provider. Opesware Technologies is not a custodian of merchant cash or settlements." },
      ],
    },
    {
      id: "mobile", title: G.mobile,
      items: [
        { q: "Is there a mobile app?", a: "Yes. A native mobile app is included in every package. It signs in securely, searches the catalogue, shows sales and keeps offline sale drafts." },
        { q: "Does it work offline?", a: "The mobile client is designed for weak connectivity and is released in stages: online with a read-only cached catalogue first, then offline stock counts, then controlled offline cash sales. Offline posting of sales is currently disabled until the required quota and device evidence is complete." },
      ],
    },
    {
      id: "security", title: G.security,
      items: [
        { q: "Is my data separate from other businesses?", a: "It is designed to be. Tenant data is isolated in PostgreSQL with forced row-level security, and a request without a verified tenant fails closed." },
        { q: "Can I limit what each person can do?", a: "Yes. Roles and permissions are enforced on the server, and sensitive changes need an independent approver." },
        { q: "Is QuincaiSoft approved by the DGI for fiscal invoicing?", a: "No approval is claimed. Fiscal rules are configurable, but approval must come from the competent authorities and your accountants." },
        { q: "Does it have OHADA, ISO or other certifications?", a: "No certification is claimed. Compliance designations require review by counsel and accountants and have not been obtained." },
        { q: "Can I export my data?", a: "Reports can be exported to CSV page by page today. A full machine-readable export of your records is planned." },
      ],
    },
    {
      id: "deployment", title: G.deployment,
      items: [
        { q: "Can it connect to my existing Windows software?", a: "A desktop bridge for existing Windows installations is planned but its integration is pending. Existing Windows software continues to work on its own." },
        { q: "Do I have to use the cloud?", a: "No. Standalone Windows needs no cloud account. Customer-hosted and dedicated modes are planned." },
        { q: "Can I sell online?", a: "An optional online store can publish your catalogue. Checkout and online payment are not available yet." },
        { q: "Can I import my existing products and customers?", a: "A supervised import with a dry run is planned. Today onboarding is arranged with our team." },
      ],
    },
    {
      id: "pricing", title: G.pricing,
      items: [
        { q: "How much does it cost?", a: "We have not published prices. Contact us for pricing; it depends on the number of branches, users and modules." },
        { q: "Is there a free trial?", a: "We do not publish trials that are not defined. Ask us about a pilot." },
        { q: "Which plans exist?", a: "The planned structure is Starter, Growth and Enterprise, with an optional online store. The mobile app is included in every package." },
      ],
    },
  ],
  related: ["features", "pricing", "compare", "resources"],
  ...CTA,
};
