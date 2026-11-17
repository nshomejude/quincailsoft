"""Local integration recovery and import services. Caller owns transaction/commit.

Only server-created business-record backups may be restored. Credentials, audit,
issued document snapshots and provider records are never rolled back here.
"""
import csv
import hashlib
import hmac
import io
import json
import re
import secrets
import time
from datetime import date

MASTER_ENTITIES = {'product', 'customer', 'supplier', 'category', 'unit'}
MAX_ROWS = 500
MAX_CSV_BYTES = 512_000


def _server():
    import server
    return server


def _tables(c):
    c.execute('CREATE TABLE IF NOT EXISTS import_previews (id TEXT PRIMARY KEY, user_id TEXT, entity TEXT, payload TEXT, digest TEXT, expires REAL, committed INTEGER DEFAULT 0)')
    c.execute('CREATE TABLE IF NOT EXISTS operation_receipts (user_id TEXT, operation_id TEXT, request_hash TEXT, result TEXT, PRIMARY KEY(user_id,operation_id))')


def _owner(user):
    if user['role'] != 'owner':
        raise _server().APIError(403, 'Only the business owner can perform recovery operations.')


def _reason(body):
    value = body.get('reason')
    if not isinstance(value, str) or not value.strip() or len(value) > 2000:
        raise _server().APIError(422, 'A reason of 1–2000 characters is required.')
    return value.strip()


def _raw(value):
    return json.dumps(value, sort_keys=True, separators=(',', ':'), ensure_ascii=True, allow_nan=False)


def _digest(value):
    return hashlib.sha256(_raw(value).encode()).hexdigest()


def _entities(c):
    s = _server()
    return {e: sorted(s.all_records(c, e), key=lambda x: x['id']) for e in sorted(s.SCHEMAS)}


def state_digest(c):
    return _digest(_entities(c))


def _backup(c, user, reason):
    payload = _entities(c)
    ident = 'BKP-' + secrets.token_hex(12)
    digest = _digest(payload)
    c.execute('INSERT INTO backups VALUES(?,?,?,?)', (ident, time.time(), _raw(payload), digest))
    _server().event(c, user, 'backup', ident, 'Business backup created', reason)
    return {'id': ident, 'digest': digest, 'entities': payload, 'state_digest': digest,
            'records': sum(map(len, payload.values())), 'scope': 'business records only'}


def _load_backup(c, ident):
    if not isinstance(ident, str) or not ident or len(ident) > 100:
        raise _server().APIError(422, 'A backup ID is required.')
    row = c.execute('SELECT * FROM backups WHERE id=?', (ident,)).fetchone()
    if not row:
        raise _server().APIError(404, 'Backup not found.')
    try:
        payload = json.loads(row['payload'])
        # Accept older harness backups, whose JSON serialization included spaces.
        valid = hmac.compare_digest(_digest(payload), row['digest']) or hmac.compare_digest(
            hashlib.sha256(json.dumps(payload, sort_keys=True).encode()).hexdigest(), row['digest'])
        _validate_backup(payload)
    except (ValueError, TypeError, KeyError):
        raise _server().APIError(422, 'Backup is malformed.')
    if not valid:
        raise _server().APIError(422, 'Backup integrity verification failed.')
    return row, payload


def _validate_backup(payload):
    s = _server()
    if not isinstance(payload, dict) or set(payload) != set(s.SCHEMAS):
        raise s.APIError(422, 'Backup must contain exactly the current business entity schemas.')
    for entity, records in payload.items():
        if not isinstance(records, list):
            raise s.APIError(422, 'Invalid record collection in backup.')
        seen = set()
        for record in records:
            if not isinstance(record, dict) or not isinstance(record.get('id'), str) or not record['id'] or record['id'] in seen:
                raise s.APIError(422, 'Invalid or duplicate record identity in backup.')
            seen.add(record['id'])
            if record.get('tenant_id') != 'demo' or type(record.get('version')) is not int or record['version'] < 1:
                raise s.APIError(422, 'Backup tenant or version is invalid.')
            if not isinstance(record.get('status'), str) or not isinstance(record.get('reference'), str):
                raise s.APIError(422, 'Backup record metadata is incomplete.')
            if len(_raw(record)) > 250_000:
                raise s.APIError(422, 'Backup record exceeds the local recovery limit.')
    # Referential fields already present must point to records in this backup.
    ids = {e: {r['id'] for r in rs} for e, rs in payload.items()}
    for entity, records in payload.items():
        relation_fields = [f for f in s.SCHEMAS[entity]['fields'] if f['type'] == 'relation']
        for record in records:
            for key, target in (('branch_id','branch'),('company_id','company')):
                if record.get(key) and record[key] not in ids[target]:
                    raise s.APIError(422, 'Backup contains an invalid organization scope.')
            for f in relation_fields:
                value = record.get(f['key'])
                if value and value not in ids.get(f['entity'], set()):
                    raise s.APIError(422, 'Backup contains a broken relation: ' + entity + '.' + f['key'])


def _receipt(c, user, body, operation):
    ident = body.get('operation_id')
    if not isinstance(ident, str) or not re.fullmatch(r'[A-Za-z0-9_-]{8,100}', ident):
        raise _server().APIError(422, 'An operation ID of 8–100 letters, digits, hyphens or underscores is required.')
    digest = _digest({'operation': operation, 'body': body})
    row = c.execute('SELECT request_hash,result FROM operation_receipts WHERE user_id=? AND operation_id=?', (user['id'], ident)).fetchone()
    if row and not hmac.compare_digest(digest, row['request_hash']):
        raise _server().APIError(409, 'Operation ID was used for another request.')
    return ident, digest, json.loads(row['result']) if row else None


def _finish_receipt(c, user, ident, digest, result):
    c.execute('INSERT INTO operation_receipts VALUES(?,?,?,?)', (user['id'], ident, digest, _raw(result)))
    return result


def _typed(c, user, entity, data):
    s = _server()
    fields = {f['key']: f for f in s.SCHEMAS[entity]['fields']}
    unknown = set(data) - set(fields)
    if unknown:
        raise s.APIError(422, 'Unknown input fields: ' + ', '.join(sorted(unknown)))
    normalized = dict(data)
    for key, value in data.items():
        f = fields[key]
        if f['type'] in ('password', 'file'):
            raise s.APIError(422, 'Password and attachment fields cannot be imported.')
        if f['type'] == 'checkbox':
            if str(value).lower() not in ('', 'true', 'false', '1', '0', 'yes', 'no'):
                raise s.APIError(422, f['label'] + ' must be true or false.')
            normalized[key] = str(value).lower() in ('true', '1', 'yes')
        if f['type'] == 'date' and value:
            try:
                date.fromisoformat(value)
            except (TypeError, ValueError):
                raise s.APIError(422, f['label'] + ' must be an ISO date.')
        if f['type'] == 'email' and value and not re.fullmatch(r'[^\s@]+@[^\s@]+\.[^\s@]+', value):
            raise s.APIError(422, 'Invalid email address.')
    # Prevent status spoofing while leaving server drafts explicitly editable.
    status = normalized.pop('status', '')
    if status and status not in ('Active', 'Inactive'):
        raise s.APIError(422, 'Only Active or Inactive master-data status can be supplied.')
    validated = s.validate(c, user, entity, normalized, None)
    validated.pop('status', None)
    return validated


def _preview(c, user, body):
    s = _server()
    entity = body.get('entity')
    if entity not in MASTER_ENTITIES:
        raise s.APIError(422, 'CSV import supports product, customer, supplier, category and unit master data only. Opening balances require a posting workflow.')
    s.check(user, entity, 'write')
    if user['role'] not in ('owner', 'manager'):
        raise s.APIError(403, 'Bulk import requires owner or manager permission.')
    source = body.get('csv')
    if not isinstance(source, str) or not source.strip() or len(source.encode()) > MAX_CSV_BYTES:
        raise s.APIError(422, 'Provide a nonempty UTF-8 CSV of at most 512 KB.')
    delimiter = body.get('delimiter', ',')
    if delimiter not in (',', ';'):
        raise s.APIError(422, 'Choose comma or semicolon delimiter.')
    mapping = body.get('mapping') or {}
    if not isinstance(mapping, dict) or not all(isinstance(k, str) and isinstance(v, str) for k, v in mapping.items()):
        raise s.APIError(422, 'Column mapping must map CSV headings to field names.')
    reader = csv.DictReader(io.StringIO(source.lstrip('\ufeff')), delimiter=delimiter, strict=True)
    headers = reader.fieldnames or []
    if not headers or len(headers) != len(set(headers)) or any(not h for h in headers):
        raise s.APIError(422, 'CSV requires unique, nonempty column headings.')
    keys = [mapping.get(h, h) for h in headers]
    if len(keys) != len(set(keys)):
        raise s.APIError(422, 'Two CSV columns cannot target the same field.')
    if set(mapping) - set(headers):
        raise s.APIError(422, 'Mapping references an absent CSV column.')
    rows, errors, seen_skus = [], [], set()
    try:
        for rownum, row in enumerate(reader, 2):
            if rownum > MAX_ROWS + 1:
                raise s.APIError(422, 'Maximum 500 rows per import.')
            if None in row or any(v is None for v in row.values()):
                errors.append({'row': rownum, 'error': 'Row has a different number of columns.'})
                continue
            data = {mapping.get(k, k): v.strip() for k, v in row.items()}
            try:
                fields = _typed(c, user, entity, data)
                if entity == 'product':
                    sku = fields['sku']
                    if sku in seen_skus:
                        raise s.APIError(422, 'Duplicate SKU within this CSV.')
                    seen_skus.add(sku)
                rows.append(fields)
            except s.APIError as err:
                errors.append({'row': rownum, 'error': err.message})
    except csv.Error as err:
        raise s.APIError(422, 'Malformed CSV: ' + str(err))
    if not rows and not errors:
        raise s.APIError(422, 'CSV has no data rows.')
    if errors:
        return {'preview_id': None, 'digest': None, 'rows': rows[:20], 'count': len(rows), 'errors': errors, 'can_commit': False}
    ident = 'IMP-' + secrets.token_hex(12)
    digest = _digest({'entity': entity, 'rows': rows})
    c.execute('INSERT INTO import_previews(id,user_id,entity,payload,digest,expires) VALUES(?,?,?,?,?,?)',
              (ident, user['id'], entity, _raw(rows), digest, time.time() + 1800))
    return {'preview_id': ident, 'digest': digest, 'rows': rows[:20], 'count': len(rows), 'errors': [], 'can_commit': True, 'expires_in_seconds': 1800}


def handle_api(c, user, path, body):
    """Return JSON result or None. Caller authenticates, checks method/CSRF, commits."""
    paths = {'/api/backup/create', '/api/backup/list', '/api/backup/verify', '/api/backup/restore',
             '/api/import/preview', '/api/import/commit', '/api/system/integrity', '/api/sessions/revoke'}
    if path not in paths:
        return None
    s = _server()
    if not isinstance(body, dict):
        raise s.APIError(400, 'Request body must be an object.')
    _tables(c)
    if path.startswith('/api/backup/'):
        _owner(user)
    if path == '/api/backup/create':
        return _backup(c, user, str(body.get('reason') or 'Manual backup'))
    if path == '/api/backup/list':
        return {'backups': [{'id': r['id'], 'created': r['created'], 'digest': r['digest'],
                            'records': sum(len(x) for x in json.loads(r['payload']).values())}
                           for r in c.execute('SELECT * FROM backups ORDER BY created DESC LIMIT 100')],
                'state_digest': state_digest(c)}
    if path == '/api/backup/verify':
        row, payload = _load_backup(c, body.get('backup_id'))
        return {'valid': True, 'id': row['id'], 'digest': row['digest'], 'records': sum(map(len, payload.values()))}
    if path == '/api/backup/restore':
        reason = _reason(body)
        ident, request_hash, prior = _receipt(c, user, body, path)
        if prior is not None:
            return prior
        if not hmac.compare_digest(str(body.get('expected_state_digest', '')), state_digest(c)):
            raise s.APIError(409, 'Business data changed since restore review. Reload the backup list and review again.')
        row, payload = _load_backup(c, body.get('backup_id'))
        # Restoring a record-only snapshot beside live provider evidence requires
        # a coordinated payment recovery procedure that this harness does not offer.
        provider_tables = [r[0] for r in c.execute("SELECT name FROM sqlite_master WHERE type='table' AND (name LIKE '%payment_intent%' OR name LIKE '%mobile_money%' OR name IN ('finance_cash_entries','finance_postings'))")]
        for table in provider_tables:
            if c.execute('SELECT 1 FROM "' + table.replace('"', '""') + '" LIMIT 1').fetchone():
                raise s.APIError(409, 'Provider or finance ledger evidence exists. Coordinated ledger recovery is required; business-only restore is blocked.')
        safety = _backup(c, user, 'Safety backup before restore: ' + reason)
        versions = {(r['entity'], r['id']): json.loads(r['data']).get('version', 0) for r in c.execute('SELECT * FROM records')}
        c.execute('DELETE FROM records')
        for entity, records in payload.items():
            for original in records:
                record = dict(original)
                record['version'] = max(record['version'], versions.get((entity, record['id']), 0)) + 1
                s.store(c, entity, record)
        c.execute('DELETE FROM sessions')
        s.event(c, user, 'backup', row['id'], 'Business records restored', reason)
        result = {'restored': True, 'records': sum(map(len, payload.values())), 'safety_backup_id': safety['id'],
                  'state_digest': state_digest(c), 'reauthenticate': True,
                  'preserved': ['users', 'audit', 'issued snapshots', 'operation receipts', 'provider records']}
        return _finish_receipt(c, user, ident, request_hash, result)
    if path == '/api/import/preview':
        return _preview(c, user, body)
    if path == '/api/import/commit':
        if user['role'] not in ('owner', 'manager'):
            raise s.APIError(403, 'Bulk import requires owner or manager permission.')
        reason = _reason(body)
        ident, request_hash, prior = _receipt(c, user, body, path)
        if prior is not None:
            s.check(user, prior['entity'], 'write')
            return prior
        preview = c.execute('SELECT * FROM import_previews WHERE id=? AND user_id=?', (body.get('preview_id'), user['id'])).fetchone()
        if not preview:
            raise s.APIError(404, 'Import preview not found for this account.')
        s.check(user, preview['entity'], 'write')
        if user['role'] not in ('owner', 'manager'):
            raise s.APIError(403, 'Bulk import requires owner or manager permission.')
        if preview['committed'] or preview['expires'] < time.time():
            raise s.APIError(409, 'Preview has expired or was already committed.')
        if not hmac.compare_digest(str(body.get('digest', '')), preview['digest']):
            raise s.APIError(409, 'Preview digest does not match.')
        rows = json.loads(preview['payload'])
        ids = []
        # Caller transaction rollback makes the whole batch atomic on any error.
        for data in rows:
            fields = _typed(c, user, preview['entity'], data)
            rid = secrets.token_hex(12)
            record = {**fields, 'id': rid, 'reference': preview['entity'][:4].upper() + '-' + rid[:8],
                      'tenant_id': 'demo', 'company_id': 'company-1', 'branch_id': user['branch_id'],
                      'status': 'Draft', 'version': 1, 'lines': [], 'business_date': date.today().isoformat(),
                      'created_by': user['id'], 'import_id': preview['id']}
            if preview['entity'] == 'product':
                record['stock'] = '0'
            s.store(c, preview['entity'], record)
            ids.append(rid)
        c.execute('UPDATE import_previews SET committed=1 WHERE id=?', (preview['id'],))
        s.event(c, user, 'import', preview['id'], 'CSV import committed', reason)
        return _finish_receipt(c, user, ident, request_hash, {'count': len(ids), 'records': ids, 'entity': preview['entity']})
    if path == '/api/system/integrity':
        if user['role'] not in ('owner', 'auditor'):
            raise s.APIError(403, 'Owner or auditor permission required.')
        result = [r[0] for r in c.execute('PRAGMA quick_check')]
        broken = []
        payload = _entities(c)
        try:
            _validate_backup(payload)
        except s.APIError as err:
            broken.append(err.message)
        return {'sqlite_ok': result == ['ok'], 'business_relations_ok': not broken, 'issues': broken,
                'records': sum(map(len, payload.values())), 'state_digest': _digest(payload),
                'checks': ['SQLite quick_check', 'entity metadata', 'declared relation targets'],
                'not_checked': ['accounting reconciliation', 'external providers', 'offsite recovery', 'filesystem durability']}
    if path == '/api/sessions/revoke':
        target_id = body.get('user_id') or user['id']
        reason = _reason(body)
        if target_id != user['id'] and user['role'] != 'owner':
            raise s.APIError(403, 'You can only revoke your own sessions.')
        target = c.execute('SELECT id,role FROM users WHERE id=?', (target_id,)).fetchone()
        if not target or (target['role'] == 'platform_admin' and user['role'] != 'platform_admin'):
            raise s.APIError(404, 'User not found.')
        count = c.execute('SELECT COUNT(*) FROM sessions WHERE user_id=?', (target_id,)).fetchone()[0]
        c.execute('DELETE FROM sessions WHERE user_id=?', (target_id,))
        c.execute('UPDATE users SET auth_version=auth_version+1 WHERE id=?', (target_id,))
        s.event(c, user, 'identity', target_id, 'Sessions revoked', reason)
        return {'revoked_sessions': count, 'reauthenticate': target_id == user['id']}
