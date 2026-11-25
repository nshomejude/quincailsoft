import type { Metadata } from "next";
import { productMetadata, renderProduct } from "@/components/product/ProductRoute";
import type { LangParams } from "@/lib/route";

export function generateMetadata({ params }: LangParams): Promise<Metadata> {
  return productMetadata(params, "mobile-app");
}

export default function Page({ params }: LangParams) {
  return renderProduct(params, "mobile-app");
}
