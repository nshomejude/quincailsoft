import {api, Me} from '@/lib/api';
import {Branch} from '@/lib/domains';
import {locale, messages} from '@/lib/locale';
import {Access} from '@/components/Access';
import {PageTitle, Records} from '@/components/Records';

export const dynamic = 'force-dynamic';

export default async function Branches() {
  try {
    const t = messages[await locale()];
    const [rows, me] = await Promise.all([api<Branch[]>('branches'), api<Me>('me')]);
    const display = rows.map(row => ({...row, current: row.id === me.scope.branchId ? 'Current branch' : ''}));

    return (
      <>
        <PageTitle title={t.branches} description="Branches in your company. This view is read-only." />
        <section className="panel">
          <Records
            rows={display}
            columns={[
              {key: 'name', label: 'Branch'},
              {key: 'timezone', label: 'Time zone'},
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
