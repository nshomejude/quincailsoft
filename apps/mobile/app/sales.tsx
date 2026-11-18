import {router} from 'expo-router';
import {useCallback, useEffect, useRef, useState} from 'react';
import {Text, TextInput} from 'react-native';
import * as Crypto from 'expo-crypto';
import {Page, Card, Action, styles} from '../src/ui';
import {useSession, request, scopeKey, hasAuthority, type Session} from '../src/session';
import {database} from '../src/storage';
import {enqueue} from '../src/queue';

type Named = {id: string; name: string; active: boolean};
type Sale = {id: string; status: string; totalXaf: string};
type DraftOptions = {products: Named[]; customers: Named[]; warehouses: Named[]};

export default function Sales() {
  const {session} = useSession();
  return <Page title="Sales">{session
    ? <ScopedSales key={scopeKey(session)} session={session}/>
    : <Text>Sign in to access sales.</Text>}</Page>;
}

function ScopedSales({session}: {session: Session}) {
  const canRead = hasAuthority(session, 'sales:read');
  const canDraft = hasAuthority(session, 'sales:write') && hasAuthority(session, 'catalog:read');
  const [rows, setRows] = useState<Sale[]>([]);
  const [options, setOptions] = useState<DraftOptions | null>(null);
  const [product, setProduct] = useState('');
  const [customer, setCustomer] = useState('');
  const [warehouse, setWarehouse] = useState('');
  const [quantity, setQuantity] = useState('1000');
  const [listError, setListError] = useState('');
  const [draftError, setDraftError] = useState('');
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);
  const saving = useRef(false);
  const loading = useRef<AbortController | null>(null);

  const refresh = useCallback(async () => {
    loading.current?.abort();
    const controller = new AbortController();
    loading.current = controller;
    const {signal} = controller;
    setListError('');
    setDraftError('');
    if (canRead) {
      void request<Sale[]>(session, 'sales', undefined, undefined, signal).then(sales => {
        if (!signal.aborted) setRows(sales);
      }).catch(error => {
        if (!signal.aborted) {
          setRows([]);
          setListError(String(error));
        }
      });
    }
    if (canDraft) {
      try {
        const [products, customers, warehouses] = await Promise.all([
          request<Named[]>(session, 'products', undefined, undefined, signal),
          request<Named[]>(session, 'customers', undefined, undefined, signal),
          request<Named[]>(session, 'warehouses', undefined, undefined, signal),
        ]);
        if (!signal.aborted) {
          setOptions({
            products: products.filter(item => item.active),
            customers: customers.filter(item => item.active),
            warehouses: warehouses.filter(item => item.active),
          });
        }
      } catch (error) {
        if (!signal.aborted) {
          setOptions(null);
          setDraftError(String(error));
        }
      }
    }
  }, [canDraft, canRead, session]);

  useEffect(() => {
    void refresh();
    return () => loading.current?.abort();
  }, [refresh]);

  async function save() {
    if (!canDraft || !options || saving.current) return;
    saving.current = true;
    setBusy(true);
    setNote('');
    setDraftError('');
    try {
      if (!options.products.some(item => item.id === product)
          || !options.customers.some(item => item.id === customer)
          || !options.warehouses.some(item => item.id === warehouse)) {
        throw Error('Select an active product, customer and warehouse.');
      }
      if (!/^\d{1,19}$/.test(quantity) || BigInt(quantity) <= 0n || BigInt(quantity) > 9223372036854775807n) {
        throw Error('Enter a positive whole quantity in thousandths, within the supported range.');
      }
      const now = new Date();
      const dateParts = new Intl.DateTimeFormat('en-CA', {
        timeZone: 'Africa/Douala', year: 'numeric', month: '2-digit', day: '2-digit',
      }).formatToParts(now);
      const datePart = (part: string) => dateParts.find(item => item.type === part)?.value;
      const operationId = Crypto.randomUUID();
      const payload = {
        operationId, warehouseId: warehouse, customerId: customer,
        businessDate: [datePart('year'), datePart('month'), datePart('day')].join('-'),
        lines: [{productId: product, quantityMilli: BigInt(quantity).toString()}],
      };
      const scope = scopeKey(session);
      await enqueue(await database(scope), {
        operationId, scope, classification: 'DRAFT', kind: 'SALE_DRAFT',
        payload: JSON.stringify(payload), createdAt: now.toISOString(),
      });
      setNote('Draft saved. Send it from Sync queue.');
      setProduct('');
      setCustomer('');
      setWarehouse('');
      setQuantity('1000');
    } catch (error) {
      setDraftError(String(error));
    } finally {
      saving.current = false;
      setBusy(false);
    }
  }

  return <>
    {(canRead || canDraft) && <Action title="Refresh" onPress={() => void refresh()}/>}
    {!canRead && <Text>Your current session cannot view sales.</Text>}
    {listError ? <Text style={styles.error}>{listError}</Text> : null}
    {canDraft && <Card>
      <Text style={styles.text}>New queued draft</Text>
      {draftError ? <Text style={styles.error}>{draftError}</Text> : null}
      {note ? <Text>{note}</Text> : null}
      {options && <>
        <Text>Product</Text>
        {options.products.map(item => <Action key={item.id} title={(product === item.id ? '✓ ' : '') + item.name} onPress={() => setProduct(item.id)}/>)}
        <Text>Customer</Text>
        {options.customers.map(item => <Action key={item.id} title={(customer === item.id ? '✓ ' : '') + item.name} onPress={() => setCustomer(item.id)}/>)}
        <Text>Warehouse</Text>
        {options.warehouses.map(item => <Action key={item.id} title={(warehouse === item.id ? '✓ ' : '') + item.name} onPress={() => setWarehouse(item.id)}/>)}
        <Text>Quantity in thousandths (1000 = one base unit)</Text>
        <TextInput style={styles.input} keyboardType="number-pad" value={quantity} onChangeText={setQuantity} maxLength={19}/>
        <Action title={busy ? 'Saving…' : 'Save local draft'} disabled={busy} onPress={() => void save()}/>
      </>}
    </Card>}
    {canRead && rows.map(sale => <Card key={sale.id}>
      <Text selectable>{sale.id}</Text>
      <Text>{sale.status} · {sale.totalXaf} FCFA</Text>
      <Action title="View sale" onPress={() => router.push({pathname: '/sale/[id]', params: {id: sale.id}})}/>
    </Card>)}
  </>;
}
