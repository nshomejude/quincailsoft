import type { GuideSection } from "../product-types";

/** Extra sections inserted before the closing "How QuincaiSoft supports this" section of each guide. */
export const extras: Record<string, GuideSection[]> = {
  "track-cement-and-bulk-materials-fcfa": [
    {
      h2: "Common mistakes to avoid",
      paras: ["A few habits cause most of the trouble with bulk materials. Watch for them in your own shop."],
      list: [
        "Recording a receipt in tonnes and a sale in bags without a written conversion.",
        "Rounding a fraction of a bag instead of finding out where it came from.",
        "Correcting a count by quietly editing a quantity, with no reason recorded.",
        "Letting several people use different names or units for the same product.",
        "Mixing damaged bags back into saleable stock.",
      ],
    },
    {
      h2: "A simple weekly routine",
      paras: [
        "Once a week, count your five fastest-moving bulk products and compare them with the records. Note the differences and their likely causes. Once a month, count everything and review the products that regularly show differences. Over a few months you will see which products, which times of day and which people are linked to losses, and you can act on facts instead of suspicion.",
      ],
    },
  ],
  "manage-customer-credit-hardware-shop": [
    {
      h2: "Common mistakes to avoid",
      paras: ["Credit problems usually start small and grow quietly."],
      list: [
        "Giving credit because a customer is insistent rather than because they are reliable.",
        "Keeping debts in a notebook that only one person understands.",
        "Letting the same customer keep buying while an old debt is unpaid.",
        "Recording a payment without saying which sale it settles.",
        "Treating a deposit as income before the goods are delivered.",
      ],
    },
    {
      h2: "Talking about money with customers",
      paras: [
        "Be clear and polite. State the amount owed, the sale it relates to and the date it was due, and offer to send a written statement. Customers who feel respected usually pay sooner. If a customer cannot pay, agree a realistic plan in writing and stop new credit until it is on track. A calm, documented approach protects both the relationship and your cash.",
      ],
    },
  ],
  "buy-and-receive-stock-from-suppliers": [
    {
      h2: "Common mistakes to avoid",
      paras: ["Most purchasing losses are simple and avoidable."],
      list: [
        "Adding stock when the order is placed instead of when goods arrive.",
        "Signing a delivery note without counting.",
        "Paying an invoice twice because the reference was not recorded once.",
        "Letting the same person order, receive and approve payment.",
        "Forgetting to record a supplier return, so the stock stays on the books.",
      ],
    },
    {
      h2: "Choosing and reviewing suppliers",
      paras: [
        "Keep a record of each supplier's delivery reliability, accuracy and price over time. Compare quotes for your main products at least occasionally, but weigh price against dependability and payment terms. A slightly higher price from a supplier who delivers what was ordered, on time, is often cheaper than a low price with shortages and delays. Review your main suppliers every few months using the records you have already kept.",
      ],
    },
  ],
  "quotations-for-contractors": [
    {
      h2: "Common mistakes to avoid",
      paras: ["Quotation disputes tend to follow the same patterns."],
      list: [
        "Leaving out the unit, so a price per bag is read as a price per tonne.",
        "Not stating a validity period, then being asked to honour an old price.",
        "Editing a quotation after it was sent instead of issuing a revision.",
        "Forgetting to mention that delivery or cutting is extra.",
        "Retyping an accepted quotation into a sale and changing a quantity by accident.",
      ],
    },
    {
      h2: "Building a relationship with contractors",
      paras: [
        "Contractors return to suppliers who are quick, accurate and clear. Reply to quotation requests promptly, explain differences between a quotation and the final invoice, and keep a record of each contractor's past projects so that repeat orders are easy to prepare. A reliable quotation process is one of the best ways to earn trade customers.",
      ],
    },
  ],
  "run-multiple-branches": [
    {
      h2: "Common mistakes to avoid",
      paras: ["Growth exposes habits that were harmless in a single shop."],
      list: [
        "Moving goods between branches on trust, with nothing recorded.",
        "Letting one shared login be used by everyone.",
        "Using different product names or units in different branches.",
        "Treating a branch's cash difference as normal.",
        "Checking only the total and never the individual branches.",
      ],
    },
    {
      h2: "Before you open the next branch",
      paras: [
        "Make sure the first branch runs on routines, not on the owner's memory. Check that stock counts, cash closes and approvals are working there. Decide who manages the new branch, what they can approve and which numbers you will review each week. A branch is easier to supervise when the rules are written down before it opens.",
      ],
    },
  ],
  "mobile-money-reconciliation-basics": [
    {
      h2: "Common mistakes to avoid",
      paras: ["Mobile money errors are often small, but they add up."],
      list: [
        "Accepting a screenshot as proof of payment.",
        "Recording a payment as collected before the provider confirms it.",
        "Asking the customer to pay again without checking the first request.",
        "Ignoring small differences because they seem too minor to matter.",
        "Mixing mobile money with cash in the same total.",
      ],
    },
    {
      h2: "What to do when numbers do not match",
      paras: [
        "Do not change your totals to make them agree. Write down each difference with its reference, the date and what you found. If it remains unexplained, ask the provider with the reference in hand. Keep the correction as a separate, dated entry that points to the original, so anyone reviewing your books later can follow what happened. Regular, calm checks make large surprises rare.",
      ],
    },
  ],
};

/** Further sections, placed after the extras above. */
export const more: Record<string, GuideSection[]> = {
  "run-multiple-branches": [
    {
      h2: "A weekly review you can actually keep",
      paras: [
        "Choose one fixed time each week, even thirty minutes, to look at the same page of numbers for every branch. Ask the same three questions each time: what changed since last week, what looks different from the other branches, and what needs a decision. Write down the decisions and who owns them. A short, regular review beats a long, occasional one, because problems are caught while the details are still fresh and the people involved can still explain them.",
      ],
    },
  ],
  "mobile-money-reconciliation-basics": [
    {
      h2: "Who should do the reconciliation",
      paras: [
        "Ideally, the person who reconciles mobile money is not the person who takes the payments at the counter. Separating the two makes errors easier to spot and removes the temptation to hide a difference. In a small shop where this is not possible, the owner or a manager can review the reconciliation and sign it off, even if someone else prepared it. Whoever does it, write down the date, the period covered and the differences found.",
      ],
    },
  ],
  "buy-and-receive-stock-from-suppliers": [
    {
      h2: "Keep the paperwork together",
      paras: [
        "For each purchase, keep the order, the delivery note, the receipt record and the supplier invoice linked, so that anyone can follow a delivery from request to payment. When a supplier disputes a quantity or a price, this trail settles the question quickly and calmly. It also makes your annual review of suppliers much easier, because the facts are already in one place.",
      ],
    },
  ],
  "quotations-for-contractors": [
    {
      h2: "Keep your quotation history",
      paras: [
        "Keep every quotation, including the ones that were not accepted. A history of what you quoted, when and at what price lets you answer a returning contractor quickly, check that new prices are consistent with old ones, and understand how often your quotations turn into sales. It also protects you if a customer later claims that a different price was promised.",
      ],
    },
  ],
};
