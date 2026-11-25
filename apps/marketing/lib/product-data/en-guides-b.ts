import type { GuideData } from "../product-types";

export const guidesB: GuideData[] = [
  {
    slug: "quotations-for-contractors",
    icon: "file",
    keywords: ["quotation", "contractor", "deposit", "building materials", "FCFA", "quote to sale"],
    metaTitle: "Quotations for contractors: a practical guide",
    description: "How to quote contractors and project buyers in a building-material business: itemise clearly, set a validity period, take a deposit and turn the accepted quotation into a sale.",
    h1: "Quotations for contractors",
    answer: "A good quotation for a contractor lists every item with its unit, quantity and price in FCFA, states how long the prices are valid, says what is and is not included, and sets the payment terms, including any deposit. When it is accepted, convert that same quotation into the sale so the agreed prices and quantities carry through without retyping.",
    sections: [
      {
        h2: "Why contractors ask for quotations",
        paras: [
          "Contractors buy against a budget and a schedule. They need to know the cost of materials before they commit to a client, and often they need a document they can show to the client or to a bank. A clear quotation helps you win the order and avoids arguments later about what was agreed.",
        ],
      },
      {
        h2: "What a quotation should contain",
        paras: [
          "Keep it simple and complete. Anyone reading it should understand exactly what is offered, for how much, and until when.",
        ],
        list: [
          "Your business name and contact details, the customer's name and the project or site.",
          "A unique quotation number and the date.",
          "Each item with its description, unit, quantity, unit price and line total, in FCFA.",
          "The validity period, after which prices may change.",
          "What is included and excluded, such as delivery, cutting or unloading.",
          "Payment terms, including any deposit and when the balance is due.",
        ],
      },
      {
        h2: "Be exact about units",
        paras: [
          "Most disputes about quotations are about units. A price per bag is not a price per tonne. Write the unit beside every quantity and price, and make sure the quantity you quote is in the same unit as the price. If the contractor thinks in tonnes and you sell in bags, show both so there is no doubt.",
        ],
      },
      {
        h2: "Set a validity period",
        paras: [
          "Material prices change. Say how long the quotation is valid, and honour it inside that period. If a quotation expires, issue a new one with the current prices rather than editing the old one. Keeping the original unchanged protects both of you.",
        ],
      },
      {
        h2: "Ask for a deposit on larger orders",
        paras: [
          "A deposit shows commitment and helps you reserve or order stock. State the amount or percentage and when it is due in the quotation. Record the deposit as money you hold for the customer, then allocate it to the sale when the goods are delivered. Say clearly what happens if the order is cancelled.",
        ],
      },
      {
        h2: "Handle partial orders and changes",
        paras: [
          "Contractors often take materials in stages as the work progresses. Decide whether one quotation can be fulfilled in several deliveries and how the remaining quantities and prices are protected. If the contractor wants to change quantities, treat it as a revision with a new version, and keep the earlier one on file.",
        ],
      },
      {
        h2: "Turn the accepted quotation into a sale",
        paras: [
          "When the contractor accepts, convert the same quotation into the sale instead of retyping it. This keeps the quantities and prices identical and gives you a clear link from the quotation to the invoice. If prices have changed since the quotation, or it has expired, stop and re-quote.",
        ],
      },
      {
        h2: "Follow up",
        paras: [
          "Keep a list of open quotations with their value and expiry date. Contact the customer before the quotation expires, record whether it was accepted or lost, and note why. Over time this shows you which products and prices win orders.",
        ],
      },
      {
        h2: "How QuincaiSoft supports this",
        paras: [
          "QuincaiSoft drafts quotations from catalogue prices with an expiry date, issues them as numbered snapshots that cannot be edited, and converts an accepted quotation into one linked sale draft. Conversion is rejected if prices changed or the quotation expired, and currently does not handle tax-bearing quotations. Partial acceptance into several orders and project credit limits are planned.",
        ],
      },
    ],
    howTo: {
      name: "How to quote a contractor and turn the quotation into a sale",
      steps: [
        { name: "List every item exactly", text: "Give description, unit, quantity, unit price and total in FCFA, with the unit written beside each figure." },
        { name: "State validity and terms", text: "Give an expiry date, what is included and excluded, and the payment terms." },
        { name: "Ask for a deposit on larger orders", text: "Record the deposit as money held for the customer and allocate it on delivery." },
        { name: "Convert the accepted quotation", text: "Create the sale from the same quotation so quantities and prices carry through." },
        { name: "Follow up before expiry", text: "Track open quotations, contact the customer and record why each was won or lost." },
      ],
    },
    related: ["industries/construction-materials", "features", "resources/manage-customer-credit-hardware-shop"],
  },
  {
    slug: "run-multiple-branches",
    icon: "building",
    keywords: ["multi-branch", "stock transfer", "branches", "warehouses", "permissions", "retail management"],
    metaTitle: "Running multiple branches: stock, cash and control",
    description: "A practical guide to running several shops or yards: separate stock and cash per branch, transfer goods through transit, set permissions and review the same numbers everywhere.",
    h1: "Running multiple branches",
    answer: "To run several branches well, keep stock, cash and sales separate for each branch, move goods between branches through a recorded transfer that passes through transit, give each person access only to the branches and actions they need, and review the same few numbers for every branch on a fixed schedule. Without those habits, a second shop usually doubles the problems rather than the profit.",
    sections: [
      {
        h2: "Keep each branch's records separate",
        paras: [
          "Every branch needs its own stock, its own tills and its own sales record. Mixing them makes it impossible to know which branch is performing or where a shortage began. You can still look at the whole business together, but the underlying records should always say which branch they belong to.",
        ],
      },
      {
        h2: "Move stock with a transfer, not a phone call",
        paras: [
          "When goods move from one branch to another, record the transfer. Goods that have left one branch and not yet arrived at the other are in transit. They should not count as available at both ends. That single rule prevents selling the same item twice.",
        ],
        list: [
          "Draft the transfer with products and quantities.",
          "Have someone other than the requester approve it.",
          "Record the dispatch from the source branch.",
          "Record the receipt at the destination, in full or in part, with the quantity actually received.",
        ],
      },
      {
        h2: "Reconcile differences on receipt",
        paras: [
          "When the destination counts less than was dispatched, the difference should be recorded and investigated, not absorbed. Decide who is responsible for goods in transit and how quickly differences must be reported.",
        ],
      },
      {
        h2: "Give people only the access they need",
        paras: [
          "A cashier at one branch does not need to see another branch's cash. A manager might see their own branch but not change prices. Define a few roles, such as owner, branch manager, stock keeper, purchasing and cashier, and give each exactly the actions required.",
          "Require a second person to approve sensitive actions: large refunds, big stock write-offs, price changes and reopening a closed period. The person who starts the action should not be the one who approves it.",
        ],
      },
      {
        h2: "Close each branch's cash every day",
        paras: [
          "Each till should open with a known float and close with a counted amount compared with what the records expect. Record any difference with a reason. Doing this daily, branch by branch, finds problems while they are small and while people still remember what happened.",
        ],
      },
      {
        h2: "Review the same numbers for every branch",
        paras: [
          "Pick a short list and look at it weekly for each branch: sales, margin, stock value, stock differences from counts, amounts owed by customers and cash differences. Comparing branches against each other is more useful than looking at any one in isolation.",
        ],
      },
      {
        h2: "Share suppliers and prices, separate responsibility",
        paras: [
          "Central purchasing can win better terms, but each branch should still receive and count what arrives. Keep one catalogue and consistent units across branches so the same product means the same thing everywhere. Allow branch-level exceptions only when you have a reason.",
        ],
      },
      {
        h2: "How QuincaiSoft supports this",
        paras: [
          "QuincaiSoft scopes records by company and branch, tracks stock per warehouse, and supports transfers with approval, dispatch into transit and partial or final receipt, along with approved stock counts, cash sessions per till and role-based permissions with independent approvals. Self-service creation of companies and branches and consolidated multi-company views are planned.",
        ],
      },
    ],
    howTo: {
      name: "How to run multiple branches",
      steps: [
        { name: "Keep each branch's records separate", text: "Give every branch its own stock, tills and sales records." },
        { name: "Move stock with a recorded transfer", text: "Draft, approve, dispatch and receive, so goods in transit are not available at both ends." },
        { name: "Give people only the access they need", text: "Define a few roles and require a second approver for sensitive actions." },
        { name: "Close each branch's cash daily", text: "Compare counted cash with expected cash and record every difference with a reason." },
        { name: "Review the same numbers for every branch", text: "Look weekly at sales, margin, stock differences, debts and cash differences." },
      ],
    },
    related: ["industries/wholesale-distribution", "features", "security", "resources/buy-and-receive-stock-from-suppliers"],
  },
  {
    slug: "mobile-money-reconciliation-basics",
    icon: "phone",
    keywords: ["mobile money", "reconciliation", "MTN Mobile Money", "Orange Money", "payments", "FCFA"],
    metaTitle: "Mobile money reconciliation basics for shops",
    description: "The basics of reconciling mobile money payments in a shop: record references, separate typed from confirmed payments, compare three sources and investigate differences.",
    h1: "Mobile money reconciliation basics",
    answer: "To reconcile mobile money, record every payment with its provider reference, treat a payment as collected only when the provider confirms it, and compare three sources regularly: your own sales records, the provider's transaction list and your bank or wallet statement. Investigate every difference, such as a missing, duplicate, late or part payment, instead of adjusting your totals to match.",
    sections: [
      {
        h2: "Why reconciliation matters",
        paras: [
          "Mobile money is convenient, but it adds a second set of records outside your shop: the provider's. A customer may show you a message that looks like a payment. A network delay may mean a payment arrives later. Without regular checks, you can end up with sales you never collected, or money you cannot explain.",
        ],
      },
      {
        h2: "Understand what you are comparing",
        paras: [
          "Reconciliation compares what you believe you collected with what actually arrived. There are three views, and each can disagree with the others for a good reason.",
        ],
        list: [
          "Your sales records: what you recorded as paid by mobile money.",
          "The provider's records: the transactions and any settlement batches they report.",
          "Your bank or wallet statement: what reached your account, after fees.",
        ],
      },
      {
        h2: "Step 1: record a reference for every payment",
        paras: [
          "For each mobile money sale, record the amount, the date, the customer or sale it relates to, and the provider's transaction reference. A payment with no reference cannot be checked later.",
        ],
      },
      {
        h2: "Step 2: separate typed from confirmed",
        paras: [
          "A reference typed in by hand has not been verified, even if it looks right. Mark it as unconfirmed until the provider confirms the payment, and do not release high-value goods against an unconfirmed payment. Where your system can ask the provider for the status, wait for a confirmation that matches the amount and currency before you call the payment collected.",
          "Be careful with screenshots and forwarded messages. They are easy to fake and are not confirmation.",
        ],
      },
      {
        h2: "Step 3: handle uncertain outcomes",
        paras: [
          "Sometimes a request is sent and no clear answer returns. Do not charge the customer again. Check the status of the original request first, using its reference. Only if the provider says it failed should you ask for payment again.",
        ],
      },
      {
        h2: "Step 4: compare regularly",
        paras: [
          "At the end of each day or week, list the mobile money sales you recorded and match each to the provider's list by reference and amount. Then compare the provider's totals with what reached your account.",
        ],
      },
      {
        h2: "Step 5: investigate differences",
        paras: [
          "Each kind of difference has a likely cause. Work through them one by one and write down the outcome.",
        ],
        list: [
          "Recorded but not confirmed: the payment may have failed or been delayed.",
          "Confirmed but not recorded: a sale may have been missed.",
          "Duplicate: the same payment may have been recorded twice.",
          "Different amount: a part payment, an over-payment or a fee.",
          "Reversed: the provider may have reversed or disputed it.",
        ],
      },
      {
        h2: "Keep refunds honest",
        paras: [
          "Do not mark a mobile money refund as done until the provider confirms it. If a refund request fails or is unclear, keep it open and check its status before you try again.",
        ],
      },
      {
        h2: "How QuincaiSoft supports this",
        paras: [
          "QuincaiSoft is built so that a mobile money payment is shown as verified only when the provider confirms it, and an unclear request is queried again by the same reference instead of being charged twice. An MTN Mobile Money adapter exists but needs live credentials and provider acceptance. Orange Money is not integrated. Importing provider settlements for full three-way reconciliation is planned.",
        ],
      },
    ],
    howTo: {
      name: "How to reconcile mobile money payments",
      steps: [
        { name: "Record a reference for every payment", text: "Note amount, date, sale and the provider's transaction reference." },
        { name: "Separate typed from confirmed", text: "Mark hand-typed references unconfirmed until the provider confirms the payment." },
        { name: "Handle uncertain outcomes", text: "Check the original request's status before asking the customer to pay again." },
        { name: "Compare regularly", text: "Match your recorded payments to the provider's list, then compare provider totals with your account." },
        { name: "Investigate differences", text: "Work through missing, duplicate, late, part and reversed payments and write down each outcome." },
      ],
    },
    faq: [
      { q: "Is a screenshot of a payment message proof of payment?", a: "No. Screenshots and forwarded messages are easy to fake. Treat a payment as collected only when the provider confirms it." },
    ],
    related: ["payments", "features", "resources/manage-customer-credit-hardware-shop"],
  },
];
