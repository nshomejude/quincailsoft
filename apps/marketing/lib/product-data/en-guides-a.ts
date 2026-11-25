import type { GuideData } from "../product-types";

export const guidesA: GuideData[] = [
  {
    slug: "track-cement-and-bulk-materials-fcfa",
    icon: "ruler",
    keywords: ["cement stock", "bulk materials", "units of measure", "FCFA", "hardware shop", "inventory"],
    metaTitle: "How to track cement and bulk materials in FCFA",
    description: "A practical guide to tracking cement, sand, iron bars and other bulk materials: choose a base unit, convert exactly, count regularly and value stock in FCFA.",
    h1: "How to track cement and bulk materials in FCFA",
    answer: "To track cement and other bulk materials reliably, choose one base unit per product, such as the bag for cement, record every receipt and sale in that unit, convert between units only with exact arithmetic, count stock on a regular schedule, and value stock in whole FCFA at your average cost. Most losses come from mixing units, not from the material itself.",
    sections: [
      {
        h2: "Why bulk materials go wrong",
        paras: [
          "Cement arrives in tonnes on the truck, is stored as bags, and is sold as bags, half-bags or by weight. Iron bars arrive as bundles and are sold as single bars or cut lengths. Every time the unit changes, a quantity can be misread, rounded or recorded twice.",
          "The goal of tracking is simple: at any moment, the number in your records should match what is physically in the yard, and you should know why when it does not.",
        ],
      },
      {
        h2: "Step 1: choose one base unit for each product",
        paras: [
          "The base unit is the smallest unit you want to count exactly. For cement it is usually the bag. For nails it might be the kilogram or the piece. For roofing sheets it is the sheet.",
          "Pick it once and keep it. Changing the unit of a product that already has stock history is how records become unreliable, so decide carefully before the first receipt.",
        ],
      },
      {
        h2: "Step 2: write down the conversions",
        paras: [
          "List every other unit you buy or sell in, and how it relates to the base unit. For example, if a bag of cement weighs 50 kg, a tonne is 20 bags. A bundle of bars might be a fixed number of pieces. Write the rule down and agree on it with your supplier and your staff.",
          "A good rule is exact: the conversion must give a whole number of base units. If a delivery of 1.5 tonnes would give 30 bags, that is fine. If a conversion gives a fraction of a base unit, stop and find out why instead of rounding.",
        ],
      },
      {
        h2: "Step 3: record receipts against the delivery note",
        paras: [
          "Count what is actually delivered, in the base unit, and compare it with the delivery note and the order. Record the quantity you counted, not the quantity on the paper. Note any damaged bags separately so they do not enter saleable stock.",
        ],
        list: [
          "Count on arrival, with the driver present if possible.",
          "Record the supplier, date and delivery note number.",
          "Keep damaged or short items out of saleable stock until the supplier agrees.",
        ],
      },
      {
        h2: "Step 4: handle loose and repacked material",
        paras: [
          "Sometimes bulk material is sold loose or repacked into smaller packs. Treat the loose material and each pack size as separate products, and move stock between them with a recorded repacking step. The quantity going out of the bulk product should equal the quantity going into the packs, plus any loss you measure and write down.",
          "Never adjust the quantity of one product because you repacked into another without a record. That is how stock vanishes on paper.",
        ],
      },
      {
        h2: "Step 5: count on a schedule",
        paras: [
          "Count the fast-moving and high-value products often, and the rest less often. A count should compare what you find with what the records say, and record the difference with a reason. Investigate large differences before you correct them.",
          "Take the count at a quiet time, or note any sales made during the count, so that movement does not distort the result.",
        ],
      },
      {
        h2: "Step 6: value the stock in FCFA",
        paras: [
          "Value stock at what it cost you, using a consistent method such as weighted average cost. Keep amounts in whole FCFA, and keep the cost of a sale frozen at the time of the sale so that later price changes do not rewrite past margins.",
          "When transport or handling costs are significant, decide how you will include them and apply the same rule every time.",
        ],
      },
      {
        h2: "How QuincaiSoft supports this",
        paras: [
          "QuincaiSoft keeps one base unit per product with exact quantities, approved stock counts that post gains and losses, and approved repacking and cutting jobs. Automatic conversion between units such as bags and tonnes is planned and is not available today. See the features page for the current status of each module.",
        ],
      },
    ],
    howTo: {
      name: "How to track cement and bulk materials in FCFA",
      steps: [
        { name: "Choose one base unit per product", text: "Pick the smallest unit you want to count exactly, such as the bag for cement, and keep it." },
        { name: "Write down the conversions", text: "List other units and their exact relation to the base unit, for example 20 bags to a tonne for 50 kg bags." },
        { name: "Record receipts against the delivery note", text: "Count what is delivered in the base unit and keep damaged items out of saleable stock." },
        { name: "Handle loose and repacked material", text: "Treat loose material and each pack size as separate products and move stock between them with a recorded step." },
        { name: "Count on a schedule", text: "Compare counted stock with the records and record each difference with a reason." },
        { name: "Value stock in FCFA", text: "Use a consistent cost method such as weighted average and keep the cost of each sale frozen." },
      ],
    },
    related: ["industries/construction-materials", "features", "resources/buy-and-receive-stock-from-suppliers"],
  },
  {
    slug: "manage-customer-credit-hardware-shop",
    icon: "wallet",
    keywords: ["customer credit", "receivables", "deposits", "hardware shop", "FCFA", "collections"],
    metaTitle: "Managing customer credit in a hardware shop",
    description: "A practical guide to selling on credit in a hardware shop: who to extend credit to, how to record it, how to take deposits and collect payments without disputes.",
    h1: "Managing customer credit in a hardware shop",
    answer: "To manage customer credit safely, extend it only to customers you know, set a limit for each, record every credit sale as a receivable linked to the sale, take deposits for large orders, apply every payment to specific debts, and review who owes what every week. Credit is a loan from your own cash, so it needs the same care as cash.",
    sections: [
      {
        h2: "Decide who gets credit",
        paras: [
          "Credit works best for regular customers whose business you know: contractors with ongoing work, institutions, and long-standing neighbours. Be slower to extend it to one-time buyers of large orders.",
          "Write a simple rule that staff can follow without calling you each time: who may be offered credit, up to what amount, and who can approve an exception.",
        ],
      },
      {
        h2: "Set a limit and a time frame",
        paras: [
          "Give each credit customer a limit that reflects what you could afford to lose, not what they would like to buy. Agree on when payment is expected. A limit with no time frame is only half a policy.",
          "Count everything the customer owes you across all branches and projects when you check the limit, not only the latest sale.",
        ],
      },
      {
        h2: "Record every credit sale as a receivable",
        paras: [
          "A credit sale should reduce your stock and create an amount owed by the customer, linked to that sale and its numbered invoice. Avoid a notebook of loose amounts: when a payment arrives you need to know which sale it settles.",
          "Because credit is not cash, it should not appear in the till. Your cash count and your receivables are two different things, and mixing them hides problems in both.",
        ],
      },
      {
        h2: "Use deposits for large orders",
        paras: [
          "For a big order, ask for a deposit before you order or reserve material. A deposit is not a sale yet. It is money you hold for the customer and owe back in goods or in cash. Record it as such, and allocate it to the sale when the goods are handed over, so you do not record the same money as income twice.",
          "If the customer cancels, decide in advance what happens to the deposit, and keep any refund approved and recorded, with the remaining balance checked.",
        ],
      },
      {
        h2: "Apply every payment to specific debts",
        paras: [
          "When a customer pays, record the amount, the date, how they paid and which sale or sales it settles. A payment larger than the debt is a warning sign: it may be a mistake or an advance, and it should be treated explicitly rather than hidden.",
        ],
        list: [
          "Give a receipt for every payment.",
          "Never record the same payment twice.",
          "For mobile money, check the provider's confirmation before you call the payment collected.",
        ],
      },
      {
        h2: "Review debts every week",
        paras: [
          "Once a week, list the customers who owe you, how much, and since when. Call the oldest debts first, and stop new credit to a customer who is behind until they pay. Do this calmly and in writing where you can: a statement of what is owed avoids most disputes.",
        ],
      },
      {
        h2: "Handle returns and disputes cleanly",
        paras: [
          "If goods come back, link the return to the original sale and give a credit note, so the customer's balance falls by the correct amount and stock returns only if the goods are in sellable condition. Do not erase the original sale. Corrections should be new entries that point to the old one.",
        ],
      },
      {
        h2: "How QuincaiSoft supports this",
        paras: [
          "QuincaiSoft records credit sales as receivables, applies cash receipts to recognised debt and rejects over-payments, holds deposits as customer liabilities and allocates them to sales, and supports approved deposit refunds. Credit limits, ageing reports and customer statements are planned and are not available yet.",
        ],
      },
    ],
    howTo: {
      name: "How to manage customer credit in a hardware shop",
      steps: [
        { name: "Decide who gets credit", text: "Write a simple rule for who may be offered credit, up to what amount, and who approves exceptions." },
        { name: "Set a limit and a time frame", text: "Give each credit customer a limit and an agreed payment date, counting everything they already owe." },
        { name: "Record each credit sale as a receivable", text: "Link the amount owed to the sale and its numbered invoice, and keep it out of the till." },
        { name: "Take deposits for large orders", text: "Hold the deposit as money owed to the customer and allocate it to the sale on handover." },
        { name: "Apply every payment to specific debts", text: "Record amount, date, method and the sale settled, and give a receipt." },
        { name: "Review debts weekly", text: "List who owes what and since when, and stop new credit to customers who are behind." },
      ],
    },
    related: ["industries/hardware-shops", "features", "payments", "resources/mobile-money-reconciliation-basics"],
  },
  {
    slug: "buy-and-receive-stock-from-suppliers",
    icon: "truck",
    keywords: ["purchasing", "goods receipt", "supplier invoice", "purchase order", "stock", "wholesale"],
    metaTitle: "Buying and receiving stock from suppliers",
    description: "A practical guide to purchasing for a shop or wholesaler: order with approval, receive against the delivery, match the supplier invoice and handle returns.",
    h1: "Buying and receiving stock from suppliers",
    answer: "To buy and receive stock without losses, raise an order with a named supplier, quantities and agreed costs, get it approved by someone other than the buyer, count every delivery against the order when it arrives, accept the stock only for what was actually received, match the supplier's invoice to the receipt, and pay what is owed on time. Ordering does not add stock. Receiving does.",
    sections: [
      {
        h2: "Separate ordering, receiving and invoicing",
        paras: [
          "Three different events happen when you buy: you order, goods arrive, and the supplier invoices. Treat them as separate records. Stock should increase when goods are received, not when you place the order, and a debt to the supplier should appear when you accept their invoice.",
          "Keeping these apart lets you spot the common problems: goods ordered but never delivered, goods delivered but not invoiced, and invoices for goods you never received.",
        ],
      },
      {
        h2: "Step 1: raise a clear order",
        paras: [
          "An order should name the supplier, each product and quantity, the agreed unit cost and the expected delivery date. Keep the quantities in the same base units you use in stock, so no conversion is needed when the goods arrive.",
          "Have someone other than the person who placed the order approve it, at least above an amount you set. This one habit prevents many errors and most internal fraud.",
        ],
      },
      {
        h2: "Step 2: receive against the order",
        paras: [
          "When the delivery arrives, count it and compare it with both the delivery note and the order. Record the quantity you actually received. Deliveries are often partial, so allow several receipts against one order and keep a running balance of what is still expected.",
        ],
        list: [
          "Count in the base unit, not in the supplier's unit, if they differ.",
          "Check condition and set aside damaged goods.",
          "Do not accept more than was ordered without an approved change.",
        ],
      },
      {
        h2: "Step 3: match the supplier invoice",
        paras: [
          "When the invoice arrives, match it to a specific receipt. Check the quantities and the unit costs against what you ordered and received. Differences should be resolved with the supplier before you record the debt.",
          "Record the supplier's invoice number once. A duplicate invoice is a common way to be paid twice, so each supplier reference should be unique.",
        ],
      },
      {
        h2: "Step 4: pay and track what you owe",
        paras: [
          "Keep a list of what you owe each supplier and when it is due. Pay against specific invoices so you always know which are settled. Late payments can cost you terms or priority, and early knowledge of your debts helps you plan cash.",
        ],
      },
      {
        h2: "Step 5: return what is wrong",
        paras: [
          "If goods are faulty or wrong, return them with a record that links to the original receipt. You cannot return more than you received. Reduce your stock by the quantity returned, and agree with the supplier whether you receive a credit or a replacement.",
        ],
      },
      {
        h2: "Know your cost",
        paras: [
          "Each receipt tells you what the goods cost. Use a consistent method to value stock, such as weighted average cost, so that the cost of a later sale reflects what you really paid. If transport or customs add to the cost, decide how to include them and apply the rule every time.",
        ],
      },
      {
        h2: "How QuincaiSoft supports this",
        paras: [
          "QuincaiSoft supports supplier records, purchase drafts, independent approval, partial goods receipts that cannot exceed the order, supplier invoices matched to receipts with unique references, and returns of uninvoiced goods. Supplier payment allocation, ageing, landed costs and requisitions are planned and are not available yet.",
        ],
      },
    ],
    howTo: {
      name: "How to buy and receive stock from suppliers",
      steps: [
        { name: "Raise a clear order", text: "Name the supplier, products, quantities, agreed costs and expected date, and have someone else approve it." },
        { name: "Receive against the order", text: "Count each delivery against the delivery note and the order, record the actual quantity and allow partial receipts." },
        { name: "Match the supplier invoice", text: "Match the invoice to a specific receipt and check quantities and costs before recording the debt." },
        { name: "Pay and track what you owe", text: "Pay against specific invoices and keep a list of what is due." },
        { name: "Return what is wrong", text: "Link returns to the original receipt and never return more than was received." },
      ],
    },
    related: ["industries/wholesale-distribution", "features", "resources/run-multiple-branches"],
  },
];
