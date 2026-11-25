export type Status = "available" | "partial" | "planned" | "pending";

export type IconName =
  | "cart" | "boxes" | "truck" | "users" | "file" | "undo" | "wallet" | "filecheck" | "chart" | "building"
  | "shield" | "scroll" | "phone" | "card" | "store" | "lock" | "monitor" | "transfer" | "book" | "scale"
  | "scissors" | "package" | "banknote" | "key" | "database" | "fingerprint" | "layers" | "clipboard"
  | "receipt" | "globe" | "route" | "list" | "walloff" | "hardhat" | "warehouse" | "hammer" | "landmark"
  | "sheet" | "cloud" | "server" | "search" | "graduation" | "calculator" | "ruler" | "handshake" | "alert" | "check";

export type Item = { icon?: IconName; title: string; body: string; status?: Status; points?: string[] };
export type Faq = { q: string; a: string };

export type Block =
  | { kind: "cards"; id?: string; title: string; intro?: string; items: Item[]; alt?: boolean }
  | { kind: "steps"; id?: string; title: string; intro?: string; items: { title: string; body: string }[]; alt?: boolean }
  | { kind: "table"; id?: string; title: string; intro?: string; caption: string; head: string[]; rows: string[][]; alt?: boolean }
  | { kind: "limits"; id?: string; title: string; items: string[]; alt?: boolean }
  | { kind: "text"; id?: string; title: string; paras: string[]; alt?: boolean }
  | { kind: "faq"; id?: string; title: string; items: Faq[]; alt?: boolean };

export type Glance = { title: string; items: { label: string; note: string; status?: Status }[] };

export type ProductPageData = {
  icon: IconName;
  kicker: string;
  metaTitle: string;
  description: string;
  h1: string;
  /** Answer-first paragraph shown directly under the H1. */
  answer: string;
  glance?: Glance;
  blocks: Block[];
  /** Language-less paths of related pages. */
  related: string[];
  ctaTitle: string;
  ctaBody: string;
};

export type GuideSection = { h2: string; paras?: string[]; list?: string[]; ordered?: boolean };
export type GuideData = {
  slug: string;
  icon: IconName;
  metaTitle: string;
  description: string;
  h1: string;
  answer: string;
  sections: GuideSection[];
  howTo?: { name: string; steps: { name: string; text: string }[] };
  faq?: Faq[];
  related: string[];
  keywords: string[];
};

/** Strings shared by every product page. */
export type ProductUi = {
  status: Record<Status, string>;
  onThisPage: string;
  related: string;
  contact: string;
  seeFeatures: string;
  author: string;
  published: string;
  readGuide: string;
  limitsTitle: string;
  moduleWhat: string;
  moduleHow: string;
  guidesTitle: string;
  relatedGuides: string;
  resources: string;
  minutes: string;
};

export type FeaturesData = {
  metaTitle: string;
  description: string;
  kicker: string;
  h1: string;
  answer: string;
  statusLegendTitle: string;
  statusLegend: { status: Status; body: string }[];
  summaryTitle: string;
  summaryCaption: string;
  summaryHead: string[];
  modules: ModuleData[];
  moreTitle: string;
  more: Item[];
  limits: string[];
  related: string[];
  ctaTitle: string;
  ctaBody: string;
};

export type FaqGroup = { id: string; title: string; items: Faq[] };
export type FaqData = {
  metaTitle: string;
  description: string;
  kicker: string;
  h1: string;
  answer: string;
  groups: FaqGroup[];
  related: string[];
  ctaTitle: string;
  ctaBody: string;
};

export type PricingData = ProductPageData & { formTitle: string };

export type IndustriesData = {
  index: ProductPageData & { sectorsTitle: string; sectorLink: string; sectors: Record<string, { summary: string }> };
  items: Record<string, ProductPageData>;
};

export type ResourcesHub = { metaTitle: string; description: string; kicker: string; h1: string; answer: string; listTitle: string; related: string[] };

export type Dataset = {
  ui: ProductUi;
  products: Record<string, ProductPageData>;
  resourcesHub: ResourcesHub;
  titles: Record<string, string>;
  features: FeaturesData;
  industries: IndustriesData;
  pricing: PricingData;
  faq: FaqData;
  guides: GuideData[];
};

/** One module on the features page. */
export type ModuleData = {
  id: string;
  icon: IconName;
  title: string;
  what: string;
  how: string[];
  status: Status;
  statusNote: string;
};
