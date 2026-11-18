import Link from 'next/link';
import {session} from '@/lib/auth';
import {api, ApiError, Me, Sale, permitted, xaf} from '@/lib/api';
import {Access} from '@/components/Access';

export const dynamic = 'force-dynamic';

function dateInDouala() {
    const parts = new Intl.DateTimeFormat('en-CA', {
        timeZone: 'Africa/Douala', year: 'numeric', month: '2-digit', day: '2-digit'
    }).formatToParts();
    return ['year', 'month', 'day'].map(type => parts.find(part => part.type === type)?.value).join('-');
}

export default async function Dashboard({searchParams}: {searchParams: Promise<{date?: string}>}) {
    try {
        const auth = await session();
        if (!auth) return <section className="panel"><h1>Welcome to QuincaiSoft</h1><p>Sign in to access your authorised company and branch.</p><a className="button" href="/auth/login">Sign in securely</a></section>;

        const [me, query] = await Promise.all([api<Me>('me'), searchParams]);
        const today = dateInDouala();
        const selectedDate = query.date && /^\d{4}-\d{2}-\d{2}$/.test(query.date) ? query.date : today;
        const mayReadSales = permitted(me,'sales:read');
        const [summary, sales] = await Promise.all([
            mayReadSales ? api<{totalXaf: string}>('sales/summary?'+new URLSearchParams({from:selectedDate,to:selectedDate})).catch((error:unknown) => {
                if (error instanceof ApiError && (error.status === 404 || error.status === 400)) return null;
                throw error;
            }) : null,
            mayReadSales ? api<Sale[]>('sales') : null,
        ]);
        const metrics = [
            {label: selectedDate === today ? 'Sales today' : 'Sales on selected date', value: summary ? xaf(summary.totalXaf) : null, note: mayReadSales ? 'Posted gross sales · before returns' : 'Sales access required'},
            {label:'Orders to prepare', value:null, note:'Order queue connection pending'},
            {label:'Low-stock products', value:null, note:'Reorder aggregate connection pending'},
            {label:'Collection pending', value:null, note:'Collection queue connection pending'},
        ];

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
            {mayReadSales && <form className="dashboard-date"><label htmlFor="overview-date">Business date</label><input id="overview-date" name="date" type="date" defaultValue={selectedDate} required/><button className="secondary">Apply</button></form>}
            <section className="stats dashboard-stats" aria-label="Workspace measures">{metrics.map(metric=><article key={metric.label}><span>{metric.label}</span><strong>{metric.value??'—'}</strong><p className="muted">{metric.value===null && metric.label.startsWith('Sales') && mayReadSales ? 'Sales aggregate unavailable' : metric.note}</p></article>)}</section>
            <div className="dashboard-split">
                <section className="panel"><div className="page-top"><h2>Recent sales</h2>{mayReadSales&&<Link className="muted" href="/sales">View all</Link>}</div>
                    {sales?.length ? <div className="table-scroll"><table><thead><tr><th>Reference</th><th>Business date</th><th>Status</th><th>Amount</th></tr></thead><tbody>{sales.slice(0,8).map(row=><tr key={row.id}><td><Link href={'/sales/'+row.id}>{row.id.slice(0,8)}</Link></td><td>{row.businessDate}</td><td><span className="badge">{row.status}</span></td><td>{xaf(row.totalXaf)}</td></tr>)}</tbody></table></div> : <p className="muted">{mayReadSales?'No sales in this branch.':'Sales access required.'}</p>}
                </section>
                <section className="panel dashboard-access"><h2>Today’s priorities</h2>
                    {permitted(me,'inventory:read')&&<p><Link href="/inventory">Review stock</Link></p>}
                    {permitted(me,'tenders:read')&&<p><Link href="/finance">Review collections and payments</Link></p>}
                    <p className="muted">Live order, reorder and collection priorities are pending integration.</p>
                </section>
            </div>
        </>;
    } catch (error) {
        return <Access error={error}/>;
    }
}
