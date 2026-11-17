import json
import sys
import tempfile
import unittest
import uuid
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'server'))
import server
import mobile_money as mm


class Transport:
    def __init__(self):
        self.calls = []
        self.accept = 202
        self.data = {'status': 'PENDING'}
        self.timeout = False
    def __call__(self, method, url, headers, body):
        self.calls.append((method, url, headers, body))
        if url.endswith('/token'):
            return 200, {'access_token': 'fake-test-token'}
        if method == 'POST':
            if self.timeout:
                raise mm.ProviderError('Test network timeout.', True)
            return self.accept, {}
        return 200, self.data


class MobileMoneyTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.original = server.DBPATH
        server.DBPATH = Path(self.temp.name) / 'test.sqlite3'
        server.DEMO = True; server.initialize()
        self.c = server.connect(); mm.initialize(self.c)
        self.user = dict(self.c.execute("SELECT * FROM users WHERE id='owner'").fetchone())
        self.payment = server.get(self.c, 'payment', 'payment-1')
        self.payment.update(id='test-mobile', reference='MM-TEST', status='Draft', amount='100',
                            allocation_amount='100', tender='Mobile money', supplier='', purchase_order='',
                            customer='customer-1', sale='sale-1', version=1)
        server.store(self.c, 'payment', self.payment)
        for period in server.all_records(self.c, 'period'):
            period.update(status='Open', start_date='2026-01-01', end_date='2026-12-31', company='company-1'); server.store(self.c, 'period', period)
        self.c.commit()
        self.transport = Transport()
        self.env = {'MTN_TARGET_ENVIRONMENT': 'mtncameroon', 'MTN_BASE_URL': 'https://momoapi.momo.africa',
                    'MTN_USER_ID': 'fake-test-user', 'MTN_CREDENTIAL_TOKEN': 'fake-test-secret',
                    'MTN_COLLECTION_SUBSCRIPTION_KEY': 'fake-test-subscription',
                    'QUINCAI_PAYMENTS_LIVE_ENABLED': '1'}
        self.adapter = mm.MTNCollections(self.env, self.transport)
        self.adapters = {'mtn': self.adapter, 'orange': mm.OrangeMoney()}
        self.body = {'payment_id': 'test-mobile', 'provider': 'mtn', 'phone': '237690000000',
                     'operation_id': str(uuid.uuid4())}
    def tearDown(self):
        self.c.close(); server.DBPATH = self.original; self.temp.cleanup()
    def initiate(self):
        return mm.initiate(self.c, self.user, self.body, self.adapters)['intent']
    def success(self, intent):
        self.transport.data = {'status': 'SUCCESSFUL', 'amount': intent['amount'],
                               'currency': intent['currency'], 'externalId': intent['id'],
                               'payer': {'partyIdType': 'MSISDN', 'partyId': self.body['phone']},
                               'financialTransactionId': 'provider-txn-1'}
        return mm.query(self.c, self.user, {'intent_id': intent['id']}, self.adapters)
    def test_accepted_is_not_paid(self):
        i = self.initiate(); self.assertEqual(i['status'], 'Pending')
        self.assertEqual(server.get(self.c, 'payment', 'test-mobile')['status'], 'Awaiting provider')
        self.assertNotIn('phone', i)
    def test_headers_and_provider_identity(self):
        i = self.initiate(); token, request = self.transport.calls
        self.assertEqual(token[2]['X-Target-Environment'], 'mtncameroon')
        self.assertEqual(request[2]['X-Reference-Id'], i['provider_reference'])
        self.assertEqual(request[3]['externalId'], i['id'])
        self.assertEqual(request[3]['currency'], 'XAF')
    def test_replay_sends_only_one_request(self):
        i = self.initiate(); calls = len(self.transport.calls)
        self.assertEqual(self.initiate()['id'], i['id']); self.assertEqual(len(self.transport.calls), calls)
    def test_operation_conflict(self):
        self.initiate(); self.body['phone'] = '237690000001'
        with self.assertRaises(server.APIError) as cm: self.initiate()
        self.assertEqual(cm.exception.status, 409)
    def test_uncertain_request_durable_and_not_reinitiated(self):
        self.transport.timeout = True; i = self.initiate(); self.assertEqual(i['status'], 'Unknown')
        self.assertEqual(self.initiate()['id'], i['id']); self.assertEqual(len(self.transport.calls), 2)
    def test_duplicate_provider_reference_queries_instead(self):
        self.transport.accept = 409; i = self.initiate(); self.assertEqual(i['status'], 'Pending')
    def test_verified_success_posts_once(self):
        i = self.initiate(); first = self.success(i); second = self.success(i)
        self.assertEqual(first['payment']['status'], 'Completed')
        self.assertEqual(first['payment']['version'], second['payment']['version'])
        self.assertTrue(first['payment']['provider_verified'])
    def test_mismatched_amount_never_posts(self):
        i = self.initiate(); self.transport.data = {'status': 'SUCCESSFUL', 'amount': '999', 'currency': 'XAF'}
        result = mm.query(self.c, self.user, {'intent_id': i['id']}, self.adapters)
        self.assertEqual(result['intent']['status'], 'Review required')
        self.assertNotEqual(result['payment']['status'], 'Completed')
    def test_spoofed_callback_does_not_settle(self):
        i = self.initiate(); calls = len(self.transport.calls)
        mm.callback(self.c, 'mtn', i['id'], {'status': 'SUCCESSFUL', 'amount': '100'})
        self.assertEqual(len(self.transport.calls), calls)
        self.assertEqual(server.get(self.c, 'payment', 'test-mobile')['status'], 'Awaiting provider')
    def test_sandbox_does_not_post_xaf_payment(self):
        self.env.update(MTN_TARGET_ENVIRONMENT='sandbox', MTN_BASE_URL='https://sandbox.momodeveloper.mtn.com')
        self.adapters['mtn'] = mm.MTNCollections(self.env, self.transport)
        i = self.initiate(); result = self.success(i)
        self.assertEqual(i['currency'], 'EUR'); self.assertEqual(result['intent']['status'], 'Sandbox successful')
        self.assertEqual(result['payment']['status'], 'Sandbox verified')
    def test_no_live_activation_without_flag(self):
        self.env['QUINCAI_PAYMENTS_LIVE_ENABLED'] = '0'
        with self.assertRaises(server.APIError) as cm: self.initiate()
        self.assertEqual(cm.exception.status, 503); self.assertEqual(self.transport.calls, [])
    def test_orange_explicitly_unavailable(self):
        self.body['provider'] = 'orange'
        with self.assertRaises(server.APIError) as cm: self.initiate()
        self.assertEqual(cm.exception.status, 503)
    def test_auditor_cannot_initiate(self):
        self.user['role'] = 'auditor'
        with self.assertRaises(server.APIError) as cm: self.initiate()
        self.assertEqual(cm.exception.status, 403)
    def test_cross_branch_query_forbidden(self):
        i = self.initiate(); self.user.update(role='cashier', branch_id='branch-2')
        with self.assertRaises(server.APIError) as cm: mm.query(self.c, self.user, {'intent_id': i['id']}, self.adapters)
        self.assertEqual(cm.exception.status, 403)
    def test_collection_cannot_exceed_outstanding(self):
        self.payment['amount'] = '999999'; server.store(self.c, 'payment', self.payment)
        with self.assertRaises(server.APIError) as cm: self.initiate()
        self.assertEqual(cm.exception.status, 422); self.assertEqual(self.transport.calls, [])
    def test_closed_period_preserves_provider_truth_and_can_reconcile(self):
        i = self.initiate()
        for p in server.all_records(self.c, 'period'):
            p['status'] = 'Closed'; server.store(self.c, 'period', p)
        self.c.commit(); result = self.success(i)
        self.assertEqual(result['intent']['status'], 'Review required')
        self.assertTrue(result['intent']['provider_confirmed'])
        self.assertEqual(result['payment']['status'], 'Awaiting provider')
        for p in server.all_records(self.c, 'period'):
            p['status'] = 'Open'; server.store(self.c, 'period', p)
        self.c.commit(); self.transport.data = {'status':'FAILED'}
        result = mm.query(self.c, self.user, {'intent_id':i['id']}, self.adapters)
        self.assertEqual(result['payment']['status'], 'Completed')
        self.assertTrue(result['payment']['posting_id'])
    def test_untrusted_provider_origin_rejected(self):
        self.env['MTN_BASE_URL'] = 'https://attacker.invalid'
        self.adapters['mtn'] = mm.MTNCollections(self.env, self.transport)
        with self.assertRaises(server.APIError) as cm: self.initiate()
        self.assertEqual(cm.exception.status, 503)

if __name__ == '__main__': unittest.main()
