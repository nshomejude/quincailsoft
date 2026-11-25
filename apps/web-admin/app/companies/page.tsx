import {api, Me} from '@/lib/api';
import {Company} from '@/lib/domains';
import {locale, messages} from '@/lib/locale';
import {Access} from '@/components/Access';
import {PageTitle, Records} from '@/components/Records';

export const dynamic = 'force-dynamic';

export default async function Companies() {
  try {
    const t = messages[await locale()];
    const [rows, me] = await Promise.all([api<Company[]>('companies'), api<Me>('me')]);
    const display = rows.map(row => ({...row, current: row.id === me.scope.companyId ? 'Current company' : ''}));

    return (
      <>
        <PageTitle title={t.companies} description="Companies in your organisation. This view is read-only." />
        <section className="panel">
          <Records
            rows={display}
            columns={[
              {key: 'name', label: 'Company'},
              {key: 'current', label: 'Your session'},
              {key: 'id', label: 'Reference'},
            ]}
          />
        </section>
      </>
    );
  } catch (error) {
    return <Access error={error} />;
  }
}
