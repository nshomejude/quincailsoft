'use client';

import Link from 'next/link';
import {usePathname} from 'next/navigation';
import {useEffect, useState} from 'react';
import type {LucideIcon} from 'lucide-react';
import {
  Banknote,
  BarChart3,
  Building2,
  CalendarClock,
  ClipboardList,
  FileText,
  GitBranch,
  LayoutDashboard,
  Menu,
  Package,
  Receipt,
  ScrollText,
  Settings,
  ShieldCheck,
  ShoppingCart,
  Smartphone,
  Store,
  Truck,
  Undo2,
  Users,
  UserRound,
  Wallet,
  Warehouse,
  X,
} from 'lucide-react';

type Item = {
  href: string;
  label: string;
  permission: string;
  icon: LucideIcon;
};

type Section = {
  label: string | null;
  items: Item[];
};

// An empty permission means the item is visible to every signed-in user.
const sections: Section[] = [
  {
    label: null,
    items: [{href: '/', label: 'overview', permission: '', icon: LayoutDashboard}],
  },
  {
    label: 'navSell',
    items: [
      {href: '/sales', label: 'sales', permission: 'sales:read', icon: ShoppingCart},
      {href: '/quotations', label: 'quotations', permission: 'quotations:read', icon: ClipboardList},
      {href: '/returns', label: 'returns', permission: 'returns:read', icon: Undo2},
      {href: '/customers', label: 'customers', permission: 'customers:read', icon: UserRound},
      {href: '/commerce', label: 'commerce', permission: 'commerce:manage', icon: Store},
    ],
  },
  {
    label: 'navBuy',
    items: [
      {href: '/purchases', label: 'purchasing', permission: 'purchasing:read', icon: Truck},
      {href: '/suppliers', label: 'suppliers', permission: 'purchasing:read', icon: Users},
    ],
  },
  {
    label: 'navStock',
    items: [
      {href: '/catalog', label: 'catalog', permission: 'catalog:read', icon: Package},
      {href: '/inventory', label: 'stock', permission: 'inventory:read', icon: Warehouse},
    ],
  },
  {
    label: 'navMoney',
    items: [
      {href: '/finance', label: 'finance', permission: 'tenders:read', icon: Wallet},
      {href: '/finance/expenses', label: 'expenses', permission: 'expenses:read', icon: Receipt},
      {href: '/finance/periods', label: 'periods', permission: 'finance:read', icon: CalendarClock},
      {href: '/finance/cash', label: 'cashSessions', permission: 'cash:read', icon: Banknote},
      {href: '/documents', label: 'documents', permission: 'documents:read', icon: FileText},
      {href: '/reports', label: 'reports', permission: 'reports:read', icon: BarChart3},
    ],
  },
  {
    label: 'navAdmin',
    items: [
      {href: '/identity', label: 'identity', permission: 'identity:manage', icon: ShieldCheck},
      {href: '/audit', label: 'audit', permission: 'audit:read', icon: ScrollText},
      {href: '/companies', label: 'companies', permission: '', icon: Building2},
      {href: '/branches', label: 'branches', permission: '', icon: GitBranch},
      {href: '/sync', label: 'devices', permission: 'sync:manage', icon: Smartphone},
      {href: '/documents/settings', label: 'settings', permission: 'documents:configure', icon: Settings},
    ],
  },
];

// The /finance hub is also reachable by cash users who cannot read tenders.
function visible(item: Item, authorities: string[]) {
  const has = (permission: string) =>
    authorities.includes(permission) || authorities.includes('SCOPE_' + permission);
  if (!item.permission) return true;
  if (item.href === '/finance') return has('tenders:read') || has('cash:read');
  return has(item.permission);
}

// Exactly one item is current: the one with the longest matching path prefix.
function currentHref(path: string, hrefs: string[]) {
  const matches = hrefs.filter(href => href === '/' ? path === '/' : path === href || path.startsWith(href + '/'));
  return matches.sort((a, b) => b.length - a.length)[0];
}

export function Navigation({authorities, labels}: {authorities: string[]; labels: Record<string, string>}) {
  const path = usePathname();
  const [open, setOpen] = useState(false);

  // Collapse the mobile menu after every navigation.
  useEffect(() => {
    setOpen(false);
  }, [path]);

  const shown = sections
    .map(section => ({...section, items: section.items.filter(item => visible(item, authorities))}))
    .filter(section => section.items.length > 0);
  const current = currentHref(path, shown.flatMap(section => section.items.map(item => item.href)));

  return (
    <>
      <button
        type="button"
        className="nav-toggle"
        aria-expanded={open}
        aria-controls="primary-nav"
        onClick={() => setOpen(value => !value)}
      >
        {open ? <X size={18} aria-hidden /> : <Menu size={18} aria-hidden />}
        {open ? labels.menuClose ?? 'Close menu' : labels.menu ?? 'Menu'}
      </button>
      <nav id="primary-nav" aria-label={labels.mainNav ?? 'Main navigation'} data-open={open}>
        {shown.map(section => (
          <div className="nav-section" key={section.label ?? 'top'}>
            {section.label && <p className="nav-label">{labels[section.label] ?? section.label}</p>}
            {section.items.map(item => {
              const Icon = item.icon;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  aria-current={current === item.href ? 'page' : undefined}
                >
                  <Icon size={18} aria-hidden />
                  {labels[item.label] ?? item.label}
                </Link>
              );
            })}
          </div>
        ))}
      </nav>
    </>
  );
}
