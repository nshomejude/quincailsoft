import {
  Banknote, BarChart3, Boxes, Building2, ClipboardList, FileText, Hammer, HandCoins, HardHat, History, KeyRound, Languages,
  Layers, Lock, Package, ReceiptText, Repeat, ScanBarcode, Scale, ShieldCheck, ShoppingCart, Smartphone, Store, Truck, Users,
  Wallet, Warehouse, WifiOff, ClipboardCheck,
  type LucideIcon,
} from "lucide-react";
import type { IconName } from "@/lib/home-content";

const ICONS: Record<IconName, LucideIcon> = {
  Package, ScanBarcode, Warehouse, Store, Truck, FileText, ReceiptText, Wallet, Smartphone, WifiOff, Users, BarChart3, ShieldCheck,
  Languages, Banknote, Building2, ClipboardList, HandCoins, ShoppingCart, Scale, ClipboardCheck, Boxes, History, Lock, Repeat,
  KeyRound, Hammer, HardHat, Layers,
};

/** Plain outline icon in a flat bordered square. Decorative: the adjacent text carries the meaning. */
export function HomeIcon({ name, size = 24 }: { name: IconName; size?: number }) {
  const Icon = ICONS[name];
  return (
    <span className="home-icon" aria-hidden="true">
      <Icon size={size} strokeWidth={1.75} color="currentColor" focusable="false" />
    </span>
  );
}
