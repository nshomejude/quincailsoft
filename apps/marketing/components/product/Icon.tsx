import {
  ArrowRightLeft, Banknote, BarChart3, BookOpen, Boxes, Building2, Calculator, CircleCheck, ClipboardCheck, Cloud, CreditCard,
  Database, FileCheck, FileSpreadsheet, FileText, Fingerprint, GraduationCap, Globe, Hammer, Handshake, HardHat, KeyRound, Landmark, Layers,
  ListChecks, Lock, Monitor, Package, Receipt, Route, Ruler, Scale, Scissors, ScrollText, Search, Server, ShieldCheck, ShoppingCart, Smartphone,
  Store, Truck, TriangleAlert, Undo2, Users, Wallet, Warehouse, WifiOff,
  type LucideIcon,
} from "lucide-react";
import type { IconName } from "@/lib/product-types";

const MAP: Record<IconName, LucideIcon> = {
  cart: ShoppingCart, boxes: Boxes, truck: Truck, users: Users, file: FileText, undo: Undo2, wallet: Wallet, filecheck: FileCheck,
  chart: BarChart3, building: Building2, shield: ShieldCheck, scroll: ScrollText, phone: Smartphone, card: CreditCard, store: Store,
  lock: Lock, monitor: Monitor, transfer: ArrowRightLeft, book: BookOpen, scale: Scale, scissors: Scissors, package: Package,
  banknote: Banknote, key: KeyRound, database: Database, fingerprint: Fingerprint, layers: Layers, clipboard: ClipboardCheck,
  receipt: Receipt, globe: Globe, route: Route, list: ListChecks, walloff: WifiOff, hardhat: HardHat, warehouse: Warehouse,
  hammer: Hammer, landmark: Landmark, sheet: FileSpreadsheet, cloud: Cloud, server: Server, search: Search, graduation: GraduationCap,
  calculator: Calculator, ruler: Ruler, handshake: Handshake, alert: TriangleAlert, check: CircleCheck,
};

export function Icon({ name, size = 20 }: { name: IconName; size?: number }) {
  const C = MAP[name];
  return <C size={size} strokeWidth={1.75} color="currentColor" fill="none" aria-hidden="true" focusable="false" />;
}
