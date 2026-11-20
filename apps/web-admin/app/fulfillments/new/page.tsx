import Link from 'next/link';
import {redirect} from 'next/navigation';
import {api, ApiError, Me, Sale, permitted} from '@/lib/api';
import {Access} from '@/components/Access';
import {FulfillmentDraftForm} from '@/components/FulfillmentForms';

export const dynamic = 'force-dynamic';

export default async function NewFulfillment({searchParams}: {searchParams: Promise<{saleId?: string}>}) {
    const {saleId} = await searchParams;
    if (!saleId) redirect('/sales');
    try {
        if (!/^[0-9a-f-]{36}$/i.test(saleId)) throw new ApiError(422, 'Choose a valid sale.');
        const [sale, me] = await Promise.all([api<Sale>('sales/' + saleId), api<Me>('me')]);
        if (!permitted(me, 'sales:write')) throw new ApiError(403);
        if (sale.status !== 'POSTED') throw new ApiError(422, 'Choose a posted sale before preparing pickup or delivery.');
        return <>
            <p className="eyebrow">FULFILLMENT</p><h1>Prepare pickup / delivery</h1>
            <section className="panel"><h2>Original sale</h2><dl>
                <div><dt>Sale</dt><dd><Link href={'/sales/' + sale.id}>{sale.id}</Link></dd></div>
                <div><dt>Customer</dt><dd>{sale.customerId}</dd></div>
                <div><dt>Warehouse</dt><dd>{sale.warehouseId}</dd></div>
            </dl></section>
            <section className="panel"><FulfillmentDraftForm saleId={sale.id} customerId={sale.customerId} businessDate={sale.businessDate}/></section>
        </>;
    } catch (error) {return <Access error={error}/>;}
}
