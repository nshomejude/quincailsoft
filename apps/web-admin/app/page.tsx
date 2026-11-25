import Link from 'next/link';
import {session} from '@/lib/auth';
import {api, Me, Named, Product, Sale, permitted, units, xaf} from '@/lib/api';
import {CashSession, ReportResult, StockBalance} from '@/lib/domains';
import {Access} from '@/components/Access';

export const dynamic = 'force-dynamic';

const MAX_WAREHOUSES = 5;
const LOW_STOCK_ROWS = 8;

// A failing widget must not take the whole dashboard down; it falls back to an unavailable state.
async function optional<T>(request: Promise<T> | null): Promise<T | null> {
  if (!request) return null;
  try {
    return await request;
  } catch {
    return null;
  }
}

async function lowestStock(warehouses: Named[]) {
  const lists = await Promise.all(
    warehouses.slice(0, MAX_WAREHOUSES).map(warehouse =>
      api<StockBalance[]>('inventory/balances?warehouseId=' + warehouse.id),
    ),
  );
  return lists
    .flat()
    .map(balance => ({
      ...balance,
      availableMilli: BigInt(balance.quantityMilli) - BigInt(balance.reservedMilli),
    }))
    .sort((a, b) => (a.availableMilli < b.availableMilli ? -1 : a.availableMilli > b.availableMilli ? 1 : 0))
    .slice(0, LOW_STOCK_ROWS);
}

export default async function Dashboard() {
  try {
    const auth = await session();
    if (!auth) {
      return (
        <>
          <p className="eyebrow">YOUR BUSINESS, IN VIEW</p>
          <h1>Welcome to QuincaiSoft</h1>
          <section className="panel">
            <p>Sign in to access your authorised company and branch.</p>
            <a className="button" href="/auth/login">Sign in securely</a>
          </section>
        </>
      );
    }

    const me = await api<Me>('me');
    const can = (permission: string) => permitted(me, permission);

    const [products, sales, warehouses, cash, receivables] = await Promise.all([
      can('catalog:read') ? api<Product[]>('products') : Promise.resolve(null),
      can('sales:read') ? api<Sale[]>('sales') : Promise.resolve(null),
      optional(can('inventory:read') ? api<Named[]>('warehouses') : null),
      optional(can('cash:read') ? api<CashSession[]>('cash-sessions') : null),
      optional(can('reports:read') ? api<ReportResult>('reports/receivables?offset=0&limit=100') : null),
    ]);
    const lowStock = warehouses ? await optional(lowestStock(warehouses)) : null;

    const names = Object.fromEntries((products ?? []).map(product => [product.id, product.name]));
    const posted = sales?.filter(sale => sale.status === 'POSTED');
    const openCash = cash?.filter(row => row.status === 'OPEN') ?? [];
    const openCashExpected = openCash.reduce((sum, row) => sum + BigInt(row.expectedXaf), 0n);

    return (
      <>
        <p className="eyebrow">YOUR BUSINESS, IN VIEW</p>
        <h1>Operations overview</h1>
        <p className="lead">
          Welcome, {auth.name}. Your company and branch permissions apply throughout this workspace.
        </p>
        <div className="quick">
          {can('sales:write') && <Link className="button" href="/sales/new">Create sale draft</Link>}
          {can('purchasing:write') && <Link className="secondary" href="/purchases/new">Create purchase order</Link>}
          {can('inventory:read') && <Link className="secondary" href="/inventory">Review stock</Link>}
          {can('reports:read') && <Link className="secondary" href="/reports">Open reports</Link>}
        </div>

        <section className="stats four">
          {products && (
            <article><span>Products in view</span><strong>{products.length}</strong></article>
          )}
          {sales && (
            <article>
              <span>Sale drafts in view</span>
              <strong>{sales.filter(sale => sale.status === 'DRAFT').length}</strong>
            </article>
          )}
          {posted && (
            <article>
              <span>Posted sales in view</span>
              <strong>{xaf(String(posted.reduce((sum, sale) => sum + BigInt(sale.totalXaf), 0n)))}</strong>
            </article>
          )}
          {cash && (
            <article>
              <span>Open cash sessions</span>
              <strong>{openCash.length}</strong>
            </article>
          )}
        </section>

        {sales && (
          <section className="panel">
            <h2>Recent sales</h2>
            <p className="muted">
              These figures cover the returned records. Use reports for an explicitly scoped reporting period.
            </p>
            {sales.length === 0 ? (
              <p>No sales yet. Create a sale draft to begin.</p>
            ) : (
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr><th>Sale</th><th>Date</th><th>Status</th><th>Total</th></tr>
                  </thead>
                  <tbody>
                    {sales.slice(0, 8).map(sale => (
                      <tr key={sale.id}>
                        <td><Link href={'/sales/' + sale.id}>{sale.id.slice(0, 8)}</Link></td>
                        <td>{sale.businessDate}</td>
                        <td>{sale.status}</td>
                        <td>{xaf(sale.totalXaf)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        )}

        {can('inventory:read') && (
          <section className="panel">
            <h2>Lowest stock</h2>
            <p className="muted">
              Products with the least available stock (on hand minus reserved) across up to {MAX_WAREHOUSES} warehouses.
              Reorder thresholds are not configured yet, so this is a ranking rather than an alert.
            </p>
            {lowStock === null ? (
              <p className="notice">Stock levels are unavailable right now.</p>
            ) : lowStock.length === 0 ? (
              <p>No stock balances recorded yet.</p>
            ) : (
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr><th>Product</th><th>On hand</th><th>Reserved</th><th>Available</th></tr>
                  </thead>
                  <tbody>
                    {lowStock.map(row => (
                      <tr key={row.warehouseId + row.productId}>
                        <td>{names[row.productId] ?? row.productId}</td>
                        <td>{units(row.quantityMilli)}</td>
                        <td>{units(row.reservedMilli)}</td>
                        <td>{units(String(row.availableMilli))}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        )}

        {can('reports:read') && (
          <section className="panel">
            <h2>Receivables</h2>
            {receivables === null ? (
              <p className="notice">The receivables report is unavailable right now.</p>
            ) : receivables.rows.length === 0 ? (
              <p>No outstanding receivables.</p>
            ) : (
              <>
                <p className="muted">{receivables.basis}</p>
                <dl>
                  <div><dt>Customers listed</dt><dd>{receivables.rows.length}</dd></div>
                  {Object.entries(receivables.totals).map(([key, value]) => (
                    <div key={key}><dt>{key}</dt><dd>{value}</dd></div>
                  ))}
                </dl>
                <Link href="/reports?code=receivables">Open the full receivables report</Link>
              </>
            )}
          </section>
        )}

        {can('cash:read') && (
          <section className="panel">
            <h2>Cash position</h2>
            {cash === null ? (
              <p className="notice">Cash sessions are unavailable right now.</p>
            ) : openCash.length === 0 ? (
              <p>No cash session is open. {can('cash:manage') && <Link href="/finance/cash">Open a session</Link>}</p>
            ) : (
              <>
                <p>
                  {openCash.length} open {openCash.length === 1 ? 'session' : 'sessions'}, expecting{' '}
                  <strong>{xaf(String(openCashExpected))}</strong> in total.
                </p>
                <div className="table-scroll">
                  <table>
                    <thead>
                      <tr><th>Till</th><th>Date</th><th>Opening</th><th>Expected</th></tr>
                    </thead>
                    <tbody>
                      {openCash.map(row => (
                        <tr key={row.id}>
                          <td>{row.tillCode}</td>
                          <td>{row.businessDate}</td>
                          <td>{xaf(row.openingXaf)}</td>
                          <td>{xaf(row.expectedXaf)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <Link href="/finance/cash">Manage cash sessions</Link>
              </>
            )}
          </section>
        )}

        <section className="panel">
          <h2>Current scope</h2>
          <dl>
            <div><dt>Company</dt><dd>{me.scope.companyId}</dd></div>
            <div><dt>Branch</dt><dd>{me.scope.branchId}</dd></div>
          </dl>
        </section>
      </>
    );
  } catch (error) {
    return <Access error={error} />;
  }
}
