"""Provider payment intents for the local integration service.

MTN Collections adapter is implemented against the documented API; activation is
configuration-gated. Orange is deliberately unavailable until the subscribed
merchant contract can be verified. No callback body is proof of payment.
"""
import base64
import hashlib
import json
import os
import re
import time
import uuid
from decimal import Decimal
from urllib.error import HTTPError, URLError
from urllib.parse import urlparse
from urllib.request import Request, build_opener, HTTPRedirectHandler


class ProviderError(Exception):
    def __init__(self, message, uncertain=False):
        super().__init__(message)
        self.uncertain = uncertain


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def http_request(method, url, headers, body=None):
    """Bounded TLS request, no redirect credential forwarding or response logging."""
    request = Request(url, data=None if body is None else json.dumps(body).encode(),
                      headers=headers, method=method)
    try:
        with build_opener(NoRedirect).open(request, timeout=15) as response:
            raw = response.read(65537)
            if len(raw) > 65536:
                raise ProviderError('Provider response exceeds the permitted size.', True)
            return response.status, json.loads(raw) if raw else {}
    except HTTPError as exc:
        # Never include upstream response bodies, tokens or phone numbers in errors.
        return exc.code, {}
    except (URLError, TimeoutError, OSError, ValueError):
        raise ProviderError('Provider connection or response could not be verified.', True)


class MTNCollections:
    id = 'mtn'
    label = 'MTN MoMo'
    HOSTS = {'sandbox.momodeveloper.mtn.com', 'proxy.momoapi.mtn.com',
             'ericssonbasicapi1.azure-api.net', 'momoapi.momo.africa'}

    def __init__(self, env=None, transport=None):
        self.env = os.environ if env is None else env
        self.transport = transport or http_request
        self.environment = self.env.get('MTN_TARGET_ENVIRONMENT', 'sandbox')
        self.currency = 'EUR' if self.environment == 'sandbox' else 'XAF'
        self.base = self.env.get('MTN_BASE_URL', 'https://sandbox.momodeveloper.mtn.com').rstrip('/')
        self.identity = self.env.get('MTN_USER_ID', '')
        self.secret = self.env.get('MTN_CREDENTIAL_TOKEN', '')
        self.subscription = self.env.get('MTN_COLLECTION_SUBSCRIPTION_KEY', '')
        self.callback = self.env.get('MTN_CALLBACK_URL', '')

    def config_error(self):
        if self.environment not in ('sandbox', 'mtncameroon'):
            return 'Only MTN sandbox and Cameroon environments are supported.'
        parsed = urlparse(self.base)
        if (parsed.scheme != 'https' or parsed.hostname not in self.HOSTS or parsed.path
                or parsed.query or parsed.fragment or parsed.username or parsed.port not in (None, 443)):
            return 'MTN base URL must be an approved HTTPS provider origin.'
        if self.environment == 'sandbox' and parsed.hostname != 'sandbox.momodeveloper.mtn.com':
            return 'Sandbox requires the sandbox provider origin.'
        if self.environment != 'sandbox':
            if parsed.hostname == 'sandbox.momodeveloper.mtn.com':
                return 'Configure the production origin confirmed by your MTN account manager.'
            if self.env.get('QUINCAI_PAYMENTS_LIVE_ENABLED') != '1':
                return 'Live payments have not been explicitly enabled on the server.'
        if not all((self.identity, self.secret, self.subscription)):
            return 'MTN merchant credentials are not configured.'
        if self.callback:
            cb = urlparse(self.callback)
            if cb.scheme != 'https' or not cb.hostname or cb.username or cb.fragment:
                return 'MTN callback URL must use HTTPS.'
        return None

    def headers(self):
        error = self.config_error()
        if error:
            raise ProviderError(error)
        basic = base64.b64encode((self.identity + ':' + self.secret).encode()).decode()
        status, data = self.transport('POST', self.base + '/collection/token', {
            'Authorization': 'Basic ' + basic,
            'Ocp-Apim-Subscription-Key': self.subscription,
            'X-Target-Environment': self.environment,
            'Accept': 'application/json'}, None)
        if status != 200 or not isinstance(data, dict) or not data.get('access_token'):
            raise ProviderError('MTN authentication failed; verify merchant configuration.')
        return {'Authorization': 'Bearer ' + str(data['access_token']),
                'Ocp-Apim-Subscription-Key': self.subscription,
                'X-Target-Environment': self.environment,
                'Content-Type': 'application/json', 'Accept': 'application/json'}

    def initiate(self, intent):
        headers = self.headers()
        headers['X-Reference-Id'] = intent['provider_reference']
        if self.callback:
            # Append opaque intent ID to a configured callback base, never accept a client URL.
            headers['X-Callback-Url'] = self.callback.rstrip('/') + '/' + intent['id']
        body = {'amount': intent['amount'], 'currency': intent['currency'],
                'externalId': intent['id'],
                'payer': {'partyIdType': 'MSISDN', 'partyId': intent['phone']},
                'payerMessage': 'QuincaiSoft payment', 'payeeNote': intent['payment_id'][:160]}
        status, _ = self.transport('POST', self.base + '/collection/v1_0/requesttopay', headers, body)
        if status in (202, 409):
            return {'status': 'Pending', 'query_due': True}
        if status >= 500 or status in (408, 429):
            raise ProviderError('MTN outcome is uncertain; query this same reference before retrying.', True)
        raise ProviderError('MTN rejected the payment request; no successful payment was confirmed.')

    def query(self, intent):
        status, data = self.transport('GET', self.base + '/collection/v1_0/requesttopay/'
                                      + intent['provider_reference'], self.headers(), None)
        if status == 404:
            raise ProviderError('MTN reference is not yet available; keep this intent for reconciliation.', True)
        if status != 200 or not isinstance(data, dict):
            raise ProviderError('MTN payment status could not be verified.', True)
        return data


class OrangeMoney:
    id = 'orange'
    label = 'Orange Money'
    environment = 'unconfigured'
    currency = 'XAF'

    def config_error(self):
        return ('Orange Money activation is blocked pending the subscribed Cameroon merchant API '
                'contract and credentials. No Orange payment request is sent.')

    def initiate(self, intent):
        raise ProviderError(self.config_error())

    def query(self, intent):
        raise ProviderError(self.config_error())


def providers():
    return {'mtn': MTNCollections(), 'orange': OrangeMoney()}


def initialize(c):
    c.execute('''CREATE TABLE IF NOT EXISTS mobile_money_intents(
        id TEXT PRIMARY KEY, user_id TEXT NOT NULL, operation_id TEXT NOT NULL,
        payment_id TEXT NOT NULL, fingerprint TEXT NOT NULL, data TEXT NOT NULL,
        UNIQUE(user_id, operation_id))''')


def _store(c, intent):
    c.execute('UPDATE mobile_money_intents SET data=? WHERE id=?',
              (json.dumps(intent), intent['id']))


def _public(intent):
    return {k: v for k, v in intent.items() if k not in ('phone', 'fingerprint', 'user_id', 'operation_id', 'confirmed_response')}


def _intent(c, id):
    from server import APIError
    row = c.execute('SELECT data FROM mobile_money_intents WHERE id=?', (id,)).fetchone()
    if not row:
        raise APIError(404, 'Payment intent not found.')
    return json.loads(row[0])


def _payment(c, user, id, op='write'):
    from server import get, check
    payment = get(c, 'payment', id)
    check(user, 'payment', op, payment)
    return payment


def _now():
    return time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())


def initiate(c, user, body, adapters=None):
    from server import APIError, number, store, event, get, check, all_records, total
    initialize(c)
    payment = _payment(c, user, body.get('payment_id'))
    provider_id = body.get('provider')
    adapter = (adapters or providers()).get(provider_id)
    if not adapter:
        raise APIError(422, 'Choose MTN MoMo or Orange Money.')
    error = adapter.config_error()
    if error:
        raise APIError(503, error)
    operation = str(body.get('operation_id', ''))
    if not operation or len(operation) > 100:
        raise APIError(422, 'A stable operation ID is required.')
    phone = str(body.get('phone', '')).strip().replace('+', '')
    if not re.fullmatch(r'[0-9]{9,15}', phone):
        raise APIError(422, 'Use a valid international phone number including country code.')
    if adapter.environment != 'sandbox' and not re.fullmatch(r'2376[0-9]{8}', phone):
        raise APIError(422, 'Cameroon payments require a 2376-prefixed mobile number.')
    amount = number(payment.get('amount'))
    if amount <= 0 or amount != amount.to_integral_value():
        raise APIError(422, 'Mobile money amount must be a positive whole currency unit.')
    digest = hashlib.sha256(json.dumps([payment['id'], provider_id, phone, str(amount),
                                       adapter.environment, adapter.currency]).encode()).hexdigest()
    row = c.execute('SELECT data,fingerprint FROM mobile_money_intents WHERE user_id=? AND operation_id=?',
                    (user['id'], operation)).fetchone()
    if row:
        if row['fingerprint'] != digest:
            raise APIError(409, 'Operation ID was already used for a different payment request.')
        return {'intent': _public(json.loads(row['data']))}
    if payment.get('status') not in ('Draft', 'Approved') or payment.get('tender') != 'Mobile money':
        raise APIError(409, 'Use an editable Mobile money payment draft.')
    if payment.get('supplier') or payment.get('purchase_order'):
        raise APIError(422, 'Collections cannot initiate outgoing supplier payments.')
    if not payment.get('sale'):
        raise APIError(422, 'Link the collection to a sale before requesting payment.')
    sale = get(c, 'sale', payment['sale'])
    check(user, 'sale', 'read', sale)
    if sale.get('status') not in ('Completed', 'Posted'):
        raise APIError(409, 'Complete the sale before collecting payment.')
    if payment.get('customer') and payment['customer'] != sale.get('customer'):
        raise APIError(422, 'Payment customer must match the sale.')
    from finance_workflows import outstanding
    if number(payment.get('allocation_amount') or amount) != amount:
        raise APIError(422, 'Split excess collection into an explicit deposit; allocation must equal the collection amount.')
    pending = sum((number(p.get('amount')) for p in all_records(c, 'payment')
                   if p.get('sale') == sale['id'] and p.get('status') == 'Awaiting provider'), Decimal(0))
    if amount > outstanding(c, sale) - pending:
        raise APIError(422, 'Collection exceeds the sale balance after existing and pending payments.')
    for row in c.execute('SELECT data FROM mobile_money_intents WHERE payment_id=?', (payment['id'],)):
        if json.loads(row[0])['status'] not in ('Failed', 'Rejected'):
            raise APIError(409, 'This payment already has an intent; query its outcome instead.')
    intent = {'id': str(uuid.uuid4()), 'payment_id': payment['id'], 'provider': provider_id,
              'provider_reference': str(uuid.uuid4()), 'amount': str(amount),
              'currency': adapter.currency, 'environment': adapter.environment,
              'phone': phone, 'status': 'Submitting', 'query_due': True,
              'created_at': _now(), 'updated_at': _now(), 'error': None}
    c.execute('INSERT INTO mobile_money_intents VALUES(?,?,?,?,?,?)',
              (intent['id'], user['id'], operation, payment['id'], digest, json.dumps(intent)))
    payment['status'] = 'Awaiting provider'; payment['version'] += 1
    payment['mobile_money_intent'] = intent['id']
    store(c, 'payment', payment)
    event(c, user, 'payment', payment['id'], 'Mobile money intent created', provider_id)
    # Durable intent BEFORE network. A process failure must not create a fresh charge on replay.
    c.commit()
    try:
        intent.update(adapter.initiate(intent))
    except ProviderError as exc:
        intent.update(status='Unknown' if exc.uncertain else 'Rejected', error=str(exc), query_due=exc.uncertain)
        if not exc.uncertain:
            payment['status'] = 'Draft'; payment['version'] += 1; store(c, 'payment', payment)
    intent['updated_at'] = _now(); _store(c, intent); c.commit()
    return {'intent': _public(intent)}


def query(c, user, body, adapters=None):
    from server import APIError, number, store, event, get, total, all_records, safe_record
    initialize(c)
    intent = _intent(c, body.get('intent_id'))
    payment = _payment(c, user, intent['payment_id'])
    if intent['status'] in ('Successful', 'Sandbox successful', 'Failed', 'Rejected'):
        return {'intent': _public(intent), 'payment': safe_record(user, 'payment', payment)}
    adapter = (adapters or providers()).get(intent['provider'])
    if not adapter or adapter.environment != intent['environment'] or adapter.currency != intent['currency']:
        raise APIError(409, 'Provider environment changed; reconcile using the original merchant configuration.')
    if adapter.config_error():
        raise APIError(503, adapter.config_error())
    try:
        data = intent.get('confirmed_response') or adapter.query(intent)
    except ProviderError as exc:
        intent.update(error=str(exc), query_due=True, updated_at=_now()); _store(c, intent); c.commit()
        return {'intent': _public(intent)}
    status = data.get('status')
    if status not in ('SUCCESSFUL', 'FAILED', 'PENDING'):
        intent.update(status='Unknown', error='Provider returned an unrecognized status.', query_due=True)
    elif status == 'SUCCESSFUL':
        # Validate all provider echoes before any accounting effect.
        try:
            valid = (number(data.get('amount')) == number(intent['amount'])
                     and data.get('currency') == intent['currency']
                     and data.get('externalId') == intent['id']
                     and data.get('payer', {}).get('partyId') == intent['phone']
                     and data.get('payer', {}).get('partyIdType') == 'MSISDN'
                     and isinstance(data.get('financialTransactionId'), str)
                     and bool(data['financialTransactionId']))
        except (APIError, TypeError, AttributeError):
            valid = False
        if valid:
            for row in c.execute('SELECT data FROM mobile_money_intents WHERE id != ?', (intent['id'],)):
                prior = json.loads(row[0])
                if prior.get('financial_transaction_id') == data['financialTransactionId'] and prior.get('provider') == intent['provider']:
                    valid = False
        if not valid:
            intent.update(status='Review required', error='Provider response does not match amount, currency, payer or reference.', query_due=True)
        elif intent['environment'] == 'sandbox':
            intent.update(status='Sandbox successful', query_due=False, error=None)
            payment['status'] = 'Sandbox verified'; payment['version'] += 1; store(c, 'payment', payment)
            event(c, user, 'payment', payment['id'], 'Sandbox collection verified', 'No XAF ledger allocation posted')
        else:
            intent.update(provider_confirmed=True, financial_transaction_id=data['financialTransactionId'], confirmed_response={k:data[k] for k in ('status','amount','currency','externalId','payer','financialTransactionId')})
            sale = get(c, 'sale', payment['sale'])
            from finance_workflows import outstanding
            allocation = number(payment.get('allocation_amount') or payment['amount'])
            if (number(payment['amount']) != number(intent['amount']) or allocation <= 0
                    or allocation != number(intent['amount']) or allocation > outstanding(c, sale)
                    or payment.get('mobile_money_intent') != intent['id']):
                intent.update(status='Review required', error='Funds confirmed, but allocation needs review. Do not charge again.', query_due=True)
            else:
                from finance_workflows import post_verified_provider_payment
                candidate = dict(payment)
                candidate.update(status='Completed', external_reference=data['financialTransactionId'],
                                 allocation_amount=str(allocation), provider_verified=True, provider=intent['provider'])
                candidate['version'] += 1
                c.execute('SAVEPOINT mobile_money_posting')
                try:
                    post_verified_provider_payment(c, user, candidate)
                    store(c, 'payment', candidate)
                    event(c, user, 'payment', candidate['id'], 'Provider collection verified', intent['provider_reference'])
                    c.execute('RELEASE mobile_money_posting')
                    payment = candidate
                    intent.update(status='Successful', query_due=False, error=None)
                except APIError as exc:
                    c.execute('ROLLBACK TO mobile_money_posting'); c.execute('RELEASE mobile_money_posting')
                    intent.update(status='Review required', query_due=True,
                                  error='Funds confirmed; accounting posting pending: ' + exc.message)
    elif status == 'FAILED':
        intent.update(status='Failed', query_due=False, error='Provider reports that collection failed.')
        if payment.get('status') == 'Awaiting provider':
            payment['status'] = 'Draft'; payment['version'] += 1; store(c, 'payment', payment)
    else:
        intent.update(status='Pending', query_due=True, error=None)
    intent['updated_at'] = _now(); _store(c, intent); c.commit()
    return {'intent': _public(intent), 'payment': safe_record(user, 'payment', payment)}


def callback(c, provider, intent_id, body):
    """Public callbacks only enqueue reconciliation, regardless of claimed status."""
    from server import APIError
    initialize(c)
    intent = _intent(c, intent_id)
    if intent['provider'] != provider:
        raise APIError(404, 'Payment intent not found.')
    if intent['status'] not in ('Successful', 'Sandbox successful', 'Failed', 'Rejected'):
        intent.update(query_due=True, updated_at=_now()); _store(c, intent); c.commit()
    return {'accepted': True, 'verification_required': True}


def handle_api(c, user, path, body):
    from server import check, get, scoped
    if not path.startswith('/api/mobile-money/'):
        return None
    initialize(c)
    if path == '/api/mobile-money/config':
        check(user, 'payment')
        return {'providers': [{'id': p.id, 'label': p.label, 'configured': not p.config_error(),
                              'environment': p.environment, 'reason': p.config_error()}
                             for p in providers().values()]}
    if path == '/api/mobile-money/intents':
        check(user, 'payment')
        visible = []
        for row in c.execute('SELECT data FROM mobile_money_intents ORDER BY rowid DESC LIMIT 500'):
            intent = json.loads(row[0]); payment = get(c, 'payment', intent['payment_id'])
            if scoped(user, payment, 'payment'):
                visible.append(_public(intent))
        return {'intents': visible}
    if path == '/api/mobile-money/initiate':
        return initiate(c, user, body)
    if path == '/api/mobile-money/query':
        return query(c, user, body)
    return None
