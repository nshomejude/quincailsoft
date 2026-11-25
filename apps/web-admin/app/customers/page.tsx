import {api} from '@/lib/api';
import {Customer} from '@/lib/domains';
import {locale, messages} from '@/lib/locale';
import {Access} from '@/components/Access';
import {PageTitle, Records} from '@/components/Records';

export const dynamic = 'force-dynamic';

export default async function Customers() {
  try {
    const t = messages[await locale()];
    const rows = await api<Customer[]>('customers');
    const display = rows.map(row => ({...row, status: row.active ? 'Active' : 'Inactive'}));

    return (
      <>
        <PageTitle
          title={t.customers}
          description="Customers available for sales, quotations and deposits in your scope."
        />
        <section className="panel">
          <Records
            rows={display}
            href="/customers"
            columns={[
              {key: 'name', label: 'Customer'},
              {key: 'status', label: 'Status'},
              {key: 'id', label: 'Reference'},
            ]}
          />
        </section>
        <section className="panel">
          <h2>Add a customer</h2>
          <p className="notice" role="note">
            Not yet available. The business service can list customers but has no endpoint to
            create or edit them, so customer registration is disabled until that endpoint exists.
          </p>
        </section>
      </>
    );
  } catch (error) {
    return <Access error={error} />;
  }
}
