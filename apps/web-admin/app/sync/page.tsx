import {api, ApiError, Me, permitted} from '@/lib/api';
import {RecoveryReceipt, SyncDevice} from '@/lib/domains';
import {locale, messages} from '@/lib/locale';
import {Access} from '@/components/Access';
import {PageTitle, Records} from '@/components/Records';

export const dynamic = 'force-dynamic';

export default async function SyncDevices() {
  try {
    const t = messages[await locale()];
    const me = await api<Me>('me');
    if (!permitted(me, 'sync:manage')) throw new ApiError(403);

    // Recovery submissions need a separate permission, so they are shown only when it is held.
    const [devices, recovery] = await Promise.all([
      api<SyncDevice[]>('sync/devices'),
      permitted(me, 'sync:recover') ? api<RecoveryReceipt[]>('sync/recovery-submissions') : Promise.resolve(null),
    ]);
    const deviceRows = devices.map(device => ({
      ...device,
      offline: device.offlineSelling ? 'Allowed' : 'Not allowed',
      fingerprint: device.fingerprint ? device.fingerprint.slice(0, 16) : '—',
    }));
    const recoveryRows = (recovery ?? []).map(receipt => ({
      ...receipt,
      id: receipt.evidenceId,
      posted: receipt.businessPosted ? 'Yes' : 'No',
    }));

    return (
      <>
        <PageTitle
          title={t.devices}
          description="Devices enrolled for offline selling and their recovery submissions. This view is read-only; enrolment and approval happen from the device flow."
        />
        <section className="panel">
          <h2>Enrolled devices</h2>
          <Records
            rows={deviceRows}
            columns={[
              {key: 'name', label: 'Device'},
              {key: 'status', label: 'Status'},
              {key: 'offline', label: 'Offline selling'},
              {key: 'fingerprint', label: 'Key fingerprint'},
              {key: 'expiresAt', label: 'Challenge expires'},
            ]}
          />
        </section>
        <section className="panel">
          <h2>Recovery submissions</h2>
          {recovery === null ? (
            <p className="muted">Your role cannot read recovery submissions.</p>
          ) : (
            <Records
              rows={recoveryRows}
              columns={[
                {key: 'receivedAt', label: 'Received'},
                {key: 'status', label: 'Status'},
                {key: 'posted', label: 'Business posted'},
                {key: 'sourceOperationId', label: 'Source operation'},
              ]}
            />
          )}
        </section>
      </>
    );
  } catch (error) {
    return <Access error={error} />;
  }
}
