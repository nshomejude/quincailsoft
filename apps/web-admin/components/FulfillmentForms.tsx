'use client';

import {useEffect, useRef, useState} from 'react';
import {useMutation} from '@tanstack/react-query';
import {useRouter} from 'next/navigation';
import {CommandIdentity} from '@/lib/command-input';
import {dispatchLines} from '@/lib/fulfillment-input';
import {units} from '@/lib/format';
import type {Fulfillment, PickLine} from '@/lib/fulfillment';

async function sendCommand(path: string, payload: Record<string, unknown>, identity: CommandIdentity): Promise<Fulfillment> {
    const operationId = identity.forRequest(path, 'POST', payload);
    const response = await fetch('/api/business/' + path, {
        method: 'POST', headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({...payload, operationId}),
    });
    const data = await response.json().catch(() => null);
    if (!response.ok) throw new Error(data?.message ?? 'The fulfillment operation failed.');
    if (typeof data?.id !== 'string') throw new Error('The business service returned an incomplete response.');
    return data;
}

export function FulfillmentDraftForm({saleId, customerId, businessDate}: {
    saleId: string; customerId: string; businessDate: string;
}) {
    const router = useRouter();
    const identity = useRef(new CommandIdentity(() => crypto.randomUUID()));
    const mutation = useMutation({
        mutationFn: (form: FormData) => {
            const text = (name: string) => String(form.get(name) ?? '').trim();
            return sendCommand('fulfillments', {
                saleId, customerId, businessDate: text('businessDate'), reason: text('reason'),
                delivery: {
                    method: text('method'), scheduledDate: text('scheduledDate') || null,
                    address: text('address') || null, recipientName: text('recipientName') || null,
                    recipientPhone: text('recipientPhone') || null, notes: text('notes') || null,
                },
            }, identity.current);
        },
        onSuccess: task => {router.push('/fulfillments/' + task.id); router.refresh();},
    });
    return <form className="form wide" onSubmit={event => {
        event.preventDefault(); mutation.mutate(new FormData(event.currentTarget));
    }}>
        <fieldset><legend>Pickup and delivery instructions</legend><div className="form-grid">
            <label>Business date<input name="businessDate" type="date" defaultValue={businessDate} min={businessDate} required/></label>
            <label>Method<select name="method" defaultValue="PICKUP" required>
                <option value="PICKUP">Pickup</option><option value="DELIVERY">Delivery</option>
            </select></label>
            <label>Scheduled date<input name="scheduledDate" type="date" min={businessDate}/></label>
            <label>Recipient name<input name="recipientName" maxLength={200} autoComplete="off"/></label>
            <label>Recipient phone<input name="recipientPhone" type="tel" maxLength={50} autoComplete="off"/></label>
        </div>
            <label>Address<textarea name="address" maxLength={1000}/></label>
            <label>Notes<textarea name="notes" maxLength={2000}/></label>
        </fieldset>
        <label>Reason<textarea name="reason" maxLength={2000} required/></label>
        {mutation.error && <p className="notice" role="alert">{mutation.error.message}</p>}
        <button className="button" disabled={mutation.isPending}>{mutation.isPending ? 'Saving…' : 'Prepare pickup / delivery'}</button>
    </form>;
}

export function FulfillmentDispatchForm({taskId, version, remaining, names}: {
    taskId: string; version: string; remaining: PickLine[]; names: Record<string, string>;
}) {
    const router = useRouter();
    const identity = useRef(new CommandIdentity(() => crypto.randomUUID()));
    const defaults = () => Object.fromEntries(remaining.map(line => [line.productId, units(line.quantityMilli)]));
    const [quantities, setQuantities] = useState<Record<string, string>>(defaults);
    const [reason, setReason] = useState('');
    const revision = JSON.stringify({version, remaining});
    useEffect(() => {setQuantities(defaults());}, [revision]);
    const mutation = useMutation({
        mutationFn: () => sendCommand('fulfillments/' + taskId + '/dispatch-lines', {
            version, reason: reason.trim(), lines: dispatchLines(remaining, quantities),
        }, identity.current),
        onSuccess: () => {setReason(''); router.refresh();},
    });
    return <form className="form wide" onSubmit={event => {event.preventDefault(); mutation.mutate();}}>
        <fieldset><legend>Items in this dispatch</legend>
            {remaining.map(line => <div className="line-row" key={line.productId}>
                <span>{names[line.productId] ?? line.productId}<small> · Remaining {units(line.quantityMilli)}</small></span>
                <label>Dispatch quantity<input inputMode="decimal" value={quantities[line.productId] ?? ''}
                    onChange={event => setQuantities(values => ({...values, [line.productId]: event.target.value}))}/></label>
            </div>)}
            <p className="muted">Enter zero for items to leave for a later dispatch.</p>
        </fieldset>
        <label>Dispatch reason<textarea maxLength={2000} required value={reason} onChange={event => setReason(event.target.value)}/></label>
        {mutation.error && <div role="alert" className="notice"><p>{mutation.error.message}</p>
            <button type="button" className="secondary" onClick={() => {mutation.reset(); router.refresh();}}>Refresh task</button>
        </div>}
        <button className="button" disabled={mutation.isPending || !remaining.length}>
            {mutation.isPending ? 'Recording…' : 'Record dispatch'}
        </button>
    </form>;
}
