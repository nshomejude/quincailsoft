 'use client';import {useState} from 'react';import Link from 'next/link';import {usePathname} from 'next/navigation';import {LayoutDashboard,Package,ShoppingCart,Truck,Wallet,FileText,Warehouse,Users,Settings} from 'lucide-react';
const items=[['/','overview','',LayoutDashboard],['/catalog','catalog','catalog:read',Package],['/inventory','stock','inventory:read',Warehouse],['/sales','sales','sales:read',ShoppingCart],['/quotations','quotations','quotations:read',FileText],['/returns','returns','returns:read',ShoppingCart],['/purchases','purchasing','purchasing:read',Truck],['/customers','customers','customers:read',Users],['/suppliers','suppliers','purchasing:read',Users],['/finance','finance','tenders:read',Wallet],['/finance/bank','bank','bank:read',Wallet],['/finance/payables','payables','purchasing:read',Wallet],['/finance/bank-collections','settlements','tenders:read',Wallet],['/finance/journals','journals','finance:read',Wallet],['/finance/expenses','expenses','expenses:read',Wallet],['/finance/periods','periods','finance:read',Wallet],['/documents','documents','documents:read',FileText],['/reports','reports','reports:read',FileText],['/workforce','workforce','workforce:employees:read',Users],['/identity','identity','identity:manage',Users],['/audit','audit','audit:read',FileText],['/documents/settings','settings','documents:configure',Settings]] as const;

const groups = [
    ['Workspace', ['/', '/sales', '/quotations', '/customers', '/returns']],
    ['Operations', ['/catalog', '/inventory', '/purchases', '/suppliers']],
    ['Management', ['/finance', '/finance/bank', '/finance/payables', '/finance/bank-collections', '/finance/journals', '/finance/expenses', '/finance/periods', '/reports', '/documents', '/workforce', '/identity', '/audit', '/documents/settings']],
] as const;

export function Navigation({authorities,labels}:{authorities:string[];labels:Record<string,string>}) {
    const path = usePathname();
    const [query, setQuery] = useState('');
    const [menuOpen, setMenuOpen] = useState(false);
    const allowed = (permission:string) => authorities.includes(permission) || authorities.includes('SCOPE_' + permission);
    const available = items.filter(([href,,permission])=>!permission || allowed(permission) || (href==='/finance' && allowed('cash:read')) || (href==='/workforce' && allowed('workforce:payroll:read')));
    return <>
        <button type="button" className="workspace-menu-toggle secondary" aria-controls="workspace-navigation" aria-expanded={menuOpen} onClick={()=>setMenuOpen(open=>!open)}>Workspaces</button>
        <div className="workspace-navigation" id="workspace-navigation" data-open={menuOpen}>
        <div className="workspace-nav-search"><label htmlFor="workspace-search">Find a workspace</label><input id="workspace-search" type="search" value={query} onChange={event=>setQuery(event.target.value)} placeholder="Search available screens"/></div>
        <nav aria-label="Workspaces">{groups.map(([group, order])=>{
            const links = order.flatMap(href=>available.filter(item=>item[0]===href)).filter(([,label])=>(labels[label]??label).toLowerCase().includes(query.trim().toLowerCase()));
            return links.length>0 && <div key={group}><h3>{group}</h3>{links.map(([href,label,,Icon])=><Link key={href} href={href} onClick={()=>setMenuOpen(false)} aria-current={path===href?'page':undefined}><Icon size={16} aria-hidden/>{labels[label]??label}</Link>)}</div>;
        })}</nav>
        </div>
    </>;
}
