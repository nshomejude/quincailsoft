'use client';

import {useRef, useState} from 'react';
import {CommandIdentity} from '@/lib/command-input';

type Attempt = {id: string; html: string; status: string};

export function FulfillmentPrint({taskId, templateId}: {taskId: string; templateId: 'PICK_TICKET' | 'DOC12'}) {
    const captureId = useRef<string | null>(null);
    const captureKey = useRef(new CommandIdentity(() => crypto.randomUUID()));
    const printKey = useRef(new CommandIdentity(() => crypto.randomUUID()));
    const outcomeKey = useRef(new CommandIdentity(() => crypto.randomUUID()));
    const frame = useRef<HTMLIFrameElement>(null);
    const [attempt, setAttempt] = useState<Attempt | null>(null);
    const [reason, setReason] = useState('');
    const [busy, setBusy] = useState(false);
    const [loaded, setLoaded] = useState(false);
    const [opened, setOpened] = useState(false);
    const [error, setError] = useState('');
    const [outcome, setOutcome] = useState('');

    async function command(path: string, payload: Record<string, unknown>, identity: CommandIdentity) {
        const operationId = identity.forRequest(path, 'POST', payload);
        const response = await fetch('/api/business/' + path, {
            method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({...payload, operationId}),
        });
        const data = await response.json().catch(() => null);
        if (!response.ok) throw new Error(data?.message ?? 'The document operation failed. Retry the same request.');
        if (typeof data?.id !== 'string') throw new Error('The document service returned an incomplete response. Retry the same request.');
        return data;
    }

    async function prepare() {
        setBusy(true); setError('');
        try {
            if (!captureId.current) {
                const capture = await command('documents/captures', {templateId, sourceId: taskId}, captureKey.current);
                captureId.current = capture.id;
            }
            const result = await command('documents/captures/' + captureId.current + '/print-attempts',
                {paper: 'A4', reason: reason.trim()}, printKey.current);
            if (typeof result.html !== 'string' || result.status !== 'REQUESTED')
                throw new Error('The document service did not return a printable request. Retry the same request.');
            setLoaded(false); setOpened(false); setAttempt(result);
        } catch (failure) {setError(failure instanceof Error ? failure.message : 'The document operation failed.');}
        finally {setBusy(false);}
    }

    async function record(status: 'PRINTED' | 'FAILED') {
        if (!attempt) return;
        setBusy(true); setError('');
        try {
            const result = await command('documents/print-attempts/' + attempt.id + '/outcomes',
                {status, reason: reason.trim()}, outcomeKey.current);
            if (result.status !== status) throw new Error('The service did not confirm this print outcome. Retry the same request.');
            setOutcome(result.status);
        } catch (failure) {setError(failure instanceof Error ? failure.message : 'The print outcome could not be recorded.');}
        finally {setBusy(false);}
    }

    return <section className="panel"><h2>{templateId === 'PICK_TICKET' ? 'Print pick ticket' : 'Print delivery note'}</h2>
        {!attempt ? <form className="form" onSubmit={event => {event.preventDefault(); void prepare();}}>
            <label>Print reason<textarea required minLength={3} maxLength={2000} value={reason}
                onChange={event => setReason(event.target.value)} disabled={busy}/></label>
            <button disabled={busy}>{busy ? 'Preparing document…' : 'Prepare printable document'}</button>
        </form> : <>
            <p>A print request has been recorded. Confirm the outcome after checking the printer.</p>
            <iframe ref={frame} title={templateId === 'PICK_TICKET' ? 'Pick ticket preview' : 'Delivery note preview'}
                className="document-preview" sandbox="allow-modals allow-same-origin" srcDoc={attempt.html}
                onLoad={() => setLoaded(true)}/>
            <div className="quick"><button disabled={!loaded || busy || opened} onClick={() => {
                setOpened(true);
                try {frame.current?.contentWindow?.print();}
                catch {setError('The browser could not open printing. Check browser permissions and try again.');}
            }}>Open browser print dialog</button>
                {!outcome && <><button disabled={busy} onClick={() => void record('PRINTED')}>Printed — operator confirmed</button>
                    <button className="secondary" disabled={busy} onClick={() => void record('FAILED')}>Record print failure</button></>}
            </div>
            {outcome && <><p role="status">Recorded outcome: {outcome}. This outcome was confirmed by the operator.</p>
                <button className="secondary" onClick={() => {
                    printKey.current = new CommandIdentity(() => crypto.randomUUID());
                    outcomeKey.current = new CommandIdentity(() => crypto.randomUUID());
                    setAttempt(null); setOutcome(''); setOpened(false); setLoaded(false); setError('');
                }}>Start another print attempt</button></>}
        </>}
        {error && <p role="alert">{error}</p>}
    </section>;
}
