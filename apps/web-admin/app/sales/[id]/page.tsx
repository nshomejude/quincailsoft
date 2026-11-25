import Link from 'next/link';
import {api, Sale, Me, permitted, Product, xaf, units} from '@/lib/api';
import {DocumentSnapshot} from '@/lib/domains';
import {Access} from '@/components/Access';
import {PostForm} from '@/components/SaleForms';
import {ReturnForm} from '@/components/ReturnForm';

export const dynamic = 'force-dynamic';

type Step = {label: string; state: 'done' | 'current' | 'pending' | 'stopped'};

// The sales service only knows DRAFT and POSTED, so any other status is shown as stopped.
function timeline(status: string, documentCount: number, canSeeDocuments: boolean): Step[] {
  if (status !== 'DRAFT' && status !== 'POSTED') {
    return [
      {label: 'Draft created', state: 'done'},
      {label: 'Status: ' + status, state: 'stopped'},
    ];
  }
  const posted = status === 'POSTED';
  const steps: Step[] = [
    {label: 'Draft created', state: 'done'},
    {label: posted ? 'Posted to stock and ledger' : 'Awaiting review and posting', state: posted ? 'done' : 'current'},
  ];
  if (canSeeDocuments) {
    steps.push({
      label: documentCount > 0 ? 'Document issued' : 'Document not yet issued',
      state: documentCount > 0 ? 'done' : posted ? 'current' : 'pending',
    });
  }
  return steps;
}

export default async function Detail({params}: {params: Promise<{id: string}>}) {
  try {
    const {id} = await params;
    const [sale, me, products] = await Promise.all([
      api<Sale>('sales/' + id),
      api<Me>('me'),
      api<Product[]>('products'),
    ]);
    const names = Object.fromEntries(products.map(product => [product.id, product.name]));

    const canSeeDocuments = permitted(me, 'documents:read');
    let docs: DocumentSnapshot[] = [];
    if (canSeeDocuments) {
      docs = await api<DocumentSnapshot[]>('documents?sourceId=' + id);
    }
    const steps = timeline(sale.status, docs.length, canSeeDocuments);

    return (
      <>
        <h1>Sale {sale.id.slice(0, 8)}</h1>
        {docs.length > 0 && (
          <div className="quick">
            {docs.map(doc => (
              <a
                key={doc.id}
                className="button"
                target="_blank"
                rel="noreferrer"
                href={'/documents/' + doc.id + '/print'}
              >
                Print {doc.kind.toLowerCase().replace(/_/g, ' ')} {doc.number}
              </a>
            ))}
          </div>
        )}
        <ol className="timeline" aria-label="Sale progress">
          {steps.map(step => (
            <li key={step.label} data-state={step.state} aria-current={step.state === 'current' ? 'step' : undefined}>
              {step.label}
            </li>
          ))}
        </ol>
        <section className="panel">
          <dl>
            <div><dt>Status</dt><dd>{sale.status}</dd></div>
            <div><dt>Business date</dt><dd>{sale.businessDate}</dd></div>
            <div><dt>Total</dt><dd>{xaf(sale.totalXaf)}</dd></div>
            <div><dt>Warehouse</dt><dd>{sale.warehouseId}</dd></div>
          </dl>
          <div className="table-scroll">
            <table>
              <thead>
                <tr><th>Product</th><th>Quantity</th><th>Unit price</th><th>Total</th></tr>
              </thead>
              <tbody>
                {sale.lines.map(line => (
                  <tr key={line.id}>
                    <td>{names[line.productId] ?? line.productId}</td>
                    <td>{units(line.quantityMilli)}</td>
                    <td>{xaf(line.unitPriceXaf)}</td>
                    <td>{xaf(line.lineTotalXaf)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
        {sale.status === 'DRAFT' && permitted(me, 'sales:post') && (
          <section className="panel">
            <h2>Review and post</h2>
            <PostForm saleId={sale.id} version={sale.version} />
          </section>
        )}
        {docs.length > 0 && (
          <section className="panel">
            <h2>Issued documents</h2>
            {docs.map(doc => (
              <p key={doc.id}>
                <Link href={'/documents/' + doc.id}>{doc.number} · {doc.kind}</Link>
              </p>
            ))}
          </section>
        )}
        {sale.status === 'POSTED' && permitted(me, 'returns:write') && (
          <section className="panel">
            <h2>Record customer return</h2>
            <ReturnForm sale={sale} names={names} />
          </section>
        )}
      </>
    );
  } catch (error) {
    return <Access error={error} />;
  }
}
