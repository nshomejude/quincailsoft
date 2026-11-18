import Link from 'next/link';
import {session} from '@/lib/auth';
import {api, Me, Product, Sale, permitted} from '@/lib/api';
import {Access} from '@/components/Access';

export const dynamic = 'force-dynamic';

export default async function Dashboard() {
    try {
        const auth = await session();
        if (!auth) return <section className="panel"><h1>Welcome to QuincaiSoft</h1><p>Sign in to access your authorised company and branch.</p><a className="button" href="/auth/login">Sign in securely</a></section>;
        const me = await api<Me>('me');
        const [sales, products, purchases, customers] = await Promise.all([
            permitted(me, 'sales:read') ? api<Sale[]>('sales') : null,
            permitted(me, 'catalog:read') ? api<Product[]>('products') : null,
            permitted(me, 'purchasing:read') ? api<{id: string; status: string}[]>('purchases') : null,
            permitted(me, 'customers:read') ? api<{items: {id: string; name: string; active: boolean}[]; total: number}>('customers/page?limit=5') : null,
        ]);
        const activity = [
            ...(sales ?? []).slice(0, 3).map(row => ({id:row.id, label:row.id.slice(0,8), workspace:'Sales', status:row.status, href:'/sales/'+row.id})),
            ...(products ?? []).slice(0, 3).map(row => ({id:row.id, label:row.name, workspace:'Products', status:row.active?'Active':'Inactive', href:'/catalog/'+row.id})),
            ...(purchases ?? []).slice(0, 2).map(row => ({id:row.id, label:row.id.slice(0,8), workspace:'Purchases', status:row.status, href:'/purchases/'+row.id})),
            ...(customers?.items ?? []).slice(0, 2).map(row => ({id:row.id, label:row.name, workspace:'Customers', status:row.active?'Active':'Inactive', href:'/customers/'+row.id})),
        ];
        const metrics = [['Sales',sales?.length],['Products',products?.length],['Purchases',purchases?.length],['Customers',customers?.total]] as const;
        return <>
            <p className="eyebrow">WORKSPACE / OVERVIEW</p>
            <div className="page-top"><div><h1>Your business, in focus.</h1><p className="lead">Company and branch workspace</p></div><span className="session-pill">● Session active</span></div>
            <div className="quick dashboard-actions">
                {permitted(me,'sales:write') && <Link className="button" href="/sales/new">＋ New sale</Link>}
                {permitted(me,'catalog:write') && <Link className="secondary" href="/catalog/new">＋ New product</Link>}
                {permitted(me,'purchasing:write') && <Link className="secondary" href="/purchases/new">＋ Purchase order</Link>}
                {permitted(me,'tenders:read') && <Link className="secondary" href="/finance">Cash &amp; payments</Link>}
                {permitted(me,'reports:read') && <Link className="secondary" href="/reports">Reports</Link>}
                {permitted(me,'documents:read') && <Link className="secondary" href="/documents">Documents</Link>}
            </div>
            <section className="stats dashboard-stats" aria-label="Workspace records">{metrics.filter(([,count])=>count!==undefined).map(([label,count])=><article key={label}><span>{label}</span><strong>{count}</strong><p className="muted">Authorized records in view</p></article>)}</section>
            <div className="dashboard-split">
                <section className="panel"><div className="page-top"><h2>Workspace activity</h2><span className="muted">Live integration data</span></div>
                    {activity.length ? <div className="table-scroll"><table><thead><tr><th>Record</th><th>Workspace</th><th>Status</th><th>Open</th></tr></thead><tbody>{activity.map(row=><tr key={row.href}><td>{row.label}</td><td>{row.workspace}</td><td><span className="badge">{row.status}</span></td><td><Link href={row.href}>View record →</Link></td></tr>)}</tbody></table></div> : <p className="muted">No records in this branch.</p>}
                </section>
                <section className="panel dashboard-access"><p className="eyebrow">YOUR ACCESS</p><h2>{auth.name}</h2><dl><dt>Company</dt><dd>{me.scope.companyId}</dd><dt>Branch</dt><dd>{me.scope.branchId}</dd><dt>Report export</dt><dd>{permitted(me,'reports:export')?'Enabled':'Unavailable'}</dd></dl></section>
            </div>
        </>;
    } catch (error) {
        return <Access error={error}/>;
    }
}
