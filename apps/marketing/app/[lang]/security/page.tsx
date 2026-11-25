import type { Metadata } from "next";
import { productMetadata, renderProduct } from "@/components/product/ProductRoute";
import type { LangParams } from "@/lib/route";

export function generateMetadata({ params }: LangParams): Promise<Metadata> {
  return productMetadata(params, "security");
}

export default function Page({ params }: LangParams) {
  return renderProduct(params, "security");
}
