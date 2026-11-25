import Link from 'next/link';
import {notFound, unstable_rethrow} from 'next/navigation';
import {api, Me, permitted, Sale, xaf} from '@/lib/api';
import {Customer, Deposit} from '@/lib/domains';
import {Access} from '@/components/Access';
import {PageTitle, Records} from '@/components/Records';

export const dynamic = 'force-dynamic';

export default async function CustomerDetail({params}: {params: Promise<{id: string}>}) {
  try {
    const {id} = await params;
    const [customers, me] = await Promise.all([api<Customer[]>('customers'), api<Me>('me')]);
    const customer = customers.find(row => row.id === id);
    if (!customer) notFound();

    // There is no per-customer endpoint, so linked records are derived from the scoped lists.
    const [deposits, sales] = await Promise.all([
      permitted(me, 'tenders:read') ? api<Deposit[]>('deposits') : Promise.resolve(null),
      permitted(me, 'sales:read') ? api<Sale[]>('sales') : Promise.resolve(null),
    ]);
    const customerDeposits = deposits?.filter(row => row.customerId === id) ?? [];
    const customerSales = sales?.filter(row => row.customerId === id) ?? [];
    const available = customerDeposits.reduce((sum, row) => sum + BigInt(row.remainingXaf), 0n);
    const posted = customerSales
      .filter(row => row.status === 'POSTED')
      .reduce((sum, row) => sum + BigInt(row.totalXaf), 0n);

    return (
      <>
        <PageTitle title={customer.name}>
          <Link className="secondary" href="/customers">All customers</Link>
        </PageTitle>
        <section className="panel">
          <dl>
            <div><dt>Status</dt><dd>{customer.active ? 'Active' : 'Inactive'}</dd></div>
            <div><dt>Reference</dt><dd>{customer.id}</dd></div>
            {deposits && <div><dt>Deposit balance available</dt><dd>{xaf(String(available))}</dd></div>}
            {sales && <div><dt>Posted sales (in view)</dt><dd>{xaf(String(posted))}</dd></div>}
          </dl>
        </section>
        <section className="panel">
          <h2>Customer deposits</h2>
          {deposits ? (
            <Records
              rows={customerDeposits}
              href="/finance/deposits"
              columns={[
                {key: 'id', label: 'Deposit'},
                {key: 'originalXaf', label: 'Received', money: true},
                {key: 'allocatedXaf', label: 'Allocated', money: true},
                {key: 'refundedXaf', label: 'Refunded', money: true},
                {key: 'remainingXaf', label: 'Available', money: true},
              ]}
            />
          ) : (
            <p className="muted">Your role cannot read deposits.</p>
          )}
        </section>
        <section className="panel">
          <h2>Sales</h2>
          {sales ? (
            <Records
              rows={customerSales}
              href="/sales"
              columns={[
                {key: 'id', label: 'Sale'},
                {key: 'businessDate', label: 'Date'},
                {key: 'status', label: 'Status'},
                {key: 'totalXaf', label: 'Total', money: true},
              ]}
            />
          ) : (
            <p className="muted">Your role cannot read sales.</p>
          )}
        </section>
      </>
    );
  } catch (error) {
    // notFound() throws a framework signal that must not be swallowed.
    unstable_rethrow(error);
    return <Access error={error} />;
  }
}
