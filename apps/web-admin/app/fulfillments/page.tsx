import Link from 'next/link';
import {api, ApiError, Me, permitted} from '@/lib/api';
import {FulfillmentPage, fulfillmentTabs} from '@/lib/fulfillment';
import {Access} from '@/components/Access';

export const dynamic = 'force-dynamic';

export default async function FulfillmentQueue({searchParams}: {
    searchParams: Promise<{status?: string; q?: string; cursor?: string}>
}) {
    try {
        const query = await searchParams;
        const status = query.status ?? 'PENDING';
        if (!fulfillmentTabs.some(tab => tab.status === status))
            throw new ApiError(422, 'Choose an available fulfillment status.');
        const filters = new URLSearchParams({status, q: query.q ?? '', limit: '25'});
        const request = new URLSearchParams(filters);
        if (query.cursor) request.set('cursor', query.cursor);
        const [page, me] = await Promise.all([
            api<FulfillmentPage>('fulfillments/page?' + request), api<Me>('me'),
        ]);
        const next = new URLSearchParams(filters);
        if (page.nextCursor) next.set('cursor', page.nextCursor);
        return <>
            <p className="eyebrow">FULFILLMENT</p>
            <div className="page-top"><h1>Pickup &amp; Delivery Queue</h1>
                {permitted(me, 'sales:write') && <Link className="button" href="/sales">Create dispatch from a sale</Link>}
            </div>
            <div className="record-tabs" aria-label="Fulfillment status">
                {fulfillmentTabs.map(tab => <Link className="secondary" key={tab.status}
                    aria-current={tab.status === status ? 'page' : undefined}
                    href={'/fulfillments?' + new URLSearchParams({status: tab.status, q: query.q ?? ''})}>
                    {tab.label}
                </Link>)}
            </div>
            <section className="panel">
                <form className="quick" action="/fulfillments">
                    <input type="hidden" name="status" value={status}/>
                    <label htmlFor="fulfillment-search">Customer, recipient or reference</label>
                    <input id="fulfillment-search" name="q" type="search" maxLength={100} defaultValue={query.q ?? ''}/>
                    <button className="secondary">Filter</button>
                </form>
                {page.items.length ? <div className="table-scroll"><table>
                    <thead><tr><th>Reference</th><th>Sale</th><th>Customer</th><th>Method</th><th>Scheduled</th><th>Status</th></tr></thead>
                    <tbody>{page.items.map(({fulfillment: task, customerName}) => <tr key={task.id}>
                        <td><Link href={'/fulfillments/' + task.id}>{task.id.slice(0, 8)}</Link></td>
                        <td><Link href={'/sales/' + task.saleId}>{task.saleId.slice(0, 8)}</Link></td>
                        <td>{customerName}</td>
                        <td>{task.delivery?.method === 'PICKUP' ? 'Pickup' : task.delivery?.method === 'DELIVERY' ? 'Delivery' : 'Unspecified'}</td>
                        <td>{task.delivery?.scheduledDate ?? '—'}</td>
                        <td><span className="badge">{task.status}</span></td>
                    </tr>)}</tbody>
                </table></div> : <p>No pickup or delivery tasks match these filters.</p>}
                <div className="quick" aria-label="Queue pages">
                    {query.cursor && <Link className="secondary" href={'/fulfillments?' + filters}>First page</Link>}
                    {page.nextCursor && <Link className="secondary" href={'/fulfillments?' + next}>Next page</Link>}
                </div>
            </section>
        </>;
    } catch (error) {
        return <Access error={error}/>;
    }
}
