import Link from 'next/link';
import {api, Me, Named, Product, Sale, permitted, units} from '@/lib/api';
import {DispatchEntry, Fulfillment, PickList} from '@/lib/fulfillment';
import {Access} from '@/components/Access';
import {CommandForm} from '@/components/CommandForm';
import {FulfillmentDispatchForm} from '@/components/FulfillmentForms';

export const dynamic = 'force-dynamic';

export default async function FulfillmentDetail({params}: {params: Promise<{id: string}>}) {
    try {
        const {id} = await params;
        const [task, pick, history, me] = await Promise.all([
            api<Fulfillment>('fulfillments/' + id), api<PickList>('fulfillments/' + id + '/picklist'),
            api<DispatchEntry[]>('fulfillments/' + id + '/dispatches'), api<Me>('me'),
        ]);
        const [sale, products, customer] = await Promise.all([
            api<Sale>('sales/' + task.saleId), permitted(me, 'catalog:read') ? api<Product[]>('products') : [],
            permitted(me, 'customers:read') ? api<Named>('customers/' + task.customerId) : null,
        ]);
        const names = Object.fromEntries(products.map(product => [product.id, product.name]));
        const remaining = Object.fromEntries(pick.lines.map(line => [line.productId, line.quantityMilli]));
        const dispatched = new Map<string, bigint>();
        for (const entry of history) dispatched.set(entry.productId,
            (dispatched.get(entry.productId) ?? 0n) + BigInt(entry.quantityMilli));
        return <>
            <p><Link href="/fulfillments">Pickup &amp; Delivery Queue</Link></p>
            <p className="eyebrow">FULFILLMENT</p>
            <div className="page-top"><h1>Pickup / Delivery Detail</h1><span className="badge">{task.status}</span></div>
            <div className="record-tabs" aria-label="Delivery sections">
                <a className="secondary" href="#reservation">Reservation</a><a className="secondary" href="#items">Line items</a>
                <a className="secondary" href="#dispatch-history">Dispatch history</a><a className="secondary" href="#confirmation">Confirmation</a>
            </div>
            <section className="panel" id="reservation"><h2>Reservation and delivery instructions</h2><dl>
                <div><dt>Reference</dt><dd>{task.id}</dd></div>
                <div><dt>Sale</dt><dd><Link href={'/sales/' + task.saleId}>{task.saleId}</Link></dd></div>
                <div><dt>Customer</dt><dd>{customer?.name ?? task.customerId}</dd></div><div><dt>Warehouse</dt><dd>{pick.warehouseId}</dd></div>
                <div><dt>Business date</dt><dd>{task.businessDate}</dd></div>
                <div><dt>Method</dt><dd>{task.delivery?.method ?? 'Unspecified'}</dd></div>
                <div><dt>Scheduled date</dt><dd>{task.delivery?.scheduledDate ?? '—'}</dd></div>
                <div><dt>Recipient</dt><dd>{task.delivery?.recipientName ?? '—'}</dd></div>
                <div><dt>Phone</dt><dd>{task.delivery?.recipientPhone ?? '—'}</dd></div>
                <div><dt>Address</dt><dd>{task.delivery?.address ?? '—'}</dd></div>
                <div><dt>Notes</dt><dd>{task.delivery?.notes ?? '—'}</dd></div>
            </dl></section>
            <section className="panel" id="items"><h2>Line items</h2><div className="table-scroll"><table>
                <thead><tr><th>Product</th><th>Authorized quantity</th><th>Recorded dispatch</th><th>Remaining</th></tr></thead>
                <tbody>{sale.lines.map(line => <tr key={line.id}><td>{names[line.productId] ?? line.productId}</td>
                    <td>{units(line.quantityMilli)}</td><td>{dispatched.has(line.productId) ? units(String(dispatched.get(line.productId))) : '—'}</td>
                    <td>{units(remaining[line.productId] ?? '0')}</td></tr>)}</tbody>
            </table></div></section>
            {permitted(me, 'sales:write') && ['PENDING', 'PARTIAL'].includes(task.status) &&
                <section className="panel"><h2>Prepare dispatch</h2><FulfillmentDispatchForm taskId={id}
                    version={task.version} remaining={pick.lines} names={names}/></section>}
            <section className="panel" id="dispatch-history"><h2>Dispatch history</h2>
                {history.length ? <div className="table-scroll"><table>
                    <thead><tr><th>Time</th><th>Product</th><th>Quantity</th><th>Actor</th><th>Operation</th></tr></thead>
                    <tbody>{history.map(entry => <tr key={entry.operationId + entry.productId}>
                        <td>{entry.dispatchedAt}</td><td>{names[entry.productId] ?? entry.productId}</td><td>{units(entry.quantityMilli)}</td>
                        <td>{entry.dispatchedBy}</td><td>{entry.operationId}</td>
                    </tr>)}</tbody>
                </table></div> : <p>No dispatch lines have been recorded for this task.</p>}
            </section>
            <section className="panel" id="confirmation"><h2>Delivery confirmation</h2>
                {task.status === 'COMPLETED' ? <p>Proof reference · {task.proofReference}</p> :
                    task.status === 'PARTIAL' ? <p>Dispatch the remaining items before completing delivery.</p> :
                    permitted(me, 'sales:write') && <CommandForm path={'fulfillments/' + id + '/complete'} fixed={{version: task.version}}
                        fields={[{name: 'proofReference', label: 'Recipient proof reference', maxLength: 2000},
                            {name: 'reason', label: 'Confirmation reason', type: 'textarea'}]} title="Confirm completed delivery"/>}
            </section>
        </>;
    } catch (error) {return <Access error={error}/>;}
}
